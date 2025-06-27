package com.sunteco.ipvalidation.service;

import com.sunteco.ipvalidation.model.domain.BucketKafka;
import com.sunteco.ipvalidation.model.request.BucketIpAllowRequest;
import com.sunteco.ipvalidation.model.request.BucketIpBlockRequest;
import com.sunteco.ipvalidation.model.request.RequestDetectedInfo;
import com.sunteco.ipvalidation.model.response.BucketIpAllowResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface IpPolicyAbstractService {
    void add(BucketIpBlockRequest request);

    void remove(BucketIpBlockRequest request);

    void update(BucketIpBlockRequest request);

    BucketIpAllowResponse list(BucketIpBlockRequest request);

    BucketIpAllowResponse list(BucketIpAllowRequest request);

    void updateBatch(BucketIpBlockRequest request);

    void updateBatch(BucketIpAllowRequest request);

    void handleBlockedSetting(BucketIpBlockRequest request);

    void handleAllowedSetting(BucketIpAllowRequest request);

    void handleAllowInit(BucketKafka bucketConfig);

    void handleBlockedSetting(BucketKafka bucketConfig);

    boolean isBlocked(RequestDetectedInfo request);

    RequestDetectedInfo detectRequestInfo(HttpServletRequest request);
}
