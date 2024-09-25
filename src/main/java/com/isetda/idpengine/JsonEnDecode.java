package com.isetda.idpengine;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class JsonEnDecode {
    private static final Logger log = LogManager.getLogger(JsonEnDecode.class);
    public static String priKey = "iset2021!_1234567890a";

    public Label errorLabel;
    public TextField EncodingKey;

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY = "iset2021!1234567890abcdefghijkln";
    private static final String IV = "0987654321abcdef";

    private static SecretKeySpec getSecretKeySpec() {
        byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static IvParameterSpec getIvParameterSpec() {
        return new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8));
    }

    public String aesEncode(String plainText, String aesKey) throws Exception {
        if (!aesKey.contains("iset2021")) {
            log.info("Encoding key is invalid.");
            errorLabel.setText("key is wrong");
            return null;

        }

        SecretKeySpec keySpec = getSecretKeySpec();
        IvParameterSpec ivSpec = getIvParameterSpec();

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String aesDecode(String encryptedText, String aesKey) throws Exception {
        if (!aesKey.contains("iset2021")) {
            log.info("Decoding key is invalid");
            errorLabel.setText("key is wrong");
            return null;

        }

        SecretKeySpec keySpec = getSecretKeySpec();
        IvParameterSpec ivSpec = getIvParameterSpec();

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public String JsonEncoding2(String jsonFilePath) throws Exception {
        String outputFile = "encoding.json";

        // 파일 존재 여부 확인
        if (!Files.exists(Paths.get(jsonFilePath))) {
            log.info("json File does not exist.");
            errorLabel.setText("File does not exist");
            return "";
        }


        String jsonText = new String(Files.readAllBytes(Paths.get(jsonFilePath)));

        if (jsonText.isEmpty()) {
            log.info("json File is empty");
            errorLabel.setText("File is empty");
            return "";
        }


        String encData = aesEncode(jsonText, EncodingKey.getText());

        // 암호화 실패 시 (encData가 null이면)
        if (encData == null || encData.isEmpty()) {
            log.info("Encoding failed due to invalid key or empty key.");
            errorLabel.setText("Invalid or missing key. Encoding failed.");
            return ""; // 파일 생성하지 않음
        }

        // 인코딩된 내용을 JSON 객체에 추가
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("", encData);

        // 수정된 JSON을 파일에 쓰기
        Files.write(Paths.get(outputFile), jsonObject.toString().getBytes());
        errorLabel.setText("Success");
        log.info("JSON File successful  Encoding");

        // 복호화 테스트
        String decodedText = aesDecode(encData, EncodingKey.getText());
        return decodedText;
        // log.info("Decoding text: {}", decodedText);
    }
}
