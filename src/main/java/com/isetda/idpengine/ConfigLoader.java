package com.isetda.idpengine;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigLoader {
    private static ConfigLoader instance;

    private List<String> bucketNames = new ArrayList<>();
    private String keyFilePath;
    private String projectId;
    private boolean deletedCheck;
    private String cloudPlatform;
    private String ocrUrl;
    private String excelFilePath;
    private String imageFolderPath;
    private String resultFilePath;

    private String configFilePath = "C:\\Users\\suaah\\IdeaProjects\\PJ20240901\\Config.xml";

    private ConfigLoader() {
        loadConfig();
    }

    public static ConfigLoader getInstance() {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }

    private void loadConfig() {
        try {
            File configFile = new File(configFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);

            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            projectId = root.getElementsByTagName("projectId").item(0).getTextContent().trim();
            bucketNames = Collections.singletonList(root.getElementsByTagName("bucketNames").item(0).getTextContent().trim());
            keyFilePath = root.getElementsByTagName("keyFilePath").item(0).getTextContent().trim();
            deletedCheck = Boolean.parseBoolean(root.getElementsByTagName("deletedCheck").item(0).getTextContent().trim());
            cloudPlatform = root.getElementsByTagName("cloudPlatform").item(0).getTextContent().trim();
            ocrUrl = root.getElementsByTagName("ocrUrl").item(0).getTextContent().trim();
            excelFilePath = root.getElementsByTagName("excelFilePath").item(0).getTextContent().trim();
            imageFolderPath = root.getElementsByTagName("imageFolderPath").item(0).getTextContent().trim();
            resultFilePath = root.getElementsByTagName("resultFilePath").item(0).getTextContent().trim();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load configuration from " + configFilePath, e);
        }
    }

    public List<String> getBucketNames() {
        return bucketNames;
    }

    public String getKeyFilePath() {
        return keyFilePath;
    }

    public String getProjectId() {
        return projectId;
    }

    public boolean isDeletedCheck() {
        return deletedCheck;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public String getOcrUrl() {
        return ocrUrl;
    }

    public String getExcelFilePath() {
        return excelFilePath;
    }

    public String getImageFolderPath() {
        return imageFolderPath;
    }

    public String getResultFilePath() {
        return resultFilePath;
    }
}