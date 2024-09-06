package io.github.lwcarani.model;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// Represents the current working directory for a user in the Dropbox-like system
public class CurrentWorkingDirectory {
	private final String userId;
	private final String username;
	private final List<String> pathComponents;

	// Constructor initializes with userId and username
	public CurrentWorkingDirectory(String userId, String username) {
		this.userId = userId;
		this.username = username;
		this.pathComponents = new ArrayList<>();
	}

	// Returns the full path including userId and all path components
	public String getFullPath() {
		StringBuilder path = new StringBuilder(userId);
		for (String component : pathComponents) {
			path.append("/").append(component);
		}
		return path.toString();
	}

	// Generates a prompt string based on the root directory and current path
	public String getPromptString(String rootDirectory) {
		// Clean the root directory path
		String cleanRoot = rootDirectory.endsWith("/") || rootDirectory.endsWith("\\")
				? rootDirectory.substring(0, rootDirectory.length() - 1)
				: rootDirectory;

		// Construct the base path
		String basePath = Paths.get(cleanRoot, "dropbox-clone", username).toString();

		// Return base path if no additional components, otherwise append components
		if (pathComponents.isEmpty()) {
			return basePath;
		}
		return Paths.get(basePath, String.join("/", pathComponents)).toString();
	}

	// Changes the current directory based on the given path
	public void changeDirectory(String path) {
		if (path.equals("/")) {
			// Reset to root
			pathComponents.clear();
		} else if (path.equals("..")) {
			// Move up one level if possible
			if (!pathComponents.isEmpty()) {
				pathComponents.remove(pathComponents.size() - 1);
			}
		} else {
			// Handle complex paths
			String[] components = path.split("/");
			for (String component : components) {
				if (component.equals("..")) {
					// Move up one level if possible
					if (!pathComponents.isEmpty()) {
						pathComponents.remove(pathComponents.size() - 1);
					}
				} else if (!component.isEmpty() && !component.equals(".")) {
					// Add valid path component
					pathComponents.add(component);
				}
			}
		}
	}
}