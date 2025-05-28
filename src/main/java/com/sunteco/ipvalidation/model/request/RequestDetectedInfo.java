package com.sunteco.ipvalidation.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestDetectedInfo {
    private String ip;
    private String bucket;
    private String host;
    private String region;
    private String requestId;
    private String hostId;
    private String requestUri;
}
