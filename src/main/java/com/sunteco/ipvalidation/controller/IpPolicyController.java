package com.sunteco.ipvalidation.controller;

import com.sunteco.ipvalidation.model.request.BucketIpBlockRequest;
import com.sunteco.ipvalidation.model.response.BucketIpAllowResponse;
import com.sunteco.ipvalidation.service.IpPolicyAbstractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/sunteco/v1/policy/ip")
public class IpPolicyController {

    @Autowired
    private IpPolicyAbstractService ipPolicyService;
    @PostMapping
    public ResponseEntity<String> add(@RequestBody BucketIpBlockRequest request) {
        ipPolicyService.add(request);
        return ResponseEntity.ok("SUCCESS");
    }
    @DeleteMapping
    public ResponseEntity<String> remove(@RequestBody BucketIpBlockRequest request) {
        ipPolicyService.remove(request);

        return ResponseEntity.ok("SUCCESS");
    }

    @PutMapping
    public ResponseEntity<String> update(@RequestBody BucketIpBlockRequest request) {
        ipPolicyService.update(request);

        return ResponseEntity.ok("SUCCESS");
    }
    @PostMapping("/list")
    public ResponseEntity<BucketIpAllowResponse> list(@RequestBody BucketIpBlockRequest request) {
        BucketIpAllowResponse response = ipPolicyService.list(request);
        return ResponseEntity.ok(response);
    }
}