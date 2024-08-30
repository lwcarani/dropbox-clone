package io.github.lwcarani.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.lwcarani.model.User;

public class SimpleUserService implements UserService {

	private final Map<String, User> users = new HashMap<>();
	private final Map<String, String> activeSessions = new HashMap<>();

	public SimpleUserService() {
		// Add some dummy users for testing
		createUser("johndoe", "password123", "john@example.com");
		createUser("janedoe", "password456", "jane@example.com");
	}

	@Override
	public User authenticateUser(String username, String password) {
		User user = users.get(username);
		if (user != null && password.equals(user.getPassword())) {
			String sessionId = UUID.randomUUID().toString();
			activeSessions.put(user.getId(), sessionId);
			return user;
		}
		return null;
	}

	@Override
	public boolean authenticateUserSession(String userId) {
		if (activeSessions.containsKey(userId)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public User createUser(String username, String password, String email) {
		if (users.containsKey(username)) {
			throw new RuntimeException("Username already exists");
		}
		User newUser = new User(UUID.randomUUID().toString(), username, email, password);
		users.put(username, newUser);
		return newUser;
	}

	@Override
	public void logout(String userId) {
		activeSessions.remove(userId);
	}

	@Override
	public void deleteUser(String userId) {
		users.values().removeIf(user -> user.getId().equals(userId));
	}
}