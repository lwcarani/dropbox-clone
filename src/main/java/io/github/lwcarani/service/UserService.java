package io.github.lwcarani.service;

import com.amazonaws.services.cognitoidp.model.AuthenticationResultType;

import io.github.lwcarani.model.User;

/**
 * Interface for managing user-related operations. This service provides methods
 * for user authentication, creation, logout, and deletion.
 */
public interface UserService {

	/**
	 * Authenticates a user with their username and password.
	 *
	 * @param username The username of the user
	 * @param password The password of the user
	 * @return An AuthenticationResultType object containing authentication details
	 */
	AuthenticationResultType authenticateUser(String username, String password);

	/**
	 * Authenticates a user session using an access token.
	 *
	 * @param accessToken The access token to validate
	 * @return true if the session is valid, false otherwise
	 */
	boolean authenticateUserSession(String accessToken);

	/**
	 * Creates a new user account.
	 *
	 * @param username The username for the new account
	 * @param password The password for the new account
	 * @param email    The email address associated with the new account
	 * @return A User object representing the newly created user
	 */
	User createUser(String username, String password, String email);

	/**
	 * Logs out a user, invalidating their current session.
	 *
	 * @param accessToken The access token of the user's current session
	 */
	void logout(String accessToken);

	/**
	 * Deletes a user's account.
	 *
	 * @param accessToken The access token of the user to be deleted
	 */
	void deleteUser(String accessToken);
}