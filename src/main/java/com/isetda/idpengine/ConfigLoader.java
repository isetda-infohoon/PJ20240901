package com.isetda.idpengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static final Logger log = LogManager.getLogger(ConfigLoader.class);
    public static ConfigLoader instance;

    public List<String> bucketNames = new ArrayList<>();
    public String keyFilePath;
    public String projectId;
    public boolean deletedCheck;
    public String cloudPlatform;
    public String ocrUrl;
    public String excelFilePath;
    public String jsonFilePath;
    public String imageFolderPath;
    public String resultFilePath;
    private String jdbcUrl;
    private String username;
    private String password;
    private boolean dbDataUsageFlag;
    public boolean writeDetailResult;
    public boolean writeExcelResults;
    public boolean writeTextResults;
    public boolean encodingCheck;

    public boolean weightCountFlag;

    public boolean markingCheck;

    public boolean cdAUsageFlag;
    public boolean cdBUsageFlag;
    public boolean cdCUsageFlag;

    public boolean cd1UsageFlag;
    public boolean cd2UsageFlag;
    public boolean cd3UsageFlag;

    public int wordMinimumCount;
    public double cdAllowableWeight;
    public boolean checkCase;
    public boolean createFolders;
    public String classificationCriteria;
    public String subClassificationCriteria;

    public int plValue;

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

            // 각 태그가 있는지 확인하고 없으면 예외 발생
            if (root.getElementsByTagName("projectId").getLength() > 0) {
                projectId = root.getElementsByTagName("projectId").item(0).getTextContent().trim();
            } else {
                log.error("The projectId tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: projectId");
            }

            if (root.getElementsByTagName("bucketNames").getLength() > 0) {
                bucketNames = Collections.singletonList(root.getElementsByTagName("bucketNames").item(0).getTextContent().trim());
            } else {
                log.error("The bucketNames tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: bucketNames");
            }

            if (root.getElementsByTagName("keyFilePath").getLength() > 0) {
                keyFilePath = root.getElementsByTagName("keyFilePath").item(0).getTextContent().trim();
            } else {
                log.error("The keyFilePath tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: keyFilePath");
            }

            if (root.getElementsByTagName("deletedCheck").getLength() > 0) {
                deletedCheck = Boolean.parseBoolean(root.getElementsByTagName("deletedCheck").item(0).getTextContent().trim());
            } else {
                log.error("The deletedCheck tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: deletedCheck");
            }

            if (root.getElementsByTagName("cloudPlatform").getLength() > 0) {
                cloudPlatform = root.getElementsByTagName("cloudPlatform").item(0).getTextContent().trim();
            } else {
                log.error("The cloudPlatform tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: cloudPlatform");
            }

            if (root.getElementsByTagName("ocrUrl").getLength() > 0) {
                ocrUrl = root.getElementsByTagName("ocrUrl").item(0).getTextContent().trim();
            } else {
                log.error("The ocrUrl tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: ocrUrl");
            }

            if (root.getElementsByTagName("jsonFilePath").getLength() > 0) {
                jsonFilePath = root.getElementsByTagName("jsonFilePath").item(0).getTextContent().trim();
            } else {
                log.error("The jsonFilePath tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: jsonFilePath");
            }

            if (root.getElementsByTagName("imageFolderPath").getLength() > 0) {
                imageFolderPath = root.getElementsByTagName("imageFolderPath").item(0).getTextContent().trim();
            } else {
                log.error("The imageFolderPath tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: imageFolderPath");
            }

            if (root.getElementsByTagName("resultFilePath").getLength() > 0) {
                resultFilePath = root.getElementsByTagName("resultFilePath").item(0).getTextContent().trim();
            } else {
                log.error("The resultFilePath tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: resultFilePath");
            }

            if (root.getElementsByTagName("jdbcUrl").getLength() > 0) {
                jdbcUrl = root.getElementsByTagName("jdbcUrl").item(0).getTextContent().trim();
            } else {
                log.error("The jdbcUrl tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: jdbcUrl");
            }

            if (root.getElementsByTagName("username").getLength() > 0) {
                username = root.getElementsByTagName("username").item(0).getTextContent().trim();
            } else {
                log.error("The username tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: username");
            }

            if (root.getElementsByTagName("password").getLength() > 0) {
                password = root.getElementsByTagName("password").item(0).getTextContent().trim();
            } else {
                log.error("The password tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: password");
            }

            if (root.getElementsByTagName("dbDataUsageFlag").getLength() > 0) {
                dbDataUsageFlag = Boolean.parseBoolean(root.getElementsByTagName("dbDataUsageFlag").item(0).getTextContent().trim());
            } else {
                log.error("The dbDataUsageFlag tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: dbDataUsageFlag");
            }

            if (root.getElementsByTagName("writeDetailResult").getLength() > 0) {
                writeDetailResult = Boolean.parseBoolean(root.getElementsByTagName("writeDetailResult").item(0).getTextContent().trim());
            } else {
                log.error("The writeDetailResult tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: writeDetailResult");
            }

            if (root.getElementsByTagName("writeExcelResults").getLength() > 0) {
                writeExcelResults = Boolean.parseBoolean(root.getElementsByTagName("writeExcelResults").item(0).getTextContent().trim());
            } else {
                log.error("The writeExcelResults tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: writeExcelResults");
            }

            if (root.getElementsByTagName("writeTextResults").getLength() > 0) {
                writeTextResults = Boolean.parseBoolean(root.getElementsByTagName("writeTextResults").item(0).getTextContent().trim());
            } else {
                log.error("The writeTextResults tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: writeTextResults");
            }

            if (root.getElementsByTagName("encodingCheck").getLength() > 0) {
                encodingCheck = Boolean.parseBoolean(root.getElementsByTagName("encodingCheck").item(0).getTextContent().trim());
            } else {
                log.error("The encodingCheck tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: encodingCheck");
            }

            // weightCountFlag 태그가 있는지 확인하고 없으면 예외 발생
            if (root.getElementsByTagName("weightCountFlag").getLength() > 0) {
                weightCountFlag = Boolean.parseBoolean(root.getElementsByTagName("weightCountFlag").item(0).getTextContent().trim());
            } else {
                log.error("The weightCountFlag tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: weightCountFlag");
            }
            // weightCountFlag 태그가 있는지 확인하고 없으면 예외 발생
            if (root.getElementsByTagName("markingCheck").getLength() > 0) {
                markingCheck = Boolean.parseBoolean(root.getElementsByTagName("markingCheck").item(0).getTextContent().trim());
            } else {
                log.error("The markingCheck tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: markingCheck");
            }

            if (root.getElementsByTagName("cdAUsageFlag").getLength() > 0) {
                cdAUsageFlag = Boolean.parseBoolean(root.getElementsByTagName("cdAUsageFlag").item(0).getTextContent().trim());
            } else {
                log.error("The cdAUsageFlag tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: cdAUsageFlag");
            }

            if (root.getElementsByTagName("cdBUsageFlag").getLength() > 0) {
                cdBUsageFlag = Boolean.parseBoolean(root.getElementsByTagName("cdBUsageFlag").item(0).getTextContent().trim());
            } else {
                log.error("The cdBUsageFlag tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: cdBUsageFlag");
            }

            if (root.getElementsByTagName("cdCUsageFlag").getLength() > 0) {
                cdCUsageFlag = Boolean.parseBoolean(root.getElementsByTagName("cdCUsageFlag").item(0).getTextContent().trim());
            } else {
                log.error("The cdCUsageFlag tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: cdCUsageFlag");
            }

            if (root.getElementsByTagName("cd1UsageFlag").getLength() > 0) {
                cd1UsageFlag = Boolean.parseBoolean(root.getElementsByTagName("cd1UsageFlag").item(0).getTextContent().trim());
            } else {
                log.error("The cd1UsageFlag tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: cd1UsageFlag");
            }

            if (root.getElementsByTagName("cd2UsageFlag").getLength() > 0) {
                cd2UsageFlag = Boolean.parseBoolean(root.getElementsByTagName("cd2UsageFlag").item(0).getTextContent().trim());
            } else {
                log.error("The cd2UsageFlag tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: cd2UsageFlag");
            }

            if (root.getElementsByTagName("cd3UsageFlag").getLength() > 0) {
                cd3UsageFlag = Boolean.parseBoolean(root.getElementsByTagName("cd3UsageFlag").item(0).getTextContent().trim());
            } else {
                log.error("The cd3UsageFlag tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: cd3UsageFlag");
            }

            if (root.getElementsByTagName("wordMinimumCount").getLength() > 0) {
                wordMinimumCount = Integer.parseInt(root.getElementsByTagName("wordMinimumCount").item(0).getTextContent().trim());
            } else {
                log.error("The wordMinimumCount tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: wordMinimumCount");
            }

            if (root.getElementsByTagName("cdAllowableWeight").getLength() > 0) {
                cdAllowableWeight = Double.parseDouble(root.getElementsByTagName("cdAllowableWeight").item(0).getTextContent().trim());
            } else {
                log.error("The cdAllowableWeight tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: cdAllowableWeight");
            }

            if (root.getElementsByTagName("checkCase").getLength() > 0) {
                checkCase = Boolean.parseBoolean(root.getElementsByTagName("checkCase").item(0).getTextContent().trim());
            } else {
                log.error("The checkCase tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: checkCase");
            }

            if (root.getElementsByTagName("createFolders").getLength() > 0) {
                createFolders = Boolean.parseBoolean(root.getElementsByTagName("createFolders").item(0).getTextContent().trim());
            } else {
                log.error("The createFolders tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: createFolders");
            }

            if (root.getElementsByTagName("classificationCriteria").getLength() > 0) {
                classificationCriteria = root.getElementsByTagName("classificationCriteria").item(0).getTextContent().trim();
            } else {
                log.error("The classificationCriteria tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: classificationCriteria");
            }

            if (root.getElementsByTagName("subClassificationCriteria").getLength() > 0) {
                subClassificationCriteria = root.getElementsByTagName("subClassificationCriteria").item(0).getTextContent().trim();
            } else {
                log.error("The subClassificationCriteria tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: subClassificationCriteria");
            }

            if (root.getElementsByTagName("plValue").getLength() > 0) {
                plValue = Integer.parseInt(root.getElementsByTagName("plValue").item(0).getTextContent().trim());
            } else {
                log.error("The plValue tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: plValue");
            }

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