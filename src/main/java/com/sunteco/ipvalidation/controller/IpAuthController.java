package com.sunteco.ipvalidation.controller;

import com.sunteco.ipvalidation.model.response.AuthorizedResponse;
import com.sunteco.ipvalidation.model.response.Error;
import com.sunteco.ipvalidation.model.response.Ok;
import com.sunteco.ipvalidation.model.request.RequestDetectedInfo;
import com.sunteco.ipvalidation.service.IpPolicyAbstractService;
import com.sunteco.ipvalidation.utils.JacksonUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("**")
public class IpAuthController {
    @Autowired
    private IpPolicyAbstractService ipPolicyService;

    @PostMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<AuthorizedResponse> checkAuth(HttpServletRequest request) {
        RequestDetectedInfo detectedInfo = ipPolicyService.detectRequestInfo(request);
        log.info("[POST] Check bucket: {},  ip: {}, fullPath: {}", detectedInfo.getBucket(), detectedInfo.getIp(), detectedInfo.getRequestUri());
        return check(detectedInfo);
    }

    @GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<AuthorizedResponse> getCheck(HttpServletRequest request) {
        RequestDetectedInfo detectedInfo = ipPolicyService.detectRequestInfo(request);
        log.info("[GET] Check bucket: {},  ip: {}, fullPath: {}", detectedInfo.getBucket(), detectedInfo.getIp(), detectedInfo.getRequestUri());
        return check(detectedInfo);
    }
    @PutMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<AuthorizedResponse> putCheck(HttpServletRequest request) {
        RequestDetectedInfo detectedInfo = ipPolicyService.detectRequestInfo(request);
        log.info("[PUT] Check bucket: {},  ip: {}, fullPath: {}", detectedInfo.getBucket(), detectedInfo.getIp(), detectedInfo.getRequestUri());
        return check(detectedInfo);
    }
    @DeleteMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<AuthorizedResponse> deleteCheck(HttpServletRequest request) {
        RequestDetectedInfo detectedInfo = ipPolicyService.detectRequestInfo(request);
        log.info("[DELETE] Check bucket: {},  ip: {}, fullPath: {}", detectedInfo.getBucket(), detectedInfo.getIp(), detectedInfo.getRequestUri());
        return check(detectedInfo);
    }

    @PatchMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<AuthorizedResponse> patchCheck(HttpServletRequest request) {
        RequestDetectedInfo detectedInfo = ipPolicyService.detectRequestInfo(request);
        log.info("[PATCH] Check bucket: {},  ip: {}, fullPath: {}", detectedInfo.getBucket(), detectedInfo.getIp(), detectedInfo.getRequestUri());
        return check(detectedInfo);
    }
    @RequestMapping(method = RequestMethod.OPTIONS, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<AuthorizedResponse> optionCheck(HttpServletRequest request) {
        RequestDetectedInfo detectedInfo = ipPolicyService.detectRequestInfo(request);
        log.info("[OPTION] Check bucket: {},  ip: {}, fullPath: {}", detectedInfo.getBucket(), detectedInfo.getIp(), detectedInfo.getRequestUri());
        return check(detectedInfo);
    }

    @RequestMapping(method = RequestMethod.HEAD, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<AuthorizedResponse> headCheck(HttpServletRequest request) {
        RequestDetectedInfo detectedInfo = ipPolicyService.detectRequestInfo(request);
        log.info("[HEAD] Check bucket: {},  ip: {}, fullPath: {}", detectedInfo.getBucket(), detectedInfo.getIp(), detectedInfo.getRequestUri());
        return check(detectedInfo);
    }
    private ResponseEntity<AuthorizedResponse> check(RequestDetectedInfo detectedInfo){
        if (ipPolicyService.isBlocked(detectedInfo)) {
            log.debug("Denied request: {}", JacksonUtils.write(detectedInfo));
            return returnXmlForbidden(detectedInfo);
        } else {
            Ok authorizedResponse = new Ok();
            authorizedResponse.setAuthorized(true);
//            authorizedResponse.setCode("OK");
            log.debug("Allow request: {}", JacksonUtils.write(detectedInfo));
            return ResponseEntity.ok(authorizedResponse);
        }
    }

    private ResponseEntity<AuthorizedResponse> returnXmlForbidden(RequestDetectedInfo request) {
        //TODO return xml
        /**
         * <?xml * version="1.0" encoding="UTF-8"?>
         * <Error>
         *     <Code>AccessDenied</Code>
         *     <BucketName>testcloudsync-stc</BucketName>
         *     <RequestId>tx00000af6018bd5e72e05b-0068368f16-55220ff-default</RequestId>
         *     <HostId>55220ff-default-default</HostId>
         * </Error>
         */
        Error authorizedResponse = new Error();
        authorizedResponse.setBucketName(request.getBucket());
        authorizedResponse.setRequestId(request.getRequestId());
        authorizedResponse.setHostId(request.getHost());
        log.info("RequestId: {}", request.getRequestId());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(authorizedResponse);
    }
}
