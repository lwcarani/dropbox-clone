package io.github.lwcarani.cli;

import java.io.Console;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.amazonaws.services.cognitoidp.model.AuthenticationResultType;

import io.github.lwcarani.model.CurrentWorkingDirectory;
import io.github.lwcarani.model.User;
import io.github.lwcarani.service.CognitoUserService;
import io.github.lwcarani.service.StorageService;
import io.github.lwcarani.service.UserPreferenceService;
import io.github.lwcarani.service.UserService;
import io.github.lwcarani.util.FileUtils;
import io.github.lwcarani.util.PasswordValidator;

@Component
public class DropboxCliRunner {

	// Service dependencies and state variables
	private final UserService userService;
	private final StorageService storageService;
	private final UserPreferenceService preferenceService;
	private Scanner scanner;
	private User currentUser;
	private String accessToken;
	private CurrentWorkingDirectory cwd;
	private String rootDirectory;
	private volatile boolean running;

	@Autowired
	private ApplicationContext context;

	// Constructor initializes services and scanner
	public DropboxCliRunner(UserService userService, StorageService storageService,
			UserPreferenceService preferenceService) {
		this.userService = userService;
		this.storageService = storageService;
		this.preferenceService = preferenceService;
		this.scanner = new Scanner(System.in);
		this.running = true;
	}

	// Main run loop for the CLI
	public void run(String... args) {
		System.out.println("Welcome to Dropbox Clone CLI!");
		System.out.println("Type 'help' for a list of commands.");

		// Register shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

		try {
			while (running) {
				displayPrompt();
				String command = scanner.nextLine().trim().toLowerCase();

				if (currentUser == null) {
					handleLoggedOutCommands(command);
				} else {
					handleLoggedInCommands(command);
				}
			}
		} catch (NoSuchElementException e) {
			// This exception is thrown when Ctrl+D (EOF) is pressed
			shutdown();
		} catch (IllegalStateException e) {
			// This exception might be thrown if the scanner is closed unexpectedly
			shutdown();
		}
	}

	// Graceful shutdown of the application
	private void shutdown() {
		if (!running)
			return; // Avoid running shutdown logic multiple times
		running = false;
		if (currentUser != null) {
			logout();
		}
		if (scanner != null) {
			scanner.close();
		}
		System.out.println("Goodbye!");

		// Trigger Spring Boot shutdown
		((ConfigurableApplicationContext) context).close();
	}

	// Displays the command prompt
	private void displayPrompt() {
		if (currentUser == null) {
			System.out.print("> ");
		} else {
			System.out.print(cwd.getPromptString(rootDirectory) + "> ");
		}
		System.out.flush(); // Ensure the prompt is displayed before waiting for input
	}

	// Handles commands when the user is not logged in
	private void handleLoggedOutCommands(String command) {
		switch (command) {
		case "login":
			login();
			break;
		case "signup":
			signup();
			break;
		case "help":
			printHelp();
			break;
		case "exit":
			shutdown();
			break;
		default:
			System.out.println("Please login first. Type 'login' to begin.");
		}
	}

	// Handles commands when the user is logged in
	private void handleLoggedInCommands(String input) {
		if (!userService.authenticateUserSession(accessToken)) {
			System.out.println("Your session has expired. Please log in again.");
			currentUser = null;
			accessToken = null;
			cwd = null;
			return;
		}

		String[] parts = input.split("\\s+", 2);
		String command = parts[0].toLowerCase();
		String args = parts.length > 1 ? parts[1].trim() : "";

		switch (command) {
		case "logout":
			logout();
			break;
		case "delete_account":
			deleteAccount();
			break;
		case "mkdir":
			mkdir(args);
			break;
		case "change_root":
			changeRootDirectory(args);
			break;
		case "cd":
			cd(args);
			break;
		case "ls":
			ls(args);
			break;
		case "push":
			push();
			break;
		case "pull":
			pull();
			break;
		case "rm":
			rm(args);
			break;
		case "help":
			printHelp();
			break;
		case "exit":
			shutdown();
			break;
		default:
			System.out.printf("'%s' not recognized. Type 'help' for options.\n", command);
		}
	}

