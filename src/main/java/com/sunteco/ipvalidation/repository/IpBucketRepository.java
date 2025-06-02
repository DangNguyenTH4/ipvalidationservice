package com.sunteco.ipvalidation.repository;

import com.sunteco.ipvalidation.model.domain.BucketIpCache;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
@Service
public class IpBucketRepository {

    private static final Set<String> ALLOWED_IPS = Set.of(
            "192.168.1.10", "10.0.0.5", "127.0.0.1", "192.168.1.198"
    );
    public static final Map<String, BucketIpCache> bucketAllowIps = new HashMap<>();
    public static final Map<String, BucketIpCache> bucketDeniedIps = new HashMap<>();
    static {
        bucketAllowIps.put("abc", new BucketIpCache(ALLOWED_IPS));
//        bucketAllowIps.put("def", new HashSet<>());
    }

    /**
     *
     */
    @PostConstruct
    void init() {
       //TODO call other service to get all information when service start
    }
}
