package io.github.lwcarani.cli;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.amazonaws.services.cognitoidp.model.AuthenticationResultType;

import io.github.lwcarani.model.CurrentWorkingDirectory;
import io.github.lwcarani.model.User;
import io.github.lwcarani.service.CognitoUserService;
import io.github.lwcarani.service.StorageService;
import io.github.lwcarani.service.UserPreferenceService;
import io.github.lwcarani.service.UserService;
import io.github.lwcarani.util.FileUtils;

@Component
public class DropboxCliRunner {

	private final UserService userService;
	private final StorageService storageService;
	private final UserPreferenceService preferenceService;
	private Scanner scanner;
	private User currentUser;
	private String accessToken;
	private CurrentWorkingDirectory cwd;
	private String rootDirectory;
	private volatile boolean running;

	public DropboxCliRunner(UserService userService, StorageService storageService,
			UserPreferenceService preferenceService) {
		this.userService = userService;
		this.storageService = storageService;
		this.preferenceService = preferenceService;
		this.scanner = new Scanner(System.in);
		this.running = true;
	}

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
	}

	private void displayPrompt() {
		if (currentUser == null) {
			System.out.print("> ");
		} else {
			System.out.print(cwd.getPromptString(rootDirectory) + "> ");
		}
		System.out.flush(); // Ensure the prompt is displayed before waiting for input
	}

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

	private void login() {
		if (currentUser != null) {
			System.out.println("You are already logged in as " + currentUser.getUsername());
			return;
		}

		try {
			System.out.print("Enter username: ");
			String username = scanner.nextLine().trim();

			System.out.print("Enter password: ");
			String password = scanner.nextLine().trim();

			AuthenticationResultType authResult = userService.authenticateUser(username, password);
			if (authResult != null && authResult.getAccessToken() != null) {
				accessToken = authResult.getAccessToken();
				String userId = ((CognitoUserService) userService).getUserId(accessToken);
				String email = ((CognitoUserService) userService).getEmail(accessToken);

				System.out.println("Welcome back, user!");
				System.out.println("username: " + username);
				System.out.println("userId: " + userId);
				System.out.println("Email: " + email);
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
		preferenceService.saveUserPreference(currentUser.getUserId(), "rootDirectory", rootDirectory);
		System.out.println("Root directory changed to: " + rootDirectory);
		FileUtils.createRootDirectory(rootDirectory);
	}

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

	private void mkdir(String folderName) {
		if (folderName.isEmpty()) {
			System.out.println("Usage: mkdir <folder_name>");
			return;
		}

		storageService.createFolder(cwd.getFullPath(), folderName);
		FileUtils.createLocalDirectory(cwd.getPromptString(rootDirectory) + "/" + folderName);
	}

	private void rm(String path) {
		if (path.isEmpty()) {
			System.out.println("Usage: rm <path>");
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

	private void ls(String path) {
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
				System.out.println(fileName);
			}
		}
	}

	private void cd(String path) {
		if (path.isEmpty()) {
			System.out.println("Usage: cd <directory>");
			return;
		}

		String newPath = cwd.getPromptString(rootDirectory) + "/" + path;
		if (FileUtils.isValidLocalDirectory(newPath)) {
			cwd.changeDirectory(path);
			System.out.println("Changed directory to: " + cwd.getPromptString(rootDirectory));
		} else {
			System.out.println("Invalid directory path: " + path);
		}
	}

	private void signup() {
		System.out.print("Enter email address: ");
		String email = scanner.nextLine().trim();

		String username;
		String password;
		String confirmPassword;

		while (true) {
			System.out.print("Enter username: ");
			username = scanner.nextLine().trim();

			System.out.print("Enter password: ");
			password = scanner.nextLine().trim();

			System.out.print("Confirm password: ");
			confirmPassword = scanner.nextLine().trim();

			if (!password.equals(confirmPassword)) {
				System.out.println("Passwords do not match. Please try again.");
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