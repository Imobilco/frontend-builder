package ru.imobilco.builder.logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.Project;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ru.imobilco.builder.ant.FileEntity;

public class BundleLogger {
	public static String CATALOG_FILE = ".build-catalog";
	
	private volatile static BundleLogger singleton;
	private static Project project;
	private List<BundleItem> catalog;
	
	private BundleLogger(Project project) {
		BundleLogger.project = project;
		loadCatalog();
	}
	
	private void loadCatalog() {
		// load library catalog
		File catalogFile = getCatalogFile();
		if (catalogFile.exists()) {
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(catalogFile);
				doc.getDocumentElement().normalize();
				
				catalog = new ArrayList<BundleItem>();
				
				NodeList child = doc.getDocumentElement().getChildNodes();
				for (int i = 0; i < child.getLength(); i++) {
					Node n = child.item(i);
					if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equalsIgnoreCase("file")) {
						Element parentFile = (Element) n;
						List<ModuleFile> childFiles = new ArrayList<ModuleFile>();
						
						NodeList fileNodes = parentFile.getElementsByTagName("file");
						for (int j = 0; j < fileNodes.getLength(); j++) {
//							childFiles.add(getAttributeValue(fileNodes.item(j), "src"));
							childFiles.add(new ModuleFile(getAttributeValue(fileNodes.item(j), "src"), getAttributeValue(fileNodes.item(j), "md5")));
						}
						
						catalog.add(new BundleItem(getAttributeValue(n, "src"), childFiles));
					}
				}
			} catch (Exception e) {
				project.log("Cannot parse catalog: " + e.getMessage());
			}
		}
	}
	
	private String getAttributeValue(Node node, String attrName) {
		return node.getAttributes().getNamedItem(attrName).getNodeValue();
	}

	private File getCatalogFile() {
		return new File(project.getBaseDir(), CATALOG_FILE);
	}

	public static BundleLogger getSingleton(Project project) {
		if (singleton == null) {
			synchronized (BundleLogger.class) {
				if (singleton == null)
					singleton = new BundleLogger(project);
			}
		}
		
		return singleton;
	}
	
	public boolean isModified(File file, List<FileEntity> childFiles) {
		return isModified(file.getAbsolutePath(), childFiles);
	}
	
	public boolean isModified(String fileName, List<FileEntity> childFiles) {
		if (catalog == null) {
			return true;
		}
		
		// find record in catalog
		BundleItem record = findCatalogRecord(fileName);
		if (record != null) {
			try {
				return record.isModified(childFiles);
			} catch (Exception e) {}
		}
					
		return true;
	}
	
	public BundleItem findCatalogRecord(String fileName) {
		for (BundleItem item : catalog) {
			if (item.isSameFile(fileName)) {
				return item;
			}
		}
		
		return null;
	}
	
	public void addToCatalog(File file, List<FileEntity> childFiles) {
		addToCatalog(file.getAbsolutePath(), childFiles);
	}
	
	public void addToCatalog(String fileName, List<FileEntity> childFiles) {
		if (catalog == null) {
			catalog = new ArrayList<BundleItem>();
		}
		
		BundleItem record = findCatalogRecord(fileName);
		if (record != null) {
			record.setChildFiles(childFiles);
		} else {
			catalog.add(new BundleItem(fileName, childFiles));
		}
	}
	
	public void saveCatalog() {
		saveCatalog(getCatalogFile());
	}
	
	public void saveCatalog(File outputFile) {
		saveCatalog(new DefaultPathProcessor(), outputFile);
	}
	
	public void saveCatalog(IPathProcessor pathProcessor) {
		saveCatalog(pathProcessor, getCatalogFile());
	}
	
	public void saveCatalog(IPathProcessor pathProcessor, File outputFile) {
		OutputStreamWriter out;
		try {
			outputFile.createNewFile();
			out = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8");
			
			out.append(toXml(pathProcessor));
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String toXml() {
		return toXml(null);
	}

	public String toXml(IPathProcessor pathProcessor) {
		StringBuilder sb = new StringBuilder();
		sb.append("<files>\n");
		for (BundleItem item : catalog) {
			sb.append(item.toXml(pathProcessor) + "\n");
		}
		sb.append("</files>\n");
		return sb.toString();
	}
}