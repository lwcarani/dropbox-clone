package io.github.lwcarani.service;

public interface UserPreferenceService {
	void saveUserPreference(String userId, String key, String value);

	String getUserPreference(String userId, String key);

	void deleteUserPreference(String userId, String key);
}