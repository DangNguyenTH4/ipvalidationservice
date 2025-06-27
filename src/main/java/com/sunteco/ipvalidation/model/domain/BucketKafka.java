package com.sunteco.ipvalidation.model.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BucketKafka {
    private String bucket;
    private IpCache cache;
    private String serviceTransactionId;
    private String type; //block / allow
}
