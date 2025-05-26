package com.sunteco.ipvalidation.controller;

import com.sunteco.ipvalidation.CoreClient;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
public class Ucloud {

    public static String generateSignature(Map<String, Object> rawParams, String privateKey) throws Exception {
        // Bước 1: Xóa Signature
        rawParams.remove("Signature");
        log.info("{}", rawParams);
        // Bước 2: Sắp xếp key
        List<String> sortedKeys = new ArrayList<>(rawParams.keySet());
        Collections.sort(sortedKeys);
        log.info("{}", sortedKeys);
        // Bước 3: Ghép chuỗi key + normalizedValue
        StringBuilder sb = new StringBuilder();
        for (String key : sortedKeys) {
            Object rawValue = rawParams.get(key);
            if (rawValue != null) {
                sb.append(key).append(normalizeValue(rawValue));
            }
        }

        sb.append(privateKey);
        String dataToSign = sb.toString();
        log.info("{}", dataToSign);
        // Bước 4: Ký HMAC-SHA1
        return sha1Hex(dataToSign);
    }

    private static String normalizeValue(Object value) {
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "true" : "false";
        } else if (value instanceof Float || value instanceof Double) {
            double val = ((Number) value).doubleValue();
            if (val == Math.floor(val)) {
                return String.format("%.0f", val);
            } else {
                return BigDecimal.valueOf(val).toPlainString();
            }
        } else {
            return value.toString();
        }
    }

    public static String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes());
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }

    public static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b & 0xff));
        }
        return hex.toString();
    }

    public static void main(String[] args) throws Exception {
        CoreClient coreClient = new CoreClient();
        Map<String, Object> params = new HashMap<>();
        params.put("Action", "GetRegion");
        params.put("PublicKey", "4eZCt4QVa7B2wB0Ef8WUycEFxfBBuIBph");


        String privateKey = "ILarvSbBnBtDyHJzj5KyMc72KOsYtKJBr8vZEgOPyXYt";
        String signature = generateSignature(params, privateKey);
        log.info("Generated Signature: " + signature);
        params.put("Signature", signature);
        Object o = coreClient.get("", params);
        log.info("Get region: {}", o);
        Map<String,Object> hostInstanceParam = CreateHostInstanceService.create();

        hostInstanceParam.put("PublicKey", "4eZCt4QVa7B2wB0Ef8WUycEFxfBBuIBph");

        String signatureCreateHost = generateSignature(hostInstanceParam, signature);
        hostInstanceParam.put("Signature", signatureCreateHost);
        log.info("Signature: {}", signatureCreateHost);
        Object creaetHost = coreClient.get("", hostInstanceParam);
        log.info("Create host: {}", creaetHost);

    }
}
