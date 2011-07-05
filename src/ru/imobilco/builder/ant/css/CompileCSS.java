package ru.imobilco.builder.ant.css;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;

import ru.imobilco.builder.ant.FileEntity;
import ru.imobilco.builder.ant.FileListTask;
import ru.imobilco.builder.logger.BundleLogger;

import com.yahoo.platform.yui.compressor.CssCompressor;

public class CompileCSS extends FileListTask {
	private File dir;
	private File toDir;
	private File webRoot;
	
	private String includes;
	private String excludes;
	
	private boolean force = false;
	
	public CompileCSS() {
		
	}
	
	public void setDir(File dir) {
		this.dir = dir;
	}
	
	public void setToDir(File dir) {
		this.toDir = dir;
	}
	
	public void setWebRoot(File dir) {
		this.webRoot = dir;
	}
	
	public void setForce(boolean force) {
		this.force = force;
	}
	
	public void setIncludes(String includes) {
		this.includes = includes;
	}
	
	public void setExcludes(String excludes) {
		this.excludes = excludes;
	}
	
	private void configureInput() {
		if (dir != null) {
			FileSet fSet = new FileSet();
			fSet.setProject(getProject());
			fSet.setDir(dir);
			
			String _includes = "**/*.css";
			if (includes != null)
				_includes += "," + includes;
			
			String _excludes = "**/_*.css";
			if (excludes != null)
				_excludes += "," + excludes;
			
			fSet.setIncludes(_includes);
			fSet.setExcludes(_excludes);
			
			addConfiguredFileset(fSet);
		}
	}
	
	private void validate() {
		if (toDir == null) {
			throw new BuildException("Output directory ('todir' attribute) is not specified");
		}
		
		if (webRoot == null) {
			webRoot = getProject().getBaseDir();
		}
		
		if (!webRoot.exists()) {
			throw new BuildException("Web boor directory doen't exists: " + webRoot);
		}
		
		configureInput();
		
		if (getFileList().size() == 0) {
			throw new BuildException("No input CSS files specified");
		}
	}
	
	public void execute() throws BuildException {
		validate();
		
		BundleLogger logger = BundleLogger.getSingleton(getProject());
		
		for (FileEntity f : getFileList()) {
			log("Compiling " + f.getAbsolutePath());
			File outputFile = new File(toDir, f.getFilePath());
			
			CSSCatalog catalog = new CSSCatalog(f.getFile(), webRoot);
			catalog.setEncoding(getEncoding());
			if (!force && !logger.isModified(outputFile, catalog.getCatalogFiles())) {
				log("Nothing to do");
			} else {
				try {
					writeFile(compileWithYUI(catalog), outputFile, catalog.getCatalogFiles());
					log("Compilation done");
				} catch (IOException e) {
					throw new BuildException(e);
				}
			}
		}
	}
	
	private String compileWithYUI(CSSCatalog catalog) throws IOException {
		StringReader reader = new StringReader(catalog.getCombinedCSS());
		CssCompressor compressor = new CssCompressor(reader);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamWriter out = new OutputStreamWriter(baos, getEncoding());
		compressor.compress(out, -1);
		out.close();
		
		return baos.toString(getEncoding());
	}
}
