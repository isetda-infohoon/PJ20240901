package com.isetda.idpengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBService {
    private static final Logger log = LogManager.getLogger(IDPEngineController.class);
    public List<Country> countryList;
    public List<Document> documentList;
    public List<Word> wordList;

    public Connection getConnection() {
        ConfigLoader configLoader = ConfigLoader.getInstance();

        String jdbcUrl = configLoader.getJdbcUrl();
        String username = configLoader.getUsername();
        String password = configLoader.getPassword();

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(jdbcUrl, username, password);
            log.info("DB Connect");
        } catch (SQLException e) {
            log.error("DB Connect 실패: {}", e.getStackTrace()[0]);
        }

        return connection;
    }

    public List<Country> getCountryData() {
        countryList = new ArrayList<>();

        try {
            Connection connection = this.getConnection();
            String sql = "SELECT COUNTRY_ID, COUNTRY_CODE, COUNTRY_NAME FROM COUNTRY ORDER BY COUNTRY_ID ASC";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int countryId = resultSet.getInt("COUNTRY_ID");
                String countryCode = resultSet.getString("COUNTRY_CODE");
                String countryName = resultSet.getString("COUNTRY_NAME");

                Country country = new Country(countryId, countryCode, countryName);
                countryList.add(country);
            }

            resultSet.close();
            preparedStatement.close();
            connection.close();

            log.info("COUNTRY 테이블 저장 완료");
        } catch (Exception e) {
            log.error("COUNTRY 테이블 저장 실패: {}", e.getStackTrace()[0]);
        }

//        for (Country vo : countryList) {
//            log.info("{}, {}, {}", vo.getCountryId(), vo.getCountryName(), vo.getCountryCode());
//        }

        return countryList;
    }

    public List<Document> getDocumentData() {
        documentList = new ArrayList<>();

        try {
            Connection connection = this.getConnection();
            String sql = "SELECT D.*, C.COUNTRY_CODE FROM DOCUMENT D JOIN COUNTRY C ON D.COUNTRY_ID = C.COUNTRY_ID WHERE C.COUNTRY_ID = ?";

            for (Country country : countryList) {
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, country.getCountryId());
                ResultSet resultSet = preparedStatement.executeQuery();

                while (resultSet.next()) {
                    int documentId = resultSet.getInt("DOCUMENT_ID");
                    int countryId = resultSet.getInt("COUNTRY_ID");
                    String documentType = resultSet.getString("DOCUMENT_TYPE");
                    String countryCode = resultSet.getString("COUNTRY_CODE");

                    Document document = new Document(documentId, countryId, documentType, countryCode);
                    documentList.add(document);
                }

                resultSet.close();
                preparedStatement.close();
            }

            connection.close();

            log.info("DOCUMENT 테이블 저장 완료");
        } catch (Exception e) {
            log.error("DOCUMENT 테이블 저장 실패: {}", e.getStackTrace()[0]);
        }

//        for (Document vo : documentList) {
//            System.out.println(vo.getDocumentId() + ", " + vo.getDocumentType() + ", " + vo.getCountryCode());
//        }

        return documentList;
    }

    public List<Word> getWordData() {
        getCountryData();

        wordList = new ArrayList<>();

        try {
            Connection connection = this.getConnection();
            String sql = "SELECT W.*, D.DOCUMENT_TYPE, C.COUNTRY_CODE FROM WORD W JOIN DOCUMENT D ON W.DOCUMENT_ID = D.DOCUMENT_ID JOIN COUNTRY C ON D.COUNTRY_ID = C.COUNTRY_ID WHERE C.COUNTRY_ID = ? ORDER BY D.DOCUMENT_ID ASC";

            for (Country country : countryList) {
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, country.getCountryId());
                ResultSet resultSet = preparedStatement.executeQuery();

                while (resultSet.next()) {
                    int wordId = resultSet.getInt("WORD_ID");
                    int documentId = resultSet.getInt("DOCUMENT_ID");
                    String word1 = resultSet.getString("WORD");
                    double wordWeight = resultSet.getDouble("WORD_WEIGHT");
                    String documentType = resultSet.getString("DOCUMENT_TYPE");
                    String countryCode = resultSet.getString("COUNTRY_CODE");

                    Word word = new Word(wordId, documentId, word1, wordWeight, documentType, countryCode);
                    wordList.add(word);
                }

                resultSet.close();
                preparedStatement.close();
            }

            connection.close();

            log.info("WORD 테이블 저장 완료");
        } catch (Exception e) {
            log.error("WORD 테이블 저장 실패: {}", e.getStackTrace()[0]);
        }

//        for (Word vo : wordList) {
//            log.info("{}, {}, {}, {}", vo.getCountryCode(), vo.getDocumentType(), vo.getWord(), vo.getWordWeight());
//        }

        return wordList;
    }


}
