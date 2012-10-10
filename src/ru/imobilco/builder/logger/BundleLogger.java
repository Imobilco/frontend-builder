package ru.imobilco.builder.logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.Project;
import org.json.simple.JSONObject;
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
    private List<String> updatedFiles;
    private File baseDir;

    private BundleLogger(Project project) {
        BundleLogger.project = project;
        baseDir = project.getBaseDir();
        updatedFiles = new ArrayList<String>();
        loadCatalog();
    }

    private String getFilePath(Node node) {
        File _f = new File(baseDir, getAttributeValue(node, "src"));
        return _f.getAbsolutePath();
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
                            childFiles.add(new ModuleFile(getFilePath(fileNodes.item(j)), getAttributeValue(fileNodes.item(j), "md5")));
                        }

                        catalog.add(new BundleItem(getFilePath(n), childFiles));
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
            } catch (Exception e) {
            }
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

        updatedFiles.add(fileName);
    }

    public void saveCatalog() {
        saveCatalogXml(getCatalogFile(), true);
    }

    public void saveCatalogXml(File outputFile, boolean printChildren) {
        saveCatalogXml(new DefaultPathProcessor(baseDir), outputFile, printChildren);
    }

    public void saveCatalogXml(IPathProcessor pathProcessor, File outputFile, boolean printChildren) {
        try {
            saveToFile(outputFile, toXml(pathProcessor, printChildren));
        } catch (Exception e) {
            project.log("Error writing catalog file", e, 0);
        }
    }

    public void saveCatalogJson(IPathProcessor pathProcessor, File outputFile, boolean printChildren) {
        try {
            saveToFile(outputFile, toJson(pathProcessor, printChildren));
        } catch (Exception e) {
            project.log("Error writing catalog file", e, 0);
        }
    }

    private static void saveToFile(File outputFile, String content) throws Exception {
        OutputStreamWriter out = null;
        try {
            File parentDir = outputFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            outputFile.createNewFile();
            out = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8");
            out.append(content);
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    public String toXml() {
        return toXml(null, true);
    }

    public String toXml(IPathProcessor pathProcessor, boolean printChildren) {
        StringBuilder sb = new StringBuilder();
        sb.append("<files>\n");
        for (BundleItem item : catalog) {
            sb.append(item.toXml(pathProcessor, printChildren) + "\n");
        }
        sb.append("</files>\n");
        return sb.toString();
    }

    public String toJson(IPathProcessor pathProcessor, boolean printChildren) {
        JSONObject object = new JSONObject();
        for (BundleItem item : catalog) {
            JSONObject innerObject = item.toJSON(pathProcessor, printChildren);
            object.put(innerObject.get("src"), innerObject);
        }
        return object.toJSONString();
    }

    public List<String> getUpdatedFiles() {
        return updatedFiles;
    }

    public File getBaseDir() {
        return baseDir;
    }
}