package io.github.lwcarani.util;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import javax.swing.JFileChooser;

public class FileUtils {

	// Select root directory based on environment (GUI or console)
	public static String selectRootDirectory() {
		if (GraphicsEnvironment.isHeadless()) {
			return selectRootDirectoryConsole();
		} else {
			return selectRootDirectoryGUI();
		}
	}

	// Select root directory using GUI file chooser
	private static String selectRootDirectoryGUI() {
		JFileChooser fileChooser = new JFileChooser();
		// Set up file chooser properties
		fileChooser.setDialogTitle("Select Root Directory");
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

		int result = fileChooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			return selectedFile.getAbsolutePath();
		}
		return null;
	}

	// Select root directory via console input
	private static String selectRootDirectoryConsole() {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Running in headless mode. Please enter the full path for the root directory:");
		String path = scanner.nextLine().trim();
		File file = new File(path);
		if (file.exists() && file.isDirectory()) {
			return path;
		} else {
			System.out.println("Invalid directory. Using the current working directory.");
			return System.getProperty("user.dir");
		}
	}

	// Create a local directory if it doesn't exist
	public static void createLocalDirectory(String path) {
		Path fullPath = Paths.get(path);

		if (!Files.exists(fullPath)) {
			try {
				Files.createDirectories(fullPath);
//				System.out.println("Local directory created: " + fullPath);
			} catch (IOException e) {
				System.err.println("Failed to create local directory: " + e.getMessage());
			}
		} else {
//			System.out.println("Local directory already exists: " + fullPath);
		}
	}

	// Create the root directory for the application
	public static void createRootDirectory(String rootPath) {
		Path fullPath = Paths.get(rootPath, "dropbox-clone");
		if (!Files.exists(fullPath)) {
			try {
				Files.createDirectories(fullPath);
//				System.out.println("Root directory created: " + fullPath);
			} catch (IOException e) {
				System.err.println("Failed to create root directory: " + e.getMessage());
			}
		} else {
//			System.out.println("Root directory already exists: " + fullPath);
		}
	}

	// Create a local file with given content
	public static void createLocalFile(String rootPath, String relativePath, byte[] content) {
		Path fullPath = Paths.get(rootPath, "dropbox-clone", relativePath);
		try {
			Files.createDirectories(fullPath.getParent());
			Files.write(fullPath, content);
//			System.out.println("Local file created: " + fullPath);
		} catch (IOException e) {
			System.err.println("Failed to create local file: " + e.getMessage());
		}
	}

	// Check if a given path is a valid local directory
	public static boolean isValidLocalDirectory(String path) {
		Path fullPath = Paths.get(path);
		return Files.isDirectory(fullPath);
	}

	// Check if a given path is a valid local directory
	public static boolean isValidLocalDirectory(Path path) {
		return Files.isDirectory(path);
	}
}