	// User login process
	private void login() {
		if (currentUser != null) {
			System.out.println("You are already logged in as " + currentUser.getUsername());
			return;
		}

		try {
			System.out.print("Enter username: ");
			String username = scanner.nextLine().trim();

			Console console = System.console();
			char[] passwordArray;
			if (console != null) {
				passwordArray = console.readPassword("Enter password: ");
			} else {
				System.out.print("Enter password: ");
				passwordArray = scanner.nextLine().trim().toCharArray();
			}
			String password = new String(passwordArray);

			// Clear the password array for security
			java.util.Arrays.fill(passwordArray, ' ');

			AuthenticationResultType authResult = userService.authenticateUser(username, password);
			if (authResult != null && authResult.getAccessToken() != null) {
				accessToken = authResult.getAccessToken();
				String userId = ((CognitoUserService) userService).getUserId(accessToken);
				String email = ((CognitoUserService) userService).getEmail(accessToken);

				System.out.println("Welcome back, user!");
				currentUser = new User(username, email, userId);
				cwd = new CurrentWorkingDirectory(userId, username);
				System.out.println("Login successful. Welcome, " + currentUser.getUsername() + "!");

				setRootDirectory();
				FileUtils.createLocalDirectory(cwd.getPromptString(rootDirectory));
			} else {
				System.out.println(
						"Login failed. Please check your credentials. To make a new account, type 'signup' to begin.");
			}
		} catch (NoSuchElementException e) {
			System.out.println("\nLogin cancelled.");
		}
	}

	// User logout process
	private void logout() {
		if (currentUser != null) {
			String username = currentUser.getUsername();
			userService.logout(accessToken);
			currentUser = null;
			accessToken = null;
			cwd = null;
			rootDirectory = null;
			System.out.println("Logout successful. Goodbye, " + username + "!");
		} else {
			System.out.println("No user is currently logged in.");
		}
	}

	// Changes the root directory for the current user for the application
	private void changeRootDirectory(String newPath) {
		if (newPath.isEmpty()) {
			System.out.println("Usage: change_root <new_root_path>");
			return;
		}

		File newRoot = new File(newPath);
		if (!newRoot.exists() || !newRoot.isDirectory()) {
			System.out.println("Invalid directory path: " + newPath);
			return;
		}

		rootDirectory = newPath;

		// Save the location of the rootDirectory for this user so that next session we
		// can automatically load it
		preferenceService.saveUserPreference(currentUser.getUserId(), "rootDirectory", rootDirectory);
		System.out.println("Root directory changed to: " + rootDirectory);
		FileUtils.createRootDirectory(rootDirectory);
	}

	// Sets the root directory if provided a valid path, otherwise uses a default
	// location
	private void setRootDirectory() {
		String savedRootDir = preferenceService.getUserPreference(currentUser.getUserId(), "rootDirectory");

		if (savedRootDir != null) {
			System.out.println("Using saved root directory: " + savedRootDir);
			rootDirectory = savedRootDir;
		} else {
			rootDirectory = FileUtils.selectRootDirectory();
			if (rootDirectory == null) {
				System.out.println("No root directory selected. Using the current working directory.");
				rootDirectory = System.getProperty("user.dir");
			}
			System.out.println("Root directory set to: " + rootDirectory);
			preferenceService.saveUserPreference(currentUser.getUserId(), "rootDirectory", rootDirectory);
		}

		FileUtils.createRootDirectory(rootDirectory);
	}

	// Creates a new directory
	private void mkdir(String folderName) {
		if (folderName.isEmpty()) {
			System.out.println("Usage: mkdir <folder_name>");
			return;
		}

		storageService.createFolder(cwd.getFullPath(), folderName);
		FileUtils.createLocalDirectory(cwd.getPromptString(rootDirectory) + "/" + folderName);
	}

