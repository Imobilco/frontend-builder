package ru.imobilco.builder.logger;

/**
 * Path processor interface that should transform all module's file path into
 * another path (for example, convert absolute paths to relative ones)
 * @author sergey
 *
 */
public interface IPathProcessor {
	/**
	 * Get new path for nodule's file
	 * @param path Absolute file path
	 * @return
	 */
	public String getPath(String path);
}
