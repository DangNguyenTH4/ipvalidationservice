package com.sunteco.ipvalidation.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BucketIpAllowResponse {
    private String bucket;
    private Set<String> ips;
    //for update an existed
    private String oldIp;
    private String newIp;

    private String success;
    private String action;
    private String requestId;
}
