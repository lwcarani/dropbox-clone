//package io.github.lwcarani;
//
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import java.lang.reflect.Method;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mock;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import io.github.lwcarani.cli.DropboxCliRunner;
//import io.github.lwcarani.model.CurrentWorkingDirectory;
//import io.github.lwcarani.model.User;
//import io.github.lwcarani.service.StorageService;
//import io.github.lwcarani.service.UserPreferenceService;
//import io.github.lwcarani.service.UserService;
//
//@SpringBootTest
//class DropboxCloneApplicationTests {
//
//	@Mock
//	private UserService userService;
//
//	@Mock
//	private StorageService storageService;
//
//	@Mock
//	private UserPreferenceService preferenceService;
//
//	private DropboxCliRunner cliRunner;
//
//	@BeforeEach
//	void setUp() {
//		cliRunner = new DropboxCliRunner(userService, storageService, preferenceService);
//	}
//
//	@Test
//	void testMkdir() throws Exception {
//		// Setup
//		String folderName = "testFolder";
//		String fullPath = "userId/testPath";
//		String rootDirectory = "/root";
//
//		// Mock CurrentWorkingDirectory and User
//		CurrentWorkingDirectory cwd = mock(CurrentWorkingDirectory.class);
//		when(cwd.getFullPath()).thenReturn(fullPath);
//		when(cwd.getPromptString(rootDirectory)).thenReturn(rootDirectory + "/testPath");
//
//		// Set private fields using reflection
//		setPrivateField(cliRunner, "cwd", cwd);
//		setPrivateField(cliRunner, "rootDirectory", rootDirectory);
//		setPrivateField(cliRunner, "currentUser", new User("testUser", "test@example.com", "userId"));
//
//		// Execute
//		invokePrivateMethod(cliRunner, "mkdir", new Class<?>[] { String.class }, folderName);
//
//		// Verify
//		verify(storageService).createFolder(fullPath, folderName);
//		// Add more verifications as needed
//	}
//
//	@Test
//	void testCd() throws Exception {
//		// Setup
//		String path = "newPath";
//		String rootDirectory = "/root";
//
//		CurrentWorkingDirectory cwd = mock(CurrentWorkingDirectory.class);
//		when(cwd.getPromptString(rootDirectory)).thenReturn(rootDirectory + "/currentPath");
//
//		setPrivateField(cliRunner, "cwd", cwd);
//		setPrivateField(cliRunner, "rootDirectory", rootDirectory);
//
//		// Execute
//		invokePrivateMethod(cliRunner, "cd", new Class<?>[] { String.class }, path);
//
//		// Verify
//		verify(cwd).changeDirectory(path);
//		// Add more verifications as needed
//	}
//
//	// Add more test methods for other functions...
//
//	private void setPrivateField(Object object, String fieldName, Object fieldValue) throws Exception {
//		java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
//		field.setAccessible(true);
//		field.set(object, fieldValue);
//	}
//
//	private void invokePrivateMethod(Object object, String methodName, Class<?>[] parameterTypes, Object... args)
//			throws Exception {
//		Method method = object.getClass().getDeclaredMethod(methodName, parameterTypes);
//		method.setAccessible(true);
//		method.invoke(object, args);
//	}
//
//	@Test
//	void contextLoads() {
//		// This test ensures that the Spring context loads correctly
//		assertNotNull(userService);
//		assertNotNull(storageService);
//		assertNotNull(preferenceService);
//	}
//}