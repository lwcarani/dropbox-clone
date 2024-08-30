package io.github.lwcarani.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleStorageService implements StorageService {

	private final Map<String, Map<String, String>> userStorage = new HashMap<>();

	@Override
	public void createFolder(String userId, String folderName) {
		getUserStorage(userId).put(folderName, "folder");
		System.out.println("Folder created: " + folderName);
	}

	@Override
	public void uploadFile(String userId, Path localFilePath, String remotePath) {
		getUserStorage(userId).put(remotePath, "file");
		System.out.println("File uploaded: " + remotePath);
	}

	@Override
	public List<String> listFiles(String userId, String path) {
		return new ArrayList<>(getUserStorage(userId).keySet());
	}

	@Override
	public void deleteFile(String userId, String filePath) {
		getUserStorage(userId).remove(filePath);
		System.out.println("File deleted: " + filePath);
	}

	@Override
	public byte[] downloadFile(String userId, String filePath) {
		System.out.println("Downloading file: " + filePath);
		return new byte[0]; // Simulated download
	}

	private Map<String, String> getUserStorage(String userId) {
		return userStorage.computeIfAbsent(userId, k -> new HashMap<>());
	}
}