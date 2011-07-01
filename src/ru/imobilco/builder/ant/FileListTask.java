package ru.imobilco.builder.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;

import ru.imobilco.builder.logger.BundleLogger;


/**
 * Abstract task implementation with file lists
 * @author sergey
 *
 */
public abstract class FileListTask extends Task {
	private ArrayList<FileEntity> fileList;
	private String outputEncoding = "UTF-8";
	
	/**
     * Add a set of files to compile.
     * @param set a set of files to compile.
     */
    public void addConfiguredFileset(FileSet set) {
    	String baseDir = set.getDir(getProject()).getAbsolutePath();
    	DirectoryScanner scanner = set.getDirectoryScanner(getProject());
    	
    	String[] fileNames = scanner.getIncludedFiles();
    	for (int i = 0; i < fileNames.length; i++) {
			getFileList().add(new FileEntity(baseDir, fileNames[i]));
		}
    }
    
    /**
     * List of files to compile.
     * @param list the list of files
     */
    public void addConfiguredFilelist(FileList list) {
    	File baseDir = list.getDir(getProject());
    	String[] fileNames = list.getFiles(getProject());
    	
    	String baseDirPath = baseDir.getAbsolutePath();
    	
    	for (int i = 0; i < fileNames.length; i++) {
			getFileList().add(new FileEntity(baseDirPath, fileNames[i]));
		}
    }
    
    public ArrayList<FileEntity> getFileList() {
    	if (fileList == null)
    		fileList = new ArrayList<FileEntity>();
    	
    	return fileList;
    }
    
    public void writeFile(String data, File file, List<FileEntity> fileList) throws IOException {
    	if (data == null)
    		return;
    	
		if (file.getParentFile().mkdirs()) {
			log("Created missing parent directory " + file.getParentFile(), Project.MSG_DEBUG);
		}
		
		OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), outputEncoding);
		out.append(data);
		out.flush();
		out.close();
		
		// save catalog
		BundleLogger logger = BundleLogger.getSingleton(getProject());
		logger.addToCatalog(file, fileList);
		logger.saveCatalog();
	}
}
