package ru.imobilco.builder.logger;

import java.io.File;

import org.apache.tools.ant.util.FileUtils;


public class XSLCatalogPathProcessor implements IPathProcessor {
	private File container;
	private boolean ensureAbsolute;
	
	public XSLCatalogPathProcessor(File container, boolean ensureAbsolute) {
		this.container = container;
		this.ensureAbsolute = ensureAbsolute;
	}

	@Override
	public String getPath(String path) {
		File f = new File(path);
		String filePath = path;
		
		try {
			filePath = FileUtils.getRelativePath(container, f);
			if (ensureAbsolute) {
				if (filePath.startsWith("./")) {
					filePath = filePath.substring(1);
				} else if (!filePath.startsWith("..") && !filePath.startsWith("/")) {
					filePath = "/" + filePath;
				}
			}
		} catch (Exception e) { }
		
		return filePath;
	}
}
