package io.github.lwcarani.util;

import java.util.regex.Pattern;

public class PasswordValidator {

	public static boolean isValid(String password) {
		// Check if password is null or empty
		if (password == null || password.trim().isEmpty()) {
			return false;
		}

		// Check minimum length
		if (password.length() < 8) {
			return false;
		}

		// Check for at least one number
		if (!Pattern.compile("[0-9]").matcher(password).find()) {
			return false;
		}

		// Check for at least one uppercase letter
		if (!Pattern.compile("[A-Z]").matcher(password).find()) {
			return false;
		}

		// Check for at least one lowercase letter
		if (!Pattern.compile("[a-z]").matcher(password).find()) {
			return false;
		}

		// Check for at least one special character
		String specialChars = "^$*.[]{}()?-\"!@#%&/\\,><':;|_~`+=";
		String specialCharsPattern = "[" + Pattern.quote(specialChars) + "\\s]";
		if (!Pattern.compile(specialCharsPattern).matcher(password).find()) {
			return false;
		}

		// Check if password starts or ends with a space
		if (password.startsWith(" ") || password.endsWith(" ")) {
			return false;
		}

		// If all checks pass, the password is valid
		return true;
	}

	// Method to provide feedback on why a password is invalid
	public static String getValidationMessage(String password) {
		if (password == null || password.trim().isEmpty()) {
			return "Password cannot be empty or null.";
		}
		if (password.length() < 8) {
			return "Password must be at least 8 characters long.";
		}
		if (!Pattern.compile("[0-9]").matcher(password).find()) {
			return "Password must contain at least one number.";
		}
		if (!Pattern.compile("[A-Z]").matcher(password).find()) {
			return "Password must contain at least one uppercase letter.";
		}
		if (!Pattern.compile("[a-z]").matcher(password).find()) {
			return "Password must contain at least one lowercase letter.";
		}
		String specialChars = "^$*.[]{}()?-\"!@#%&/\\,><':;|_~`+=";
		String specialCharsPattern = "[" + Pattern.quote(specialChars) + "\\s]";
		if (!Pattern.compile(specialCharsPattern).matcher(password).find()) {
			return "Password must contain at least one special character.";
		}
		if (password.startsWith(" ") || password.endsWith(" ")) {
			return "Password cannot start or end with a space.";
		}
		return "Password is valid.";
	}
}