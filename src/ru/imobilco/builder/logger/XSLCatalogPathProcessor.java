package ru.imobilco.builder.logger;

import java.io.File;

public class XSLCatalogPathProcessor implements IPathProcessor {
	public static final String EMPTY = "";
	
	private File container;
	private boolean ensureAbsolute;
	
	public XSLCatalogPathProcessor(File container, boolean ensureAbsolute) {
		this.container = container;
		this.ensureAbsolute = ensureAbsolute;
	}

	@Override
	public String getPath(String path) {
		File f = new File(path);
		String filePath = makeFilePathRelative(container, f);
		
		if (ensureAbsolute) {
			if (filePath.startsWith("./")) {
				filePath = filePath.substring(1);
			} else if (!filePath.startsWith("..") && !filePath.startsWith("/")) {
				filePath = "/" + filePath;
			}
		}
		
		return filePath;
	}
	
	/**
	 * Returns a relative path for the second file compared to the first
	 * 
	 * @param fileA
	 *            The "reference" file path
	 * @param fileB
	 *            The file to make relative
	 * @return String
	 */
	public static String makeFilePathRelative(File fileA, File fileB) {
		String separator = System.getProperty("file.separator"); //$NON-NLS-1$

		String a = fileA.toString();
		if (!fileA.isDirectory()) {
			a = fileA.getParent().toString() + separator;
		}

		String b = fileB.toString();
		if (fileB.isDirectory()) {
			b = b + separator;
		}

		String r = replace(b, a, EMPTY);
		if (r.endsWith(separator)) {
			r = r.substring(0, r.length() - 1);
		}

		return r;
	}
	
	/**
	 * Replace one string with another
	 * 
	 * @param str
	 * @param pattern
	 * @param replace
	 * @return String
	 */
	public static String replace(String str, String pattern, String replace) {

		int s = 0;
		int e = 0;
		StringBuffer result = new StringBuffer();

		while ((e = str.indexOf(pattern, s)) >= 0) {
			result.append(str.substring(s, e));
			result.append(replace);
			s = e + pattern.length();
		}
		result.append(str.substring(s));
		return result.toString();

	}
}
