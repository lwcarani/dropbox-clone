package io.github.lwcarani.service;

/**
 * Interface for managing user preferences. This service provides methods to
 * save, retrieve, and delete user-specific preferences.
 */
public interface UserPreferenceService {

	/**
	 * Saves a user preference.
	 *
	 * @param userId The unique identifier of the user
	 * @param key    The key of the preference to save
	 * @param value  The value of the preference to save
	 */
	void saveUserPreference(String userId, String key, String value);

	/**
	 * Retrieves a user preference.
	 *
	 * @param userId The unique identifier of the user
	 * @param key    The key of the preference to retrieve
	 * @return The value of the requested preference, or null if not found
	 */
	String getUserPreference(String userId, String key);

	/**
	 * Deletes a user preference.
	 *
	 * @param userId The unique identifier of the user
	 * @param key    The key of the preference to delete
	 */
	void deleteUserPreference(String userId, String key);
}