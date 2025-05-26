package com.sunteco.ipvalidation.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@Slf4j
public class IpAuthController {
    private static final Set<String> ALLOWED_IPS = Set.of(
            "192.168.1.10", "10.0.0.5", "127.0.0.1"
    );
    @PostMapping
    public ResponseEntity<String> checkAuth(HttpServletRequest request) {
        log.info("Check: ");
        String ip = extractClientIp(request);
        String  bucket = extractBucket(request);
        log.info("Check bucket: {}", bucket);
        log.info("IP: {}", ip);
        if (ALLOWED_IPS.contains(ip)) {
            return ResponseEntity.ok("Authorized");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden IP: " + ip);
        }
    }

    @GetMapping
    public ResponseEntity<String> getCheck(HttpServletRequest request) {
        log.info("Check: ");
        String ip = extractClientIp(request);
        String  bucket = extractBucket(request);
        log.info("Check bucket: {}", bucket);
        log.info("IP: {}", ip);
        if (ALLOWED_IPS.contains(ip)) {
            return ResponseEntity.ok("Authorized");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden IP: " + ip);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfwd = request.getHeader("x-forwarded-for");
        if (xfwd != null && !xfwd.isBlank()) {
            return xfwd.split(",")[0].trim(); // lấy IP đầu tiên
        }
        return request.getRemoteAddr();
    }
    private String extractBucket(HttpServletRequest request) {
        String bucket = request.getHeader("bucket");
        log.info("Bucket: {}", bucket);
        return bucket;
    }
}
