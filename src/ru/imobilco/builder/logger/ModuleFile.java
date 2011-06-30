package ru.imobilco.builder.logger;

import java.io.File;


/**
 * A single file that belongs to a parent compiled file
 */
public class ModuleFile {
	private String fileName;
	private String md5;
	
	public ModuleFile(String fileName) {
		this.fileName = fileName;
	}
	
	public ModuleFile(String fileName, String md5) {
		this(fileName);
		this.md5 = md5;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public String getMd5() {
		if (md5 == null) {
			try {
				md5 = MD5Checksum.getMD5Checksum(fileName);
			} catch (Exception e) {}
		}
		
		return md5;
	}
	
	public boolean isSameContent(File file) {
		try {
			return getMd5().equals(MD5Checksum.getMD5Checksum(file));
		} catch (Exception e) {}
		
		return false;
	}
	
	public String toXml() {
		return toXml(null);
	}
	
	public String toXml(IPathProcessor pathProcessor) {
		String filePath = getFileName();
		if (pathProcessor != null)
			filePath = pathProcessor.getPath(filePath);
		
		return "<file src=\"" + filePath + "\" md5=\"" + getMd5() + "\" />";
	}
}
