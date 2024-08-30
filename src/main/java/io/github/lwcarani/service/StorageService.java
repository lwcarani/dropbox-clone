package io.github.lwcarani.service;

import java.nio.file.Path;
import java.util.List;

public interface StorageService {
	void createFolder(String fullPath, String folderName);

	void uploadFile(String fullPath, Path localFilePath, String remotePath);

	List<String> listFiles(String fullPath, String path);

	void deleteFile(String fullPath, String filePath);

	byte[] downloadFile(String fullPath, String filePath);

	void syncLocalChanges(String rootDirectory, String userId);

	void pushToS3(String userId, String username, String rootDirectory);

	void pullFromS3(String userId, String username, String rootDirectory);

	void deleteDirectory(String fullPath);

	boolean isValidS3Directory(String fullPath);
}