package ru.imobilco.builder.ant.js;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import ru.imobilco.builder.ant.FileEntity;
import ru.imobilco.builder.logger.BundleLogger;

import com.google.common.collect.Lists;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.MessageFormatter;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.WarningLevel;
import com.google.javascript.jscomp.ant.AntErrorManager;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

public class CompileJS extends Task {
	private String encoding = "UTF-8"; 
	private String outputEncoding = "UTF-8";
	private File destFile;
	private File destDir;
	private boolean useClosure = true;
	private boolean force = false;
	
	private ArrayList<FileEntity> fileList;
	
	public CompileJS() {
		fileList = new ArrayList<FileEntity>();
	}
	
	/**
     * Add a set of files to compile.
     * @param set a set of files to compile.
     */
    public void addConfiguredFileset(FileSet set) {
    	String baseDir = set.getDir(getProject()).getAbsolutePath();
    	DirectoryScanner scanner = set.getDirectoryScanner(getProject());
    	
    	String[] fileNames = scanner.getIncludedFiles();
    	for (int i = 0; i < fileNames.length; i++) {
			fileList.add(new FileEntity(baseDir, fileNames[i]));
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
			fileList.add(new FileEntity(baseDirPath, fileNames[i]));
		}
    }
    
    public void setUseClosure(boolean flag) {
    	useClosure = flag;
    }

    /**
	 * Stores "destfile" task attribute
	 * @param path
	 */
	public void setDestFile(File path) {
		destFile = path;
	}
	
	/**
	 * Stores "destdir" task attribute
	 * @param path
	 */
	public void setDestDir(File path) {
		destDir = path;
	}
	
	public void setEncoding(String enc) {
		encoding = enc;
	}
	
	public void setForce(boolean force) {
		this.force = force;
	}
	
	private void validate() {
		if (destDir == null && destFile == null) {
			throw new BuildException("Either 'destdir' or 'destfile' attribute should be defined");
		}
		
		// check file existence
		for (FileEntity f : fileList) {
			if (!f.getFile().exists()) {
				throw new BuildException("File " + f.getAbsolutePath() + " doesn't exists");	
			}
		}
	}
	
	public void execute() throws BuildException {
		validate();
		
		if (destFile != null) {
			// concat all files into a single one
			compileList(fileList, destFile);
		} else {
			// compile each file individually
			for (int i = 0; i < fileList.size(); i++) {
				FileEntity fe = fileList.get(i);
				File outputFile = new File(destDir, fe.getFilePath());
				compileList(fileList.subList(i, i + 1), outputFile);
			}
		}
		
		log("JS compilation done");
		log(" ");
	}
	
	public void compileList(List<FileEntity> files, File output) {
		log("Compiling " + files.size() + " file(s) as " + output);
		
		BundleLogger logger = BundleLogger.getSingleton(getProject());
		if (!force && !logger.isModified(output, files)) {
			log("Nothing to do");
			return;
		}
		
		String result;
		try {
			if (useClosure) {
				result = compileWithClosure(files);
			} else {
				result = compileWithYUI(files);
			}
		} catch (Exception e) {
			throw new BuildException("Cannot minify files: " + e.getMessage());
		};
		
		if (result != null) {
			try {
				writeFile(result, output);
				logger.addToCatalog(output, files);
				logger.saveCatalog();
				log("Compilation done");
			} catch (IOException e) {
				throw new BuildException("Cannot write result into "  + output + ": " + e.getMessage());
			}
		}
	}
	
	private void writeFile(String data, File file) throws IOException {
		if (file.getParentFile().mkdirs()) {
			log("Created missing parent directory " + file.getParentFile(), Project.MSG_DEBUG);
		}
		
		OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(
				file), outputEncoding);
		out.append(data);
		out.flush();
		out.close();
	}
	
	/**
	 * Compile input data with Google Closure Compiler
	 * @param input
	 * @return 
	 * @throws IOException 
	 */
	private String compileWithClosure(List<FileEntity> files) throws Exception {
		Compiler.setLoggingLevel(Level.OFF);

	    CompilerOptions options = createClosureCompilerOptions();
	    Compiler compiler = createClosureCompiler(options);
	    
	    List<JSSourceFile> externs = Lists.newLinkedList();
	    List<JSSourceFile> sources = Lists.newLinkedList();
	    
	    for (FileEntity f : files) {
	    	sources.add(JSSourceFile.fromFile(f.getFile(), Charset.forName(encoding)));
		}
	    
	    Result result = compiler.compile(externs, sources, options);
		if (result.success) {
			return compiler.toSource();
		} else {
			throw new BuildException("Compilation failed.");
		}
	}
	
	private String compileWithYUI(List<FileEntity> files) throws Exception {
		//collect all file streams into single reader
		ArrayList<InputStream> streams = new ArrayList<InputStream>();
		for (FileEntity f : files) {
			streams.add(f.getInputStream());
		}
		
		Enumeration<InputStream> streamsEnum = Collections.enumeration(streams);
		InputStreamReader reader = new InputStreamReader(new SequenceInputStream(streamsEnum), encoding);
		
		JavaScriptCompressor compressor = new JavaScriptCompressor(reader, new ErrorReporter() {
			
			public void warning(String message, String sourceName,
					int line, String lineSource, int lineOffset) {
				if (line < 0) {
					log(message, Project.MSG_WARN);
				} else {
					log(line + ':' + lineOffset + ':' + message, Project.MSG_WARN);
				}
			}
			
			public void error(String message, String sourceName,
					int line, String lineSource, int lineOffset) {
				if (line < 0) {
					log(message, Project.MSG_ERR);
				} else {
					log(line + ':' + lineOffset + ':' + message, Project.MSG_ERR);
				}
			}
			
			public EvaluatorException runtimeError(String message, String sourceName,
					int line, String lineSource, int lineOffset) {
				error(message, sourceName, line, lineSource, lineOffset);
				return new EvaluatorException(message);
			}
		});
		
		// Close the input stream first, and then open the output stream,
		// in case the output file should override the input file.
		reader.close(); reader = null;
		
		boolean munge = true;
		boolean preserveAllSemiColons = false;
		boolean disableOptimizations = false;
		boolean verbose = false;
		int linebreakpos = -1;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamWriter out = new OutputStreamWriter(baos, encoding);
		
		compressor.compress(out, linebreakpos, munge, verbose, preserveAllSemiColons, disableOptimizations);
		out.close();
		
		return baos.toString(encoding);
	}
	
	private CompilerOptions createClosureCompilerOptions() {
		CompilerOptions options = new CompilerOptions();

		CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
		WarningLevel.DEFAULT.setOptionsForWarningLevel(options);
		options.prettyPrint = false;
		options.printInputDelimiter = false;
		options.generateExports = false;

		options.setManageClosureDependencies(false);
		return options;
	}
	
	private Compiler createClosureCompiler(CompilerOptions options) {
		Compiler compiler = new Compiler();
		MessageFormatter formatter = options.errorFormat.toFormatter(compiler,
				false);
		AntErrorManager errorManager = new AntErrorManager(formatter, this);
		compiler.setErrorManager(errorManager);
		return compiler;
	}
}
