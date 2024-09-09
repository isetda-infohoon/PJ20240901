package com.isetda.idpengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

//public class DBService {
//    private static final Logger log = LogManager.getLogger(IDPEngineController.class);
//    public List<Country> countryList;
//    public List<Document> documentList;
//
//    public Connection getConnection() {
//        ConfigLoader configLoader = ConfigLoader.getInstance();
//
//        String jdbcUrl = configLoader.getJdbcUrl();
//        String username = configLoader.getUsername();
//        String password = configLoader.getPassword();
//
//        Connection connection = null;
//        try {
//            connection = DriverManager.getConnection(jdbcUrl, username, password);
//            log.info("Connected to MSSQL database!");
//        } catch (SQLException e) {
//            log.error("database에 연결하지 못했습니다: {}", e.getStackTrace()[0]);
//        }
//
//        return connection;
//    }
//
//    public List<Country> getCountryData() {
//        countryList = new ArrayList<>();
//
//        try {
//            Connection connection = this.getConnection();
//            String sql = "SELECT COUNTRY_ID, COUNTRY_CODE, COUNTRY_NAME FROM COUNTRY";
//            PreparedStatement preparedStatement = connection.prepareStatement(sql);
//            ResultSet resultSet = preparedStatement.executeQuery();
//
//            while (resultSet.next()) {
//                int countryId = resultSet.getInt("COUNTRY_ID");
//                String countryCode = resultSet.getString("COUNTRY_CODE");
//                String countryName = resultSet.getString("COUNTRY_NAME");
//
//                Country country = new Country(countryId, countryCode, countryName);
//                countryList.add(country);
//            }
//
//            resultSet.close();
//            preparedStatement.close();
//            connection.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        for (Country vo : countryList) {
//            System.out.println(vo.getCountryId() + ", " + vo.getCountryName() + ", " + vo.getCountryCode());
//        }
//
//        return countryList;
//    }
//
//    public List<Document> getDocumentData() {
//        documentList = new ArrayList<>();
//
//        try {
//            Connection connection = this.getConnection();
//            String sql = "SELECT D.*, C.COUNTRY_CODE FROM DOCUMENT D JOIN COUNTRY C ON D.COUNTRY_ID = C.COUNTRY_ID WHERE C.COUNTRY_ID = ?";
//
//            for (Country country : countryList) {
//                PreparedStatement preparedStatement = connection.prepareStatement(sql);
//                preparedStatement.setInt(1, country.getCountryId());
//                ResultSet resultSet = preparedStatement.executeQuery();
//
//                while (resultSet.next()) {
//                    int documentId = resultSet.getInt("DOCUMENT_ID");
//                    int countryId = resultSet.getInt("COUNTRY_ID");
//                    String documentType = resultSet.getString("DOCUMENT_TYPE");
//                    String countryCode = resultSet.getString("COUNTRY_CODE");
//
//                    Document document = new Document(documentId, countryId, documentType, countryCode);
//                    documentList.add(document);
//                }
//
//                resultSet.close();
//                preparedStatement.close();
//            }
//
//            connection.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        for (Document vo : documentList) {
//            System.out.println(vo.getDocumentId() + ", " + vo.getDocumentType() + ", " + vo.getCountryCode());
//        }
//
//        return documentList;
//    }
//
//}
