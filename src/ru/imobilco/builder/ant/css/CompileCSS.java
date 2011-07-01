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
	private File toDir;
	private File webRoot;
	private boolean force = false;
	
	public CompileCSS() {
		
	}
	
	public void setDir(File dir) {
		FileSet fSet = new FileSet();
		fSet.setProject(getProject());
		fSet.setDir(dir);
		fSet.setIncludes("**/*.css");
		fSet.setExcludes("**/_*.css");
		
		addConfiguredFileset(fSet);
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
	
	private void validate() {
		
	}
	
	public void execute() throws BuildException {
		validate();
		
		BundleLogger logger = BundleLogger.getSingleton(getProject());
		
		for (FileEntity f : getFileList()) {
			log("Compiling " + f.getAbsolutePath());
			File outputFile = new File(toDir, f.getFilePath());
			
			CSSCatalog catalog = new CSSCatalog(f.getFile(), webRoot);
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
