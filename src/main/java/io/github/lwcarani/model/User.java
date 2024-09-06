package io.github.lwcarani.model;

// Represents a user in the system
public class User {
	// User's username
	private String username;
	// User's email address
	private String email;
	// Unique identifier for the user
	private String userId;

	// Constructor to initialize a User object
	public User(String username, String email, String userId) {
		this.username = username;
		this.email = email;
		this.userId = userId;
	}

	// Getter for username
	public String getUsername() {
		return username;
	}

	// Setter for username
	public void setUsername(String username) {
		this.username = username;
	}

	// Getter for email
	public String getEmail() {
		return email;
	}

	// Setter for email
	public void setEmail(String email) {
		this.email = email;
	}

	// Getter for userId
	public String getUserId() {
		return userId;
	}

	// Setter for userId
	public void setUserId(String userId) {
		this.userId = userId;
	}
}