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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    public String synapApiKey;
    public String synapOcrUrl;
    public String docuAnalyzerApiKey;
    public String docuAnalyzerUrl;
    public String excelFilePath;
    public String ruleFilePath;
    public String ruleFolderPath;
    List<FolderMapping> folderMappings = new ArrayList<>();
    public List<String> imageFolderPaths = new ArrayList<>();
    public String imageFolderPath;
    public String resultFilePath;
    private String jdbcUrl;
    private String username;
    private String password;
    private boolean dbDataUsageFlag;

    public String drivePath;
    public String driveUNCPath;
    public String driveUsername;
    public String drivePassword;

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

    public boolean apiUsageFlag;
    public int apiCycle;
    public String apiURL;
    public String apiUserId;
    public String serviceType;
    public boolean classifyByFirstPage;
    public boolean useUnclassifiedAsCS;
    public boolean createClassifiedFolder;
    public boolean useUrlEncoding;
    public boolean useCallbackUpdate;
    public boolean useMdFileCreation;
    public boolean useSourceDeletion;
    public String resultFileNamingRule;

    public boolean excelFileDownload;
    public boolean csvFileDownload;
    public boolean htmlFileDownload;

    public boolean usePdfExtractImage;

    private String configFilePath = "Config.xml";
    private static final String backupFilePath = "Config_backup.xml";
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
            if (!configFile.exists() || configFile.length() == 0) {
                log.warn("Config.xml is missing or empty. Creating default configuration.");
                restoreFromBackupOrDefault();
            } else {
                Files.copy(configFile.toPath(), new File(backupFilePath).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
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

            if (root.getElementsByTagName("apiUsageFlag").getLength() > 0) {
                apiUsageFlag = Boolean.parseBoolean(root.getElementsByTagName("apiUsageFlag").item(0).getTextContent().trim());
            } else {
                log.error("The apiUsageFlag tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: apiUsageFlag");
            }

            if (root.getElementsByTagName("synapApiKey").getLength() > 0) {
                synapApiKey = root.getElementsByTagName("synapApiKey").item(0).getTextContent().trim();
            } else {
                log.error("The synapApiKey tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: synapApiKey");
            }

            if (root.getElementsByTagName("synapOcrUrl").getLength() > 0) {
                synapOcrUrl = root.getElementsByTagName("synapOcrUrl").item(0).getTextContent().trim();
            } else {
                log.error("The synapOcrUrl tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: synapOcrUrl");
            }

            if (root.getElementsByTagName("docuAnalyzerApiKey").getLength() > 0) {
                docuAnalyzerApiKey = root.getElementsByTagName("docuAnalyzerApiKey").item(0).getTextContent().trim();
            } else {
                log.error("The docuAnalyzerApiKey tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: docuAnalyzerApiKey");
            }

            if (root.getElementsByTagName("docuAnalyzerUrl").getLength() > 0) {
                docuAnalyzerUrl = root.getElementsByTagName("docuAnalyzerUrl").item(0).getTextContent().trim();
            } else {
                log.error("The docuAnalyzerUrl tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: docuAnalyzerUrl");
            }

            if (root.getElementsByTagName("ruleFilePath").getLength() > 0) {
                ruleFilePath = root.getElementsByTagName("ruleFilePath").item(0).getTextContent().trim();
            } else {
                log.error("The ruleFilePath tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: ruleFilePath");
            }

            if (root.getElementsByTagName("ruleFolderPath").getLength() > 0) {
                ruleFolderPath = root.getElementsByTagName("ruleFolderPath").item(0).getTextContent().trim();
            } else {
                log.error("The ruleFolderPath tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: ruleFolderPath");
            }

            if (apiUsageFlag) {
                if (root.getElementsByTagName("path").getLength() > 0) {
                    for (int i = 0; i < root.getElementsByTagName("path").getLength(); i++) {
                        Element mappingElement = (Element) root.getElementsByTagName("path").item(i);

                        String imagePath = mappingElement.getElementsByTagName("imageFolderPath").item(0).getTextContent().trim();
                        String resultPath = mappingElement.getElementsByTagName("resultFilePath").item(0).getTextContent().trim();

                        folderMappings.add(new FolderMapping(imagePath, resultPath));
                    }
                } else {
                    log.error("The imageFolderPath tag does not exist in Config.xml. Application will terminate.");
                    throw new RuntimeException("Missing required configuration: imageFolderPath");
                }
            } else {
                if (root.getElementsByTagName("path").getLength() > 0) {
                    Element mappingElement = (Element) root.getElementsByTagName("path").item(0);

                    imageFolderPath = mappingElement.getElementsByTagName("imageFolderPath").item(0).getTextContent().trim();
                    resultFilePath = mappingElement.getElementsByTagName("resultFilePath").item(0).getTextContent().trim();
                } else {
                    log.error("The imageFolderPath tag does not exist in Config.xml. Application will terminate.");
                    throw new RuntimeException("Missing required configuration: imageFolderPath");
                }
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

            if (root.getElementsByTagName("drivePath").getLength() > 0) {
                drivePath = root.getElementsByTagName("drivePath").item(0).getTextContent().trim();
            } else {
                log.error("The drivePath tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: drivePath");
            }

            if (root.getElementsByTagName("driveUsername").getLength() > 0) {
                driveUsername = root.getElementsByTagName("driveUsername").item(0).getTextContent().trim();
            } else {
                log.error("The driveUsername tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: driveUsername");
            }

            if (root.getElementsByTagName("driveUNCPath").getLength() > 0) {
                driveUNCPath = root.getElementsByTagName("driveUNCPath").item(0).getTextContent().trim();
            } else {
                log.error("The driveUNCPath tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: driveUNCPath");
            }

            if (root.getElementsByTagName("drivePassword").getLength() > 0) {
                drivePassword = root.getElementsByTagName("drivePassword").item(0).getTextContent().trim();
            } else {
                log.error("The drivePassword tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: drivePassword");
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

            if (root.getElementsByTagName("apiCycle").getLength() > 0) {
                apiCycle = Integer.parseInt(root.getElementsByTagName("apiCycle").item(0).getTextContent().trim());
            } else {
                log.error("The apiCycle tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: apiCycle");
            }

            if (root.getElementsByTagName("apiURL").getLength() > 0) {
                apiURL = root.getElementsByTagName("apiURL").item(0).getTextContent().trim();
            } else {
                log.error("The apiURL tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: apiURL");
            }

            if (root.getElementsByTagName("apiUserId").getLength() > 0) {
                apiUserId = root.getElementsByTagName("apiUserId").item(0).getTextContent().trim();
            } else {
                log.error("The apiUserId tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: apiUserId");
            }

            if (root.getElementsByTagName("serviceType").getLength() > 0) {
                serviceType = root.getElementsByTagName("serviceType").item(0).getTextContent().trim();
            } else {
                log.error("The serviceType tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: serviceType");
            }

            if (root.getElementsByTagName("classifyByFirstPage").getLength() > 0) {
                classifyByFirstPage = Boolean.parseBoolean(root.getElementsByTagName("classifyByFirstPage").item(0).getTextContent().trim());
            } else {
                log.error("The classifyByFirstPage tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: classifyByFirstPage");
            }

            if (root.getElementsByTagName("excelFileDownload").getLength() > 0) {
                excelFileDownload = Boolean.parseBoolean(root.getElementsByTagName("excelFileDownload").item(0).getTextContent().trim());
            } else {
                log.error("The excelFileDownload tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: excelFileDownload");
            }

            if (root.getElementsByTagName("csvFileDownload").getLength() > 0) {
                csvFileDownload = Boolean.parseBoolean(root.getElementsByTagName("csvFileDownload").item(0).getTextContent().trim());
            } else {
                log.error("The csvFileDownload tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: csvFileDownload");
            }

            if (root.getElementsByTagName("htmlFileDownload").getLength() > 0) {
                htmlFileDownload = Boolean.parseBoolean(root.getElementsByTagName("htmlFileDownload").item(0).getTextContent().trim());
            } else {
                log.error("The htmlFileDownload tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: htmlFileDownload");
            }

            if (root.getElementsByTagName("useUnclassifiedAsCS").getLength() > 0) {
                useUnclassifiedAsCS = Boolean.parseBoolean(root.getElementsByTagName("useUnclassifiedAsCS").item(0).getTextContent().trim());
            } else {
                log.error("The useUnclassifiedAsCS tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: useUnclassifiedAsCS");
            }

            if (root.getElementsByTagName("createClassifiedFolder").getLength() > 0) {
                createClassifiedFolder = Boolean.parseBoolean(root.getElementsByTagName("createClassifiedFolder").item(0).getTextContent().trim());
            } else {
                log.error("The createClassifiedFolder tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: createClassifiedFolder");
            }

            if (root.getElementsByTagName("useUrlEncoding").getLength() > 0) {
                useUrlEncoding = Boolean.parseBoolean(root.getElementsByTagName("useUrlEncoding").item(0).getTextContent().trim());
            } else {
                log.error("The useUrlEncoding tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: useUrlEncoding");
            }

            if (root.getElementsByTagName("useCallbackUpdate").getLength() > 0) {
                useCallbackUpdate = Boolean.parseBoolean(root.getElementsByTagName("useCallbackUpdate").item(0).getTextContent().trim());
            } else {
                log.error("The useCallbackUpdate tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: useCallbackUpdate");
            }

            if (root.getElementsByTagName("useMdFileCreation").getLength() > 0) {
                useMdFileCreation = Boolean.parseBoolean(root.getElementsByTagName("useMdFileCreation").item(0).getTextContent().trim());
            } else {
                log.error("The useMdFileCreation tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: useMdFileCreation");
            }

            if (root.getElementsByTagName("usePdfExtractImage").getLength() > 0) {
                usePdfExtractImage = Boolean.parseBoolean(root.getElementsByTagName("usePdfExtractImage").item(0).getTextContent().trim());
            } else {
                log.error("The usePdfExtractImage tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: usePdfExtractImage");
            }

            if (root.getElementsByTagName("useSourceDeletion").getLength() > 0) {
                useSourceDeletion = Boolean.parseBoolean(root.getElementsByTagName("useSourceDeletion").item(0).getTextContent().trim());
            } else {
                log.error("The useSourceDeletion tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: useSourceDeletion");
            }

            if (root.getElementsByTagName("resultFileNamingRule").getLength() > 0) {
                resultFileNamingRule = root.getElementsByTagName("resultFileNamingRule").item(0).getTextContent().trim();
            } else {
                log.error("The resultFileNamingRule tag does not exist in Config.xml. Application will terminate.");
                throw new RuntimeException("Missing required configuration: resultFileNamingRule");
            }

            Files.copy(configFile.toPath(), new File(backupFilePath).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.error("Failed to load configuration. Restoring from backup or default.", e);
            restoreFromBackupOrDefault();
//            e.printStackTrace();
//            throw new RuntimeException("Failed to load configuration from " + configFilePath, e);
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

    private void restoreFromBackupOrDefault() {
        try {
            File backupFile = new File(backupFilePath);
            File configFile = new File(configFilePath);

            if (backupFile.exists() && backupFile.length() > 0) {
                log.warn("Restoring Config.xml from backup.");
                Files.copy(backupFile.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                log.warn("No backup found. Creating default Config.xml.");
                String defaultXml = "<Config><projectId>default</projectId><bucketNames>defaultBucket</bucketNames></Config>";
                Files.writeString(configFile.toPath(), defaultXml, StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to restore Config.xml", ex);
        }
    }

    private void createDefaultConfig(File configFile) {
        try {
            String defaultXml = "<Config><projectId>default</projectId><bucketNames>defaultBucket</bucketNames></Config>";
            Files.writeString(configFile.toPath(), defaultXml, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create default Config.xml", ex);
        }
    }

    public void saveConfig() {
        try {
            File configFile = new File(configFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

            // 공백 보존 설정
            dbFactory.setIgnoringElementContentWhitespace(false);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc;

            if (configFile.exists() && configFile.length() > 0) {
                doc = dBuilder.parse(configFile);
            } else {
                // 새 Document 생성
                doc = dBuilder.newDocument();
                Element root = doc.createElement("Config");
                doc.appendChild(root);
            }

            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            // 기존 태그 값 수정
            if (!apiUsageFlag) {
                root.getElementsByTagName("imageFolderPath").item(0).setTextContent(imageFolderPath);
                root.getElementsByTagName("resultFilePath").item(0).setTextContent(resultFilePath);
            }

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