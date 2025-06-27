package com.sunteco.ipvalidation.service;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
@Slf4j
public class InstanceSessionService {
    public static final String serviceSessionId;
    static {
        serviceSessionId = UUID.randomUUID().toString();
        log.info("Instance session id is {}", serviceSessionId);
    }
}
