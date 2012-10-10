package ru.imobilco.builder.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileEntity {
	private String baseDir;
	private String filePath;
	
	/**
	 * @param baseDir File's base dir
	 * @param filePath File's path, relative to baseDir
	 */
	public FileEntity(String baseDir, String filePath) {
		this.baseDir = baseDir;
		this.filePath = filePath;
	}
	
	public String getBaseDir() {
		return baseDir;
	}
	
	public String getFilePath() {
		return filePath;
	}
	
	public File getFile() {
		return new File(baseDir, filePath);
	}
	
	public String getAbsolutePath() {
		return getFile().getAbsolutePath();
	}
	
	public InputStream getInputStream() {
		try {
			return new FileInputStream(getFile());
		} catch (IOException e) { }
		
		return null;
	}
}
