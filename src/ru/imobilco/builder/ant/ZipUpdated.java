package ru.imobilco.builder.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.util.FileUtils;

import ru.imobilco.builder.logger.BundleLogger;

public class ZipUpdated extends Zip {
	private File baseDir;
	
	
	public void execute() {
		List<String> updatedFiles = BundleLogger.getSingleton(getProject()).getUpdatedFiles();
		List<String> relPaths = new ArrayList<String>();
		
		try {
			if (updatedFiles != null && updatedFiles.size() > 0) {
				for (String filePath : updatedFiles) {
					relPaths.add(FileUtils.getRelativePath(baseDir, new File(filePath)));
				}
				
				setIncludes(join(relPaths, ","));
				
				super.execute();
			}
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}
	
	public void setBasedir(File baseDir) {
		this.baseDir = baseDir;
		super.setBasedir(baseDir);
	}
	
	@SuppressWarnings("rawtypes")
	public static String join(Collection s, String delimiter) {
	    StringBuilder buffer = new StringBuilder();
	    Iterator iter = s.iterator();
	    while (iter.hasNext()) {
	        buffer.append(iter.next());
	        if (iter.hasNext()) {
	            buffer.append(delimiter);
	        }
	    }
	    return buffer.toString();
	}
}
