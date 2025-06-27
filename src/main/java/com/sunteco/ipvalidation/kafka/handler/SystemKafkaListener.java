/*
 *  Copyright (C) 2021 Sunteco, Inc.
 *
 *  Sunteco License Notice
 *
 *  The contents of this file are subject to the Sunteco License
 *  Version 1.0 (the "License"). You may not use this file except in
 *  compliance with the License. The Initial Developer of the Original
 *  Code is Sunteco, JSC. Portions Copyright 2021 Sunteco JSC
 *
 *  All Rights Reserved.
 */

package com.sunteco.ipvalidation.kafka.handler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sunteco.ipvalidation.constant.SystemKafkaTopic;
import com.sunteco.ipvalidation.model.domain.BucketKafka;
import com.sunteco.ipvalidation.model.request.BucketIpAllowRequest;
import com.sunteco.ipvalidation.model.request.BucketIpBlockRequest;
import com.sunteco.ipvalidation.service.IpBucketService;
import com.sunteco.ipvalidation.service.IpPolicyAbstractService;
import com.sunteco.ipvalidation.utils.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * @author dang.nguyen1@sunteco.io
 */
@Component
public class SystemKafkaListener {

    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private IpPolicyAbstractService ipPolicyAbstractService;
    private ObjectMapper objectMapper = new ObjectMapper();
    @Value("${system.env}")
    private String systemEnvironment;

    {
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @KafkaListener(topics = SystemKafkaTopic.BUCKET_BLOCKED_IP_SETTING)
    void handleBucketIpBlockSettingAction(@Payload String message) {
        try {
            logger.info("BUCKET_BLOCKED_IP_SETTING event message: {}", message);
            BucketIpBlockRequest request = JacksonUtils.readValue(message, BucketIpBlockRequest.class);
            if (!systemEnvironment.equals(request.getEnv())) {
                return;
            }
            ipPolicyAbstractService.handleBlockedSetting(request);
        } catch (Exception e) {
            logger.error("cannot handle message BUCKET_BLOCKED_IP_SETTING {}", message);
            e.printStackTrace();
        }
    }
    @KafkaListener(topics = SystemKafkaTopic.BUCKET_ALLOWED_IP_SETTING)
    void handleBucketIpAllowSettingAction(@Payload String message) {
        try {
            logger.info("BUCKET_ALLOWED_IP_SETTING event message: {}", message);
            BucketIpAllowRequest request = JacksonUtils.readValue(message, BucketIpAllowRequest.class);
            if (!systemEnvironment.equals(request.getEnv())) {
                return;
            }
            ipPolicyAbstractService.handleAllowedSetting(request);
        } catch (Exception e) {
            logger.error("cannot handle message BUCKET_ALLOWED_IP_SETTING {}", message);
            e.printStackTrace();
        }
    }

    @KafkaListener(topics = SystemKafkaTopic.IP_VALIDATION_INSTANCE_INIT_ALLOW_HANDLE)
    void handleAllowIpInstanceInit(@Payload String message) {
        try {
            logger.info("IP_VALIDATION_INSTANCE_INIT_ALLOW_HANDLE event message: {}", message);
            BucketKafka request = JacksonUtils.readValue(message, BucketKafka.class);
            ipPolicyAbstractService.handleAllowInit(request);
        } catch (Exception e) {
            logger.error("cannot handle message IP_VALIDATION_INSTANCE_INIT_ALLOW_HANDLE {}", message);
            e.printStackTrace();
        }
    }

    @KafkaListener(topics = SystemKafkaTopic.IP_VALIDATION_INSTANCE_INIT_BLOCK_HANDLE)
    void handleBlockIpInstanceInit(@Payload String message) {
        try {
            logger.info("IP_VALIDATION_INSTANCE_INIT_BLOCK_HANDLE event message: {}", message);
            BucketKafka request = JacksonUtils.readValue(message, BucketKafka.class);
            ipPolicyAbstractService.handleBlockedSetting(request);
        } catch (Exception e) {
            logger.error("cannot handle message IP_VALIDATION_INSTANCE_INIT_BLOCK_HANDLE {}", message);
            e.printStackTrace();
        }
    }

}