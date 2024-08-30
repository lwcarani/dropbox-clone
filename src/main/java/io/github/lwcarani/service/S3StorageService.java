package io.github.lwcarani.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import jakarta.annotation.PostConstruct;

@Service
public class S3StorageService implements StorageService {
	private AmazonS3 s3Client;

	@Value("${aws.s3.bucket-user-storage}")
	private String bucketName;

	@Value("${aws.accessKey}")
	private String accessKey;

	@Value("${aws.secretKey}")
	private String secretKey;

	@Value("${aws.region}")
	private String region;

	@PostConstruct
	public void init() {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
		this.s3Client = AmazonS3ClientBuilder.standard().withRegion(Regions.fromName(region))
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
	}

	@Override
	public void createFolder(String fullPath, String folderName) {
		String folderKey = fullPath + "/" + folderName + "/";

		try {
			// Check if the folder already exists
			ListObjectsV2Request listRequest = new ListObjectsV2Request().withBucketName(bucketName)
					.withPrefix(folderKey).withMaxKeys(1);
			ListObjectsV2Result listResult = s3Client.listObjectsV2(listRequest);

			if (!listResult.getObjectSummaries().isEmpty()) {
				System.out.println("Folder already exists in S3: " + folderName);
				return;
			}

			// If the folder doesn't exist, create it
			s3Client.putObject(bucketName, folderKey, "");
			System.out.println("Folder created successfully in S3: " + folderName);
		} catch (Exception e) {
			System.err.println("Couldn't create folder in S3: " + e.getMessage());
		}
	}

	@Override
	public List<String> listFiles(String fullPath, String path) {
		String prefix = fullPath + (path.isEmpty() ? "" : "/" + path);
		ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName).withPrefix(prefix)
				.withDelimiter("/");
		try {
			ListObjectsV2Result result = s3Client.listObjectsV2(req);
			List<String> files = result.getObjectSummaries().stream().map(S3ObjectSummary::getKey)
					.map(key -> key.substring(prefix.length())).filter(key -> !key.isEmpty())
					.collect(Collectors.toList());
			files.addAll(result.getCommonPrefixes());
			return files;
		} catch (Exception e) {
			System.err.println("Couldn't list files in S3: " + e.getMessage());
		}
		return List.of();
	}

	@Override
	public boolean isValidDirectory(String currentPath, String newPath) {
		if (newPath.equals("..")) {
			String parentPath = getParentPath(currentPath);
			return !parentPath.equals(currentPath);
		}

		String fullPath = newPath.startsWith("/") ? currentPath.split("/")[0] + newPath : currentPath + "/" + newPath;
		fullPath = fullPath.endsWith("/") ? fullPath : fullPath + "/";

		try {
			ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName).withPrefix(fullPath)
					.withDelimiter("/").withMaxKeys(1);
			ListObjectsV2Result result = s3Client.listObjectsV2(req);
			return !result.getCommonPrefixes().isEmpty() || !result.getObjectSummaries().isEmpty();
		} catch (Exception e) {
			System.err.println("Couldn't validate directory in S3: " + e.getMessage());
		}
		return false;
	}

	private String getParentPath(String path) {
		List<String> components = List.of(path.split("/"));
		if (components.size() <= 1) {
			return path;
		}
		return String.join("/", components.subList(0, components.size() - 1));
	}

	@Override
	public void uploadFile(String fullPath, Path localFilePath, String remotePath) {
		System.out.format("Uploading %s to S3 bucket %s...\n", localFilePath, bucketName);
		String fileKey = fullPath + "/" + remotePath;
		try {
			PutObjectResult result = s3Client.putObject(bucketName, fileKey, localFilePath.toFile());
			System.out.println("File uploaded successfully to S3: " + remotePath);
		} catch (AmazonServiceException e) {
			System.err.println("Amazon S3 couldn't upload file: " + e.getErrorMessage());
		} catch (SdkClientException e) {
			System.err.println("SDK Client couldn't upload file: " + e.getMessage());
		}
	}

//	@Override
//	public boolean isValidDirectory(String currentPath, String newPath) {
//		String fullPath = currentPath;
//		if (!newPath.startsWith("/")) {
//			fullPath += "/" + newPath;
//		} else {
//			fullPath = currentPath.split("/")[0] + newPath;
//		}
//
//		try {
//			ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName).withPrefix(fullPath)
//					.withDelimiter("/").withMaxKeys(1);
//			ListObjectsV2Result result = s3Client.listObjectsV2(req);
//			return !result.getCommonPrefixes().isEmpty() || !result.getObjectSummaries().isEmpty();
//		} catch (AmazonServiceException e) {
//			System.err.println("Amazon S3 couldn't validate directory: " + e.getErrorMessage());
//		} catch (SdkClientException e) {
//			System.err.println("SDK Client couldn't validate directory: " + e.getMessage());
//		}
//		return false;
//	}

//	@Override
//	public List<String> listFiles(String userId, String path) {
//		System.out.format("Listing objects in %s/%s:\n", userId, path);
//		ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName)
//				.withPrefix(userId + "/" + path);
//		try {
//			ListObjectsV2Result result = s3Client.listObjectsV2(req);
//			return result.getObjectSummaries().stream().map(S3ObjectSummary::getKey).collect(Collectors.toList());
//		} catch (AmazonServiceException e) {
//			System.err.println("Amazon S3 couldn't list files: " + e.getErrorMessage());
//		} catch (SdkClientException e) {
//			System.err.println("SDK Client couldn't list files: " + e.getMessage());
//		}
//		return List.of(); // Return empty list if there's an error
//	}

	@Override
	public void deleteFile(String userId, String filePath) {
		String fileKey = userId + "/" + filePath;
		try {
			s3Client.deleteObject(bucketName, fileKey);
			System.out.println("File deleted successfully: " + filePath);
		} catch (AmazonServiceException e) {
			System.err.println("Amazon S3 couldn't delete file: " + e.getErrorMessage());
		} catch (SdkClientException e) {
			System.err.println("SDK Client couldn't delete file: " + e.getMessage());
		}
	}

	@Override
	public byte[] downloadFile(String userId, String filePath) {
		String fileKey = userId + "/" + filePath;
		try {
			S3Object s3Object = s3Client.getObject(bucketName, fileKey);
			S3ObjectInputStream inputStream = s3Object.getObjectContent();
			try {
				byte[] content = inputStream.readAllBytes();
				System.out.println("File downloaded successfully: " + filePath);
				return content;
			} catch (IOException e) {
				System.err.println("Failed to read file content: " + e.getMessage());
			} finally {
				try {
					inputStream.close();
					s3Object.close();
				} catch (IOException e) {
					System.err.println("Failed to close S3 object: " + e.getMessage());
				}
			}
		} catch (AmazonServiceException e) {
			System.err.println("Amazon S3 couldn't download file: " + e.getErrorMessage());
		} catch (SdkClientException e) {
			System.err.println("SDK Client couldn't download file: " + e.getMessage());
		}
		return new byte[0]; // Return empty byte array if there's an error
	}

	@Override
	public void syncLocalChanges(String rootDirectory, String userId) {
		Path localRoot = Paths.get(rootDirectory, "dropbox-clone", userId);
		try {
			Files.walk(localRoot).filter(Files::isRegularFile).forEach(file -> {
				String relativePath = localRoot.relativize(file).toString();
				uploadFile(userId, file, relativePath);
			});
		} catch (IOException e) {
			System.err.println("Failed to sync local changes: " + e.getMessage());
		}
	}
}