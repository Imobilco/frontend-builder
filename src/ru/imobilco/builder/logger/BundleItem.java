package ru.imobilco.builder.logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ru.imobilco.builder.ant.js.FileEntity;

/**
 * A bundle logger record that contains list of files that where compiled into
 * a single library
 */
public class BundleItem {
	private String parentFile;
	private List<ModuleFile> childFiles;
	
	public BundleItem(String parentFile) {
		this.parentFile = parentFile;
	}
	
	@SuppressWarnings("rawtypes")
	public BundleItem(String parentFile, List childFiles) {
		this(parentFile);
		this.setChildFiles(childFiles);
	}
	
	/**
	 * Check if stored library was modified since last build (e.g. if library 
	 * should be re-compiled)
	 * @param list
	 * @return
	 * @throws Exception 
	 */
	public boolean isModified(List<FileEntity> list) throws Exception {
		if (getChildFiles() == null || getChildFiles().size() != list.size())
			return true;
		
		// compare child files checksums
		for (FileEntity f : list) {
			int ix = getChildIndex(f.getAbsolutePath());
			if (ix == -1 || !getChildFiles().get(ix).isSameContent(f.getFile())) {
				return true;
			}
		}
		
		return false;
	}
	
	public int getChildIndex(String fileName) {
		for (int i = 0; i < getChildFiles().size(); i++) {
			if (getChildFiles().get(i).getFileName().equals(fileName)) {
				return i;
			}
		}
		
		return -1;
	}
	
	public String toXml() { 
		return toXml(null);
	}
	
	public String toXml(IPathProcessor pathProcessor) {
		StringBuilder sb = new StringBuilder();
		String filePath = parentFile;
		if (pathProcessor != null) {
			filePath = pathProcessor.getPath(filePath);
		}
		
		sb.append("<file src=\"" + filePath + "\">\n");
		
		if (getChildFiles() != null) {
			for (ModuleFile mf : getChildFiles()) {
				sb.append("\t" + mf.toXml(pathProcessor) + "\n");
			}
		}
		
		sb.append("</file>");
		return sb.toString();
	}
	
	public String getParentFile() {
		return parentFile;
	}
	
	public boolean isSameFile(String fileName) {
		return parentFile.equals(fileName);
	}

	public List<ModuleFile> getChildFiles() {
		return childFiles;
	}

	@SuppressWarnings("rawtypes")
	public void setChildFiles(List childFiles) {
		this.childFiles = new ArrayList<ModuleFile>();
		
		for (Object f : childFiles) {
			if (f instanceof String) {
				this.getChildFiles().add(new ModuleFile((String) f));
			} else if (f instanceof File) {
				this.getChildFiles().add(new ModuleFile(((File) f).getAbsolutePath()));
			} else if (f instanceof FileEntity) {
				this.getChildFiles().add(new ModuleFile(((FileEntity) f).getAbsolutePath()));
			} else if (f instanceof ModuleFile) {
				this.getChildFiles().add((ModuleFile) f);
			}
		}
	}
}
