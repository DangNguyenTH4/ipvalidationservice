package com.sunteco.ipvalidation.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BucketIpAllowRequest {
    private String action; //add, remove, update, list
    private String bucket;
    private Set<String> ips;
    //for update an existed
    private String oldIp;
    private String newIp;
    private Set<String> addedIps;
    private Set<String> removedIps;
    private String env;
}
