package ru.imobilco.builder.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import ru.imobilco.builder.logger.BundleLogger;
import ru.imobilco.builder.logger.XSLCatalogPathProcessor;

public class SaveCatalog extends Task {
	private File webRoot;
	private File destFile;
	private boolean ensureAbsolute = true;
	
	public void setWebroot(File webRoot) {
		this.webRoot = webRoot;
	}
	
	public void setDestfile(File destFile) {
		this.destFile = destFile;
	}
	
	public void setAbsolute(boolean ensureAbsolute) {
		this.ensureAbsolute = ensureAbsolute;
	}
	
	public void validate() {
		if (webRoot == null) {
			throw new BuildException("'webroot' is not defined");
		}
		if (destFile == null) {
			throw new BuildException("'destfile' is not defined");
		}
		
		if (webRoot.isFile()) {
			throw new BuildException("Web root should be a directory");
		}
		
		if (!webRoot.exists()) {
			throw new BuildException("Web root directory " + webRoot + " does not exists");
		}
	}
	
	public void execute() {
		validate();
		BundleLogger.getSingleton(getProject()).saveCatalog(new XSLCatalogPathProcessor(webRoot, ensureAbsolute), destFile);
		log("Bundle catalog saved");
	}
}
