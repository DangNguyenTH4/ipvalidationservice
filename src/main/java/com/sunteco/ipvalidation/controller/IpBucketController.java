package com.sunteco.ipvalidation.controller;

import com.sunteco.ipvalidation.model.request.BucketIpAllowRequest;
import com.sunteco.ipvalidation.model.response.BucketIpAllowResponse;
import com.sunteco.ipvalidation.repository.IpBucketRepository;
import com.sunteco.ipvalidation.service.IpBucketService;
import com.sunteco.ipvalidation.service.IpV4RangeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Slf4j
@RequestMapping("/api/sunteco/v1/bucket/ip")
public class IpBucketController {

    @Autowired
    private IpBucketService ipBucketService;
    @PostMapping
    public ResponseEntity<String> add(@RequestBody BucketIpAllowRequest request) {
        ipBucketService.add(request);
        return ResponseEntity.ok("SUCCESS");
    }
    @DeleteMapping
    public ResponseEntity<String> remove(@RequestBody BucketIpAllowRequest request) {
        ipBucketService.remove(request);

        return ResponseEntity.ok("SUCCESS");
    }

    @PutMapping
    public ResponseEntity<String> update(@RequestBody BucketIpAllowRequest request) {
        ipBucketService.update(request);

        return ResponseEntity.ok("SUCCESS");
    }
    @PostMapping("/list")
    public ResponseEntity<BucketIpAllowResponse> list(@RequestBody BucketIpAllowRequest request) {
        BucketIpAllowResponse response = ipBucketService.list(request);
        return ResponseEntity.ok(response);
    }
}