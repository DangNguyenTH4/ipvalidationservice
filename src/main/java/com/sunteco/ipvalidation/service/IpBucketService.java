package com.sunteco.ipvalidation.service;

import com.sunteco.ipvalidation.model.domain.BucketIpCache;
import com.sunteco.ipvalidation.model.request.BucketIpAllowRequest;
import com.sunteco.ipvalidation.model.request.RequestDetectedInfo;
import com.sunteco.ipvalidation.model.response.BucketIpAllowResponse;
import com.sunteco.ipvalidation.repository.IpBucketRepository;
import com.sunteco.ipvalidation.utils.JacksonUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class IpBucketService {
    @Autowired
    private IpV4RangeService ipV4RangeService;
    private Set<String> primaryDomain = Set.of("s3.sunteco.cloud", "localhost:8080");
    public void add(BucketIpAllowRequest request) {
        Set<String> ips = new HashSet<>();
        Set<String> cidrs = new HashSet<>();
        for (String ip : request.getIps()) {
            if (ipV4RangeService.isValidCIDR(ip)) {
                cidrs.add(ip);
            } else {
                ips.add(ip);
            }
        }
        if (IpBucketRepository.bucketAllowIps.containsKey(request.getBucket())) {
            BucketIpCache cache = IpBucketRepository.bucketAllowIps.get(request.getBucket());
            cache.getIps().addAll(ips);
            cache.getCidrs().addAll(cidrs);
        } else {
            BucketIpCache cache = new BucketIpCache();
            cache.setIps(ips);
            cache.setCidrs(cidrs);
            IpBucketRepository.bucketAllowIps.put(request.getBucket(), cache);
        }
        this.triggerBucketIpCacheUpdate(request.getBucket(), IpBucketRepository.bucketAllowIps.get(request.getBucket()));
    }

    public void remove(BucketIpAllowRequest request) {
        if (!IpBucketRepository.bucketAllowIps.containsKey(request.getBucket())) {
            return;
        }
        Set<String> ips = new HashSet<>();
        Set<String> cidrs = new HashSet<>();
        for (String ip : request.getIps()) {
            if (ipV4RangeService.isValidCIDR(ip)) {
                cidrs.add(ip);
            } else {
                ips.add(ip);
            }
        }
        IpBucketRepository.bucketAllowIps.get(request.getBucket()).getCidrs().removeAll(cidrs);
        IpBucketRepository.bucketAllowIps.get(request.getBucket()).getIps().removeAll(ips);
        this.triggerBucketIpCacheUpdate(request.getBucket(), IpBucketRepository.bucketAllowIps.get(request.getBucket()));
    }

    public void update(BucketIpAllowRequest request) {
        if (!IpBucketRepository.bucketAllowIps.containsKey(request.getBucket())) {
            return;
        }
        if (ipV4RangeService.isValidCIDR(request.getOldIp())) {
            IpBucketRepository.bucketAllowIps.get(request.getBucket()).getCidrs().remove(request.getOldIp());
        } else {
            IpBucketRepository.bucketAllowIps.get(request.getBucket()).getIps().remove(request.getOldIp());
        }
        if (ipV4RangeService.isValidCIDR(request.getNewIp())) {
            IpBucketRepository.bucketAllowIps.get(request.getBucket()).getCidrs().remove(request.getNewIp());
        } else {
            IpBucketRepository.bucketAllowIps.get(request.getBucket()).getIps().remove(request.getNewIp());
        }
        this.triggerBucketIpCacheUpdate(request.getBucket(), IpBucketRepository.bucketAllowIps.get(request.getBucket()));
    }

    public BucketIpAllowResponse list(BucketIpAllowRequest request) {
        BucketIpAllowResponse response = new BucketIpAllowResponse();
        response.setBucket(request.getBucket());
        if (IpBucketRepository.bucketAllowIps.containsKey(request.getBucket())) {
            response.setIps(IpBucketRepository.bucketAllowIps.get(request.getBucket()).getIps());
            response.getIps().addAll(IpBucketRepository.bucketAllowIps.get(request.getBucket()).getCidrs());
        } else {
            response.setIps(new HashSet<>());
        }
        return response;
    }
    @Autowired
    private KafkaTemplate kafkaTemplate;
    private void triggerBucketIpCacheUpdate(String bucket, BucketIpCache cache) {
        log.info("Bucket ip cache update triggered.");
        log.info("Bucket {}, ips {}", bucket, JacksonUtils.write(cache.getIps()));
        //TODO push to kafka to handler at other service
        BucketKafka bucketKafka = new BucketKafka();
        bucketKafka.setBucket(bucket);
        bucketKafka.setCache(cache);
        kafkaTemplate.send("BUCKET_CACHE_IP_VALIDATION_UPDATE", JacksonUtils.write(bucketKafka));
    }
    @Getter
    @Setter
    public static class BucketKafka{
        private String bucket;
        private BucketIpCache cache;
    }


    public boolean isAllowwIp(RequestDetectedInfo request) {
        String ip = request.getIp();
        String bucket = request.getBucket();
        if ("".equals(bucket)) {
            return true;
        }
        // If not setup before, it mean allow all
        if (!IpBucketRepository.bucketAllowIps.containsKey(bucket)) {
            return true;
        }

        if (IpBucketRepository.bucketAllowIps.get(bucket).getIps().contains(ip)) {
            return true;
        }

        for (String cidr : IpBucketRepository.bucketAllowIps.get(bucket).getCidrs()) {
            if(ipV4RangeService.isInRange(ip, cidr)) {
                return true;
            }
        }
        return false;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfwd = request.getHeader("x-forwarded-for");
        if (xfwd != null && !xfwd.isBlank()) {
            return xfwd.split(",")[0].trim(); // lấy IP đầu tiên
        }
        return request.getRemoteAddr();
    }

    private String extractBucket(HttpServletRequest request) {
        String host = request.getHeader("host");
        String bucket = "";
        if (isPathStyle(host)) {
            bucket = getBucketFromPath(request);
        } else {
            bucket = getBucketFromDomain(host);
        }
        return bucket;
    }

    private boolean isPathStyle(String domain) {
        return primaryDomain.contains(domain);
    }

    private String getBucketFromPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] parts = path.split("/");
        return parts[0];
    }

    private String getBucketFromDomain(String domain) {
        return null;
    }
    public RequestDetectedInfo detectIp(HttpServletRequest request) {
        String ip = extractClientIp(request);
        String bucket = extractBucket(request);
        RequestDetectedInfo detectedInfo = new RequestDetectedInfo();
        detectedInfo.setIp(ip);
        detectedInfo.setBucket(bucket);
        detectedInfo.setRequestId(request.getRequestId());
        detectedInfo.setHost(request.getHeader("host"));
        detectedInfo.setRequestUri(request.getRequestURL().toString());
        return detectedInfo;
    }
}
