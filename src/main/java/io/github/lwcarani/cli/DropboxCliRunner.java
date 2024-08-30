package io.github.lwcarani.cli;

import java.io.File;
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
			System.out.print(cwd.getPromptString() + "> ");
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
				FileUtils.createLocalDirectory(rootDirectory, currentUser.getUsername());
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
		FileUtils.createLocalDirectory(rootDirectory, cwd.getPromptString() + "/" + folderName);
	}

	private void ls(String path) {
		Path fullPath = Paths.get(rootDirectory, "dropbox-clone", cwd.getPromptString(), path);
		File directory = fullPath.toFile();

		if (!directory.exists() || !directory.isDirectory()) {
			System.out.println("Directory does not exist or is not accessible: " + fullPath);
			return;
		}

		File[] filesAndDirs = directory.listFiles();
		if (filesAndDirs == null || filesAndDirs.length == 0) {
			System.out.println("Directory is empty.");
		} else {
			System.out.println("Contents of " + cwd.getPromptString() + (path.isEmpty() ? "" : "/" + path) + ":");
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

		String newPath = cwd.getPromptString() + "/" + path;
		if (FileUtils.isValidLocalDirectory(rootDirectory, newPath)) {
			cwd.changeDirectory(path);
			System.out.println("Changed directory to: " + cwd.getPromptString());
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
			password = scanner.nextLine().trim(); // TODO - In a real app, use a secure method to read passwords

			System.out.print("Confirm password: ");
			confirmPassword = scanner.nextLine().trim(); // In a real app, use a secure method to read passwords

			if (!password.equals(confirmPassword)) {
				System.out.println("Passwords do not match. Please try again.");
			} else {
				break;
			}
		}

		try {
			System.out.println("Your account has successfully been created!");
			System.out.println("Please log in with your new credentials.");
		} catch (RuntimeException e) {
			System.out.println("An error occurred while trying to create your account: " + e.getMessage());
		}
	}

	private void printHelp() {
		System.out.println("Available commands:");
		System.out.println("  signup - Sign up for a new account");
		System.out.println("  login - Log in to your account");
		System.out.println("  logout - Log out of your account");
		System.out.println("  mkdir <folder_name> - Make a new directory in the current location");
		System.out.println("  cd <path> - Change current directory");
		System.out.println("  ls [path] - Display contents of current folder or specified path");
		System.out.println("  help - Show this help message");
		System.out.println("  exit - Exit the application");
	}
}