	// Removes a directory and its contents
	private void rm(String path) {
		if (path.isEmpty()) {
			System.out.println("Usage: rm <path>");
			return;
		}

		String currentFullPath = cwd.getPromptString(rootDirectory);
		Path currentPath = Paths.get(currentFullPath);
		Path newPath;

		// Ensure the new path is still within the root directory
		try {
			if (Paths.get(path).isAbsolute()) {
				// Handle absolute path
				newPath = Paths.get(path);
			} else {
				// Handle relative path
				newPath = currentPath.resolve(path).normalize();
			}
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			System.out.printf("%s is an invalid path.", path);
			return;
		}
		// Ensure the new path is still within the root directory
		Path rootPath = Paths.get(cwd.getRootString(rootDirectory));
		if (!newPath.startsWith(rootPath)) {
			System.out.println("Cannot navigate outside of root directory.");
			return;
		}

		String fullCloudPath = cwd.getFullPath() + "/" + path;
		Path fullLocalPath = Paths.get(cwd.getPromptString(rootDirectory), path);

		if (!storageService.isValidS3Directory(fullCloudPath) && !Files.exists(fullLocalPath)) {
			System.out.println("Directory does not exist locally or in the cloud: " + path);
			return;
		}

		System.out.println(
				"Warning: This will delete the directory and all its contents both locally and in S3. Continue? (y/n)");
		String confirmation = scanner.nextLine().trim().toLowerCase();

		if (confirmation.equals("y")) {
			// Delete from S3
			if (storageService.isValidS3Directory(fullCloudPath)) {
				storageService.deleteDirectory(fullCloudPath);
				System.out.println("Cloud directory deleted successfully: " + fullCloudPath);
			} else {
				System.out.println("Cloud directory does not exist: " + fullCloudPath);
			}

			// Delete locally
			if (Files.exists(fullLocalPath) && Files.isDirectory(fullLocalPath)) {
				try {
					Files.walk(fullLocalPath).sorted((p1, p2) -> -p1.compareTo(p2)).forEach(p -> {
						try {
							Files.delete(p);
						} catch (Exception e) {
							System.err.println("Error deleting " + p + ": " + e.getMessage());
						}
					});
					System.out.println("Local directory deleted successfully: " + fullLocalPath);
				} catch (Exception e) {
					System.err.println("Error deleting local directory: " + e.getMessage());
				}
			} else {
				System.out.println("Local directory does not exist: " + fullLocalPath);
			}
		} else {
			System.out.println("Delete operation cancelled.");
		}
	}

	// Pushes local folder and file changes to cloud storage
	private void push() {
		System.out.println(
				"Warning: This will overwrite any existing files in the cloud with local files. Continue? (y/n)");
		String confirmation = scanner.nextLine().trim().toLowerCase();
		if (confirmation.equals("y")) {
			storageService.pushToS3(currentUser.getUserId(), currentUser.getUsername(), rootDirectory);
			System.out.println("Push completed successfully.");
		} else {
			System.out.println("Push operation cancelled.");
		}
	}

	// Pulls cloud changes to local, overwriting any duplicate folder or file names
	private void pull() {
		System.out.println(
				"Warning: This will overwrite any existing local files with files currently stored in the cloud. Continue? (y/n)");
		String confirmation = scanner.nextLine().trim().toLowerCase();
		if (confirmation.equals("y")) {
			storageService.pullFromS3(currentUser.getUserId(), currentUser.getUsername(), rootDirectory);
			System.out.println("Pull completed successfully.");
		} else {
			System.out.println("Pull operation cancelled.");
		}
	}

	// Lists contents of a directory
	private void ls(String path) {

		String currentFullPath = cwd.getPromptString(rootDirectory);
		Path currentPath = Paths.get(currentFullPath);
		Path newPath;

		// Ensure the new path is still within the root directory
		try {
			if (Paths.get(path).isAbsolute()) {
				// Handle absolute path
				newPath = Paths.get(path);
			} else {
				// Handle relative path
				newPath = currentPath.resolve(path).normalize();
			}
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			System.out.printf("%s is an invalid path.", path);
			return;
		}
		// Ensure the new path is still within the root directory
		Path rootPath = Paths.get(cwd.getRootString(rootDirectory));
		if (!newPath.startsWith(rootPath)) {
			System.out.println("Cannot navigate outside of root directory.");
			return;
		}

		// Now check happy path
		Path fullPath = Paths.get(cwd.getPromptString(rootDirectory), path);
		File directory = fullPath.toFile();

		if (!directory.exists() || !directory.isDirectory()) {
			System.out.println("Directory does not exist or is not accessible: " + fullPath);
			return;
		}

		File[] filesAndDirs = directory.listFiles();
		if (filesAndDirs == null || filesAndDirs.length == 0) {
			System.out.println("Directory is empty.");
		} else {
			System.out.println(
					"Contents of " + cwd.getPromptString(rootDirectory) + (path.isEmpty() ? "" : "/" + path) + ":");
			List<String> fileNames = Arrays.stream(filesAndDirs).map(File::getName).sorted()
					.collect(Collectors.toList());

			for (String fileName : fileNames) {
				System.out.println("  " + fileName);
			}
		}
	}

