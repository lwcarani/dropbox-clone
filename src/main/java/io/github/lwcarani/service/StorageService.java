package io.github.lwcarani.service;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface for managing storage operations. This service provides methods for
 * interacting with both local and cloud (S3) storage.
 */
public interface StorageService {

	/**
	 * Creates a new folder in the storage.
	 *
	 * @param fullPath   The full path where the folder should be created
	 * @param folderName The name of the folder to create
	 */
	void createFolder(String fullPath, String folderName);

	/**
	 * Uploads a file from the local file system to the storage.
	 *
	 * @param fullPath      The full path in the storage where the file should be
	 *                      uploaded
	 * @param localFilePath The path of the file on the local file system
	 * @param remotePath    The path where the file should be stored in the remote
	 *                      storage
	 */
	void uploadFile(String fullPath, Path localFilePath, String remotePath);

	/**
	 * Lists files in a specified path in the storage.
	 *
	 * @param fullPath The full path to list files from
	 * @param path     The relative path within the full path
	 * @return A list of file names in the specified path
	 */
	List<String> listFiles(String fullPath, String path);

	/**
	 * Deletes a file from the storage.
	 *
	 * @param fullPath The full path of the file to delete
	 * @param filePath The relative path of the file to delete
	 */
	void deleteFile(String fullPath, String filePath);

	/**
	 * Downloads a file from the storage.
	 *
	 * @param fullPath The full path of the file to download
	 * @param filePath The relative path of the file to download
	 * @return The file contents as a byte array
	 */
	byte[] downloadFile(String fullPath, String filePath);

	/**
	 * Pushes local files to S3 storage.
	 *
	 * @param userId        The ID of the user
	 * @param username      The username of the user
	 * @param rootDirectory The root directory on the local file system
	 */
	void pushToS3(String userId, String username, String rootDirectory);

	/**
	 * Pulls files from S3 storage to the local file system.
	 *
	 * @param userId        The ID of the user
	 * @param username      The username of the user
	 * @param rootDirectory The root directory on the local file system
	 */
	void pullFromS3(String userId, String username, String rootDirectory);

	/**
	 * Deletes a directory and its contents from the storage.
	 *
	 * @param fullPath The full path of the directory to delete
	 */
	void deleteDirectory(String fullPath);

	/**
	 * Checks if a given path is a valid S3 directory.
	 *
	 * @param fullPath The full path to check
	 * @return true if the path is a valid S3 directory, false otherwise
	 */
	boolean isValidS3Directory(String fullPath);
}