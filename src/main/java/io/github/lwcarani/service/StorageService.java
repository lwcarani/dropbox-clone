package io.github.lwcarani.service;

import java.nio.file.Path;
import java.util.List;

public interface StorageService {
	void createFolder(String fullPath, String folderName);

	void uploadFile(String fullPath, Path localFilePath, String remotePath);

	List<String> listFiles(String fullPath, String path);

	void deleteFile(String fullPath, String filePath);

	byte[] downloadFile(String fullPath, String filePath);

	boolean isValidDirectory(String currentPath, String newPath);

	void syncLocalChanges(String rootDirectory, String userId);
}