	// Changes the current working directory
	private void cd(String path) {
		if (path.isEmpty()) {
			System.out.println("Usage: cd <directory>");
			return;
		}

		// handle reset to root and up one directory
		if (path.equals("/")) {
			cwd.changeDirectory(path);
			System.out.println("Changed directory to: " + cwd.getPromptString(rootDirectory));
			return;
		} else if (path.equals("..")) {
			cwd.changeDirectory(path);
			System.out.println("Changed directory to: " + cwd.getPromptString(rootDirectory));
			return;
		}
		// otherwise, proceed to more complex requests

		String currentFullPath = cwd.getPromptString(rootDirectory);
		Path currentPath = Paths.get(currentFullPath);
		Path newPath;

		try {
			if (Paths.get(path).isAbsolute()) {
				// Handle absolute path
				newPath = Paths.get(path);
			} else {
				// Handle relative path
				newPath = currentPath.resolve(path).normalize();
			}
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			System.out.printf("%s is an invalid path.", path);
			return;
		}
		// Ensure the new path is still within the root directory
		Path rootPath = Paths.get(cwd.getRootString(rootDirectory));
		if (!newPath.startsWith(rootPath)) {
			System.out.println("Cannot navigate outside of root directory.");
			return;
		}

		if (FileUtils.isValidLocalDirectory(newPath)) {
			cwd.changeDirectory(path);
			System.out.println("Changed directory to: " + cwd.getPromptString(rootDirectory));
		} else {
			System.out.println("Invalid directory path: " + path);
		}
	}

	// User signup process
	private void signup() {
		System.out.print("Enter email address: ");
		String email = scanner.nextLine().trim();

		String username;
		String password;
		String confirmPassword;

		while (true) {
			System.out.print("Enter username: ");
			username = scanner.nextLine().trim();

			Console console = System.console();
			char[] passwordArray;
			if (console != null) {
				passwordArray = console.readPassword("Enter password: ");
			} else {
				System.out.print("Enter password: ");
				passwordArray = scanner.nextLine().trim().toCharArray();
			}
			password = new String(passwordArray);

			// Clear the password array for security
			java.util.Arrays.fill(passwordArray, ' ');

			if (console != null) {
				passwordArray = console.readPassword("Confirm password: ");
			} else {
				System.out.print("Confirm password: ");
				passwordArray = scanner.nextLine().trim().toCharArray();
			}
			confirmPassword = new String(passwordArray);

			// Clear the password array for security
			java.util.Arrays.fill(passwordArray, ' ');

			if (!password.equals(confirmPassword)) {
				System.out.println("Passwords do not match. Please try again.");
			} else if (!PasswordValidator.isValid(password)) {
				System.out.println(PasswordValidator.getValidationMessage(password));
			} else {
				break;
			}
		}

		try {
			userService.createUser(username, password, email);
			System.out.println("Your account has successfully been created!");
			System.out.println("Please log in with your new credentials.");
		} catch (RuntimeException e) {
			System.out.println("An error occurred while trying to create your account: " + e.getMessage());
		}
	}

	// Deletes the user's account
	private void deleteAccount() {

		System.out.println(
				"Warning: proceeding will permanently delete your account and you will lose all files backed up in the cloud. Continue? (y/n)");
		String confirmation = scanner.nextLine().trim().toLowerCase();

		if (confirmation.equals("y")) {
			try {
				// delete cloud bucket holding their data
				storageService.deleteDirectory(currentUser.getUserId());
				// delete account
				userService.deleteUser(accessToken);
				// end user session
				currentUser = null;
				accessToken = null;
				cwd = null;
				rootDirectory = null;
				System.out.println("Your account has successfully been deleted!");

			} catch (RuntimeException e) {
				System.out.println("An error occurred while trying to delete your account: " + e.getMessage());
			}
		} else {
			System.out.println("Delete account operation cancelled.");
		}

	}

	// Prints help information
	private void printHelp() {
		System.out.println("Available commands:");
		System.out.println("  signup - Sign up for a new account");
		System.out.println("  login - Log in to your account");
		System.out.println("  logout - Log out of your account");
		System.out.println("  delete_account - Permanently delete your account");
		System.out.println("  push - Upload all local files and folders to cloud storage");
		System.out.println("  pull - Download all file files and folders from cloud to local machine");
		System.out.println("  mkdir <folder_name> - Make a new directory at the specified location");
		System.out.println("  cd <path> - Change current directory to the specified path");
		System.out.println("  ls <path> - Display contents of current folder or specified path");
		System.out.println("  rm <path> - Delete a directory and its contents both locally and from cloud");
		System.out.println("  chang_root <path> - Set a new root directory for your dropbox-clone files");
		System.out.println("  help - Show this help message");
		System.out.println("  exit - Exit the application");
	}
}