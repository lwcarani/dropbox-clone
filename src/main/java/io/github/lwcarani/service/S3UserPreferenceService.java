package io.github.lwcarani.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;

@Service
public class S3UserPreferenceService implements UserPreferenceService {

	private final AmazonS3 s3Client;
	private final String bucketName;

	public S3UserPreferenceService(AmazonS3 s3Client, @Value("${aws.s3.bucket-user-preferences}") String bucketName) {
		this.s3Client = s3Client;
		this.bucketName = bucketName;
	}

	@Override
	public void saveUserPreference(String userId, String key, String value) {
		String objectKey = getUserPreferenceKey(userId, key);
		s3Client.putObject(bucketName, objectKey, value);
	}

	@Override
	public String getUserPreference(String userId, String key) {
		String objectKey = getUserPreferenceKey(userId, key);
		try {
			return s3Client.getObjectAsString(bucketName, objectKey);
		} catch (AmazonS3Exception e) {
			if (e.getStatusCode() == 404) {
				return null; // Preference not found
			}
			throw e; // Re-throw other exceptions
		}
	}

	@Override
	public void deleteUserPreference(String userId, String key) {
		String objectKey = getUserPreferenceKey(userId, key);
		s3Client.deleteObject(bucketName, objectKey);
	}

	private String getUserPreferenceKey(String userId, String key) {
		return userId + "/" + key;
	}
}