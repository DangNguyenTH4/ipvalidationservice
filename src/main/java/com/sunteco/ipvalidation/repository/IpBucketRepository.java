package com.sunteco.ipvalidation.repository;

import com.sunteco.ipvalidation.model.domain.BucketIpCache;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
@Service
public class IpBucketRepository {

    private static final Set<String> ALLOWED_IPS = Set.of(
            "192.168.1.10", "10.0.0.5", "127.0.0.1", "192.168.1.198"
    );
    public static final Map<String, BucketIpCache> bucketBlockedIps = new HashMap<>();
    /**
     * Like AWS Deny all IP, if not in. If the client ip in side range, it will allow
     */
    public static final Map<String, BucketIpCache> bucketBlockIpsNotIn = new HashMap<>();
    static {
//        bucketAllowedIps.put("abc", new BucketIpCache(ALLOWED_IPS));
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
