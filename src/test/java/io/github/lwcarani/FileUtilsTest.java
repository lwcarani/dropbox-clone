package io.github.lwcarani;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.lwcarani.util.FileUtils;

public class FileUtilsTest {

	@TempDir
	Path tempDir;

	@Test
	public void testCreateLocalDirectory_Success() {
		String dirPath = tempDir.resolve("testDir").toString();
		FileUtils.createLocalDirectory(dirPath);

		assertTrue(Files.exists(Path.of(dirPath)));
		assertTrue(Files.isDirectory(Path.of(dirPath)));
	}

	@Test
	public void testCreateRootDirectory_Success() {
		String rootPath = tempDir.toString();
		FileUtils.createRootDirectory(rootPath);

		Path dropboxClonePath = Path.of(rootPath, "dropbox-clone");
		assertTrue(Files.exists(dropboxClonePath));
		assertTrue(Files.isDirectory(dropboxClonePath));
	}

	@Test
	public void testCreateLocalFile_Success() {
		String rootPath = tempDir.toString();
		String relativePath = "testFile.txt";
		byte[] content = "Test content".getBytes();

		FileUtils.createLocalFile(rootPath, relativePath, content);

		Path filePath = Path.of(rootPath, "dropbox-clone", relativePath);
		assertTrue(Files.exists(filePath));
		assertTrue(Files.isRegularFile(filePath));
	}

	@Test
	public void testIsValidLocalDirectory_Success() {
		String dirPath = tempDir.resolve("testDir").toString();
		FileUtils.createLocalDirectory(dirPath);

		assertTrue(FileUtils.isValidLocalDirectory(dirPath));
	}
}