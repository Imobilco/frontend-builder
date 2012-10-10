package ru.imobilco.builder.logger;

import java.io.File;

import org.apache.tools.ant.util.FileUtils;

public class DefaultPathProcessor implements IPathProcessor {
	private File baseDir;
	
	public DefaultPathProcessor(File baseDir) {
		this.baseDir = baseDir;
	}

	public String getPath(String path) {
		try {
			if (baseDir != null)
				return FileUtils.getRelativePath(baseDir, new File(path));
		} catch (Exception e) {}
		
		return path;
	}
}
