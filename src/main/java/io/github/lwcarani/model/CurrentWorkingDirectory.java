package io.github.lwcarani.model;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CurrentWorkingDirectory {
	private final String userId;
	private final String username;
	private final List<String> pathComponents;

	public CurrentWorkingDirectory(String userId, String username) {
		this.userId = userId;
		this.username = username;
		this.pathComponents = new ArrayList<>();
	}

	public String getFullPath() {
		StringBuilder path = new StringBuilder(userId);
		for (String component : pathComponents) {
			path.append("/").append(component);
		}
		return path.toString();
	}

	public String getPromptString(String rootDirectory) {
		String cleanRoot = rootDirectory.endsWith("/") || rootDirectory.endsWith("\\")
				? rootDirectory.substring(0, rootDirectory.length() - 1)
				: rootDirectory;

		String basePath = Paths.get(cleanRoot, "dropbox-clone", username).toString();

		if (pathComponents.isEmpty()) {
			return basePath;
		}

		return Paths.get(basePath, String.join("/", pathComponents)).toString();
	}

	public void changeDirectory(String path) {
		if (path.equals("/")) {
			pathComponents.clear();
		} else if (path.equals("..")) {
			if (!pathComponents.isEmpty()) {
				pathComponents.remove(pathComponents.size() - 1);
			}
		} else {
			String[] components = path.split("/");
			for (String component : components) {
				if (component.equals("..")) {
					if (!pathComponents.isEmpty()) {
						pathComponents.remove(pathComponents.size() - 1);
					}
				} else if (!component.isEmpty() && !component.equals(".")) {
					pathComponents.add(component);
				}
			}
		}
	}
}