package com.isetda.idpengine;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JsonInDecode {
    private static final Logger log = LogManager.getLogger(JsonInDecode.class);
    public static String priKey = "iset2021!_1234567890a";
    public static String aesEncode(String plainText) throws Exception {
        String key = priKey;
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"),"AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(key.getBytes("UTF-8"));

        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE,keySpec,ivParameterSpec);
        byte[] enBytes = c.doFinal(plainText.getBytes("UTF-8"));

        return Hex.encodeHexString(enBytes);
    }
    public static String aesDecode(String encryptedText) throws Exception {
        String key = priKey;
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(key.getBytes("UTF-8")); // 동일한 IV 사용

        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec);

        // Hex 문자열을 바이트 배열로 변환
        byte[] encryptedBytes = Hex.decodeHex(encryptedText.toCharArray());
        byte[] decryptedBytes = c.doFinal(encryptedBytes);

        return new String(decryptedBytes, "UTF-8");
    }

    public void JsonEncoding2() throws Exception {
        String outputFile = "encoding.json";
        String jsonFilePath = "국가, 문서 양식별 추출 단어 리스트.json";
        String jsonText = new String(Files.readAllBytes(Paths.get(jsonFilePath)));

        System.out.println("안녕1 :"+jsonText);
        // JSON 파싱
        String encData = aesEncode(jsonText);


        log.info("인코딩 텍스트 : {}",encData);
        JSONObject jsonObject = new JSONObject();
        // 인코딩된 내용을 JSON 객체에 추가
        jsonObject.put("", encData);


        // 수정된 JSON을 파일에 쓰기
        Files.write(Paths.get(outputFile), jsonObject.toString().getBytes());

        log.info("JSON 파일이 성공적으로 인코딩되어 저장되었습니다.");
        String decodedText = aesDecode(encData);
        System.out.println("복호화된 텍스트: " + decodedText);
    }
}
