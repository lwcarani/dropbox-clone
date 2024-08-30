package io.github.lwcarani.service;

import com.amazonaws.services.cognitoidp.model.AuthenticationResultType;

import io.github.lwcarani.model.User;

public interface UserService {
	AuthenticationResultType authenticateUser(String username, String password);

	boolean authenticateUserSession(String accessToken);

	User createUser(String username, String password, String email);

	void logout(String accessToken);

	void deleteUser(String accessToken);
}