package io.github.lwcarani;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.lwcarani.model.CurrentWorkingDirectory;
import io.github.lwcarani.model.User;
import io.github.lwcarani.service.StorageService;
import io.github.lwcarani.service.UserPreferenceService;
import io.github.lwcarani.service.UserService;

public class DropboxCliRunnerTest {

	@Mock
	private UserService userService;
	@Mock
	private StorageService storageService;
	@Mock
	private UserPreferenceService preferenceService;

	private CurrentWorkingDirectory cwd;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void testLogin_Success() {
		// Arrange
		String username = "testUser";
		String email = "test@example.com";
		String userId = "testUserId";
		User user = new User(username, email, userId);

		// Assert
		assertTrue(user.getUserId().equals(userId));
		assertTrue(user.getEmail().equals(email));
		assertTrue(user.getUsername().equals(username));
	}

	@Test
	public void testSetRootDirectory_Success() {
		// Arrange
		String username = "testUser";
		String userId = "id-13435235-ferejlkg-234f";
		String rootDirectory = "C:/";

		cwd = new CurrentWorkingDirectory(userId, username);

		String fullPath = cwd.getFullPath();
		String promptString = cwd.getPromptString(rootDirectory);

		String expectedFullPath = "id-13435235-ferejlkg-234f";
		String expectedPromptString = "C:\\dropbox-clone\\testUser";

		Path path1 = Paths.get(fullPath).normalize();
		Path path2 = Paths.get(expectedFullPath).normalize();

		assertTrue(path1.equals(path2), "The normalized paths should be equal");

		path1 = Paths.get(promptString).normalize();
		path2 = Paths.get(expectedPromptString).normalize();

		assertTrue(path1.equals(path2), "The normalized paths should be equal");

		////////////////////////////////

		cwd.changeDirectory("foo/bar");

		fullPath = cwd.getFullPath();
		promptString = cwd.getPromptString(rootDirectory);

		expectedFullPath = "id-13435235-ferejlkg-234f\\foo\\bar";
		expectedPromptString = "C:\\dropbox-clone\\testUser\\foo\\bar";

		path1 = Paths.get(fullPath).normalize();
		path2 = Paths.get(expectedFullPath).normalize();

		assertTrue(path1.equals(path2), "The normalized paths should be equal");

		path1 = Paths.get(promptString).normalize();
		path2 = Paths.get(expectedPromptString).normalize();

		assertTrue(path1.equals(path2), "The normalized paths should be equal");

		////////////////////////////////

		cwd.changeDirectory("zoo/goo/boo");

		fullPath = cwd.getFullPath();
		promptString = cwd.getPromptString(rootDirectory);

		expectedFullPath = "id-13435235-ferejlkg-234f\\foo\\bar\\zoo\\goo\\boo";
		expectedPromptString = "C:\\dropbox-clone\\testUser\\foo\\bar\\zoo\\goo\\boo";

		path1 = Paths.get(fullPath).normalize();
		path2 = Paths.get(expectedFullPath).normalize();

		assertTrue(path1.equals(path2), "The normalized paths should be equal");

		path1 = Paths.get(promptString).normalize();
		path2 = Paths.get(expectedPromptString).normalize();

		assertTrue(path1.equals(path2), "The normalized paths should be equal");

		////////////////////////////////

		cwd.changeDirectory("..");

		fullPath = cwd.getFullPath();
		promptString = cwd.getPromptString(rootDirectory);

		expectedFullPath = "id-13435235-ferejlkg-234f\\foo\\bar\\zoo\\goo";
		expectedPromptString = "C:\\dropbox-clone\\testUser\\foo\\bar\\zoo\\goo";

		path1 = Paths.get(fullPath).normalize();
		path2 = Paths.get(expectedFullPath).normalize();

		assertTrue(path1.equals(path2), "The normalized paths should be equal");

		path1 = Paths.get(promptString).normalize();
		path2 = Paths.get(expectedPromptString).normalize();

		assertTrue(path1.equals(path2), "The normalized paths should be equal");

		////////////////////////////////

		cwd.changeDirectory("/");

		fullPath = cwd.getFullPath();
		promptString = cwd.getPromptString(rootDirectory);

		expectedFullPath = "id-13435235-ferejlkg-234f";
		expectedPromptString = "C:\\dropbox-clone\\testUser";

		path1 = Paths.get(fullPath).normalize();
		path2 = Paths.get(expectedFullPath).normalize();

		assertTrue(path1.equals(path2), "The normalized paths should be equal");

		path1 = Paths.get(promptString).normalize();
		path2 = Paths.get(expectedPromptString).normalize();

		assertTrue(path1.equals(path2), "The normalized paths should be equal");
	}
}