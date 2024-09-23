package com.isetda.idpengine;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigLoader {
    public static ConfigLoader instance;

    public List<String> bucketNames = new ArrayList<>();
    public String keyFilePath;
    public String projectId;
    public boolean deletedCheck;
    public String cloudPlatform;
    public String ocrUrl;
    public String excelFilePath;
    public String imageFolderPath;
    public String resultFilePath;

    private String configFilePath = "Config.xml";
    //jar파일 만들 때 상대경로 config 파일 빼놓기 위한 경로

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
    public String setImageFolderPath(String ImgPath) {
        imageFolderPath = ImgPath;
        return imageFolderPath;
    }

    public void saveConfig() {
        try {
            File configFile = new File(configFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

            // 공백 보존 설정
            dbFactory.setIgnoringElementContentWhitespace(false);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);

            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            // 기존 태그 값 수정
            root.getElementsByTagName("imageFolderPath").item(0).setTextContent(imageFolderPath);
            root.getElementsByTagName("resultFilePath").item(0).setTextContent(resultFilePath);

            // 변경된 내용을 XML 파일에 저장
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            // 불필요한 포맷 방지
            transformer.setOutputProperty(OutputKeys.INDENT, "no"); // Indentation을 비활성화
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(configFile);
            transformer.transform(source, result);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save configuration to " + configFilePath, e);
        }
    }


}