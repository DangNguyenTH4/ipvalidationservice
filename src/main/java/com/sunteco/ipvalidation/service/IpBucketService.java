package com.sunteco.ipvalidation.service;

import com.sunteco.ipvalidation.constant.SystemKafkaTopic;
import com.sunteco.ipvalidation.model.domain.BucketIpCache;
import com.sunteco.ipvalidation.model.request.BucketIpAllowRequest;
import com.sunteco.ipvalidation.model.request.RequestDetectedInfo;
import com.sunteco.ipvalidation.model.response.BucketIpAllowResponse;
import com.sunteco.ipvalidation.repository.IpBucketRepository;
import com.sunteco.ipvalidation.utils.JacksonUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Array;
import java.util.*;

@Service
@Slf4j
public class IpBucketService {
    @Autowired
    private IpV4RangeService ipV4RangeService;
    @Value("${system.endpoint.primary:s3.sunteco.cloud, localhost:8080}")
    private Set<String> primaryDomain ;
    private String serviceSessionId;
    @PostConstruct
    public void init() {
        log.info("Initializing IpBucketService, send signal get config");
        this.serviceSessionId = UUID.randomUUID().toString();
        BucketKafka bucketKafka =  new BucketKafka();
        bucketKafka.setServiceTransactionId(serviceSessionId);
        this.kafkaTemplate.send(SystemKafkaTopic.IP_VALIDATION_INSTANCE_INIT, JacksonUtils.write(bucketKafka));
    }

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
            if(!CollectionUtils.isEmpty(IpBucketRepository.bucketAllowIps.get(request.getBucket()).getIps())) {
                response.setIps(IpBucketRepository.bucketAllowIps.get(request.getBucket()).getIps());
            }
            if(!CollectionUtils.isEmpty(IpBucketRepository.bucketAllowIps.get(request.getBucket()).getCidrs())) {
                response.getIps().addAll(IpBucketRepository.bucketAllowIps.get(request.getBucket()).getCidrs());
            }
        } else {
            response.setIps(new HashSet<>());
        }
        return response;
    }

    public void updateBatch(BucketIpAllowRequest request) {

        if (!IpBucketRepository.bucketAllowIps.containsKey(request.getBucket())) {
            return;
        }

        if (IpBucketRepository.bucketAllowIps.get(request.getBucket()).getCidrs() == null) {
            IpBucketRepository.bucketAllowIps.get(request.getBucket()).setCidrs(new HashSet<>());
        }

        if (IpBucketRepository.bucketAllowIps.get(request.getBucket()).getIps() == null) {
            IpBucketRepository.bucketAllowIps.get(request.getBucket()).setIps(new HashSet<>());
        }

        if(!CollectionUtils.isEmpty(request.getRemovedIps())) {
            Set<String> ipsRemove = new HashSet<>();
            Set<String> cidrsRemove = new HashSet<>();
            for (String ip : request.getRemovedIps()) {
                if (ipV4RangeService.isValidCIDR(ip)) {
                    cidrsRemove.add(ip);
                } else {
                    ipsRemove.add(ip);
                }
            }
            IpBucketRepository.bucketAllowIps.get(request.getBucket()).getCidrs().removeAll(cidrsRemove);
            IpBucketRepository.bucketAllowIps.get(request.getBucket()).getIps().removeAll(ipsRemove);
        }

        if(!CollectionUtils.isEmpty(request.getAddedIps())) {
            Set<String> ipsAdd = new HashSet<>();
            Set<String> cidrsAdd = new HashSet<>();
            for (String ip : request.getAddedIps()) {
                if (ipV4RangeService.isValidCIDR(ip)) {
                    cidrsAdd.add(ip);
                } else {
                    ipsAdd.add(ip);
                }
            }
            IpBucketRepository.bucketAllowIps.get(request.getBucket()).getCidrs().addAll(cidrsAdd);
            IpBucketRepository.bucketAllowIps.get(request.getBucket()).getIps().addAll(ipsAdd);
        }

        this.triggerBucketIpCacheUpdate(request.getBucket(), IpBucketRepository.bucketAllowIps.get(request.getBucket()));

    }

    @Autowired
    private KafkaTemplate kafkaTemplate;
    private void triggerBucketIpCacheUpdate(String bucket, BucketIpCache cache) {
        log.info("Bucket ip cache update triggered.");
        log.info("Bucket {}, ips {}", bucket, JacksonUtils.write(cache));
        //TODO push to kafka to handler at other service
        BucketKafka bucketKafka = new BucketKafka();
        bucketKafka.setBucket(bucket);
        bucketKafka.setCache(cache);
        bucketKafka.setServiceTransactionId(serviceSessionId);
        kafkaTemplate.send(SystemKafkaTopic.BUCKET_CACHE_IP_VALIDATION_UPDATE, JacksonUtils.write(bucketKafka));
    }

    public void handle(BucketIpAllowRequest request) {
        log.info("Handling bucket ip allow request: {}.", JacksonUtils.write(request));
        if(request.getAction() == null || request.getAction().isEmpty()) {
            return;
        }
        if("add".equals(request.getAction())) {
            this.add(request);
        }else if("remove".equals(request.getAction())) {
            this.remove(request);
        }else if("update".equals(request.getAction())) {
            this.update(request);
        }else if("list".equals(request.getAction())) {
            BucketIpAllowResponse response = this.list(request);
            response.setAction("list");
            kafkaTemplate.send(SystemKafkaTopic.BUCKET_CACHE_IP_VALIDATION_LIST, JacksonUtils.write(response));
        }else if("updateBatch".equals(request.getAction())) {
            this.updateBatch(request);
        }
    }

    public void handle(BucketKafka request) {
        log.info("Handle bucket kafka request: {}.", JacksonUtils.write(request));

        if (serviceSessionId == null) {
            log.info("service session id not set, skip handling bucket ip allow request.");
        }
        if (!serviceSessionId.equals(request.getServiceTransactionId())) {
            return;
        }
        if (request.getBucket() == null || request.getBucket().isEmpty()) {
            return;
        }
        if (request.getCache() == null) {
            return;
        }
        BucketIpCache cache = new BucketIpCache();
        if (request.getCache().getIps() != null) {
            cache.setIps(request.cache.getIps());
        }
        if (request.getCache().getCidrs() != null) {
            cache.setCidrs(request.cache.getCidrs());
        }
        IpBucketRepository.bucketAllowIps.put(request.getBucket(), cache);
    }

    @Getter
    @Setter
    public static class BucketKafka{
        private String bucket;
        private BucketIpCache cache;
        private String serviceTransactionId;
    }


    public boolean isAllowwIp(RequestDetectedInfo request) {
        String ip = request.getIp();
        String bucket = request.getBucket();
        if ("".equals(bucket)) {
            return true;
        }
        // If not setup before, it mean allow all
        if (!IpBucketRepository.bucketAllowIps.containsKey(bucket)) {
            log.debug("Not setup before so allow");
            return true;
        }

        if (IpBucketRepository.bucketAllowIps.get(bucket).getIps() != null && IpBucketRepository.bucketAllowIps.get(bucket).getIps().contains(ip)) {
            return true;
        }
        if(IpBucketRepository.bucketAllowIps.get(bucket).getCidrs() != null) {
            for (String cidr : IpBucketRepository.bucketAllowIps.get(bucket).getCidrs()) {
                if (ipV4RangeService.isInRange(ip, cidr)) {
                    return true;
                }
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
        log.info("Path {}", path);
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] parts = path.split("/");
        log.info("Parts {}", JacksonUtils.write(parts));
        return parts[0];
    }

    private String getBucketFromDomain(String domain) {
        return null;
    }
    public RequestDetectedInfo detectIp(HttpServletRequest request) {
        log(request);
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

    public static void log(HttpServletRequest request) {
        log.debug("========== 🐾 HTTP REQUEST DEBUG INFO ==========");

        // 🎯 IP
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String xRealIp = request.getHeader("X-Real-IP");
        String remoteAddr = request.getRemoteAddr();

        String clientIp = xForwardedFor != null && !xForwardedFor.isEmpty()
                ? xForwardedFor
                : (xRealIp != null && !xRealIp.isEmpty() ? xRealIp : remoteAddr);

        log.debug("➡️  [Client IP]            " + clientIp);
        log.debug("🔁 X-Forwarded-For:        " + JacksonUtils.write(xForwardedFor));
        log.debug("🔁 X-Real-IP:              " + JacksonUtils.write(xRealIp));
        log.debug("🔁 RemoteAddr:             " + remoteAddr);

        // 📄 Method, URI
        log.debug("📝 Method:                 " + request.getMethod());
        log.debug("🔗 URI:                    " + request.getRequestURI());
        log.debug("🔍 Query String:           " + request.getQueryString());

        // 🧠 Protocol, Encoding
        log.debug("📡 Protocol:               " + request.getProtocol());
        log.debug("🈳 Content Type:           " + request.getContentType());
        log.debug("🔤 Character Encoding:     " + request.getCharacterEncoding());

        // 📦 Headers
        log.debug("📦 Headers:");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            log.debug("    🧷 " + name + ": " + request.getHeader(name));
        }

        // 🧾 Parameters
        log.debug("🧾 Parameters:");
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            log.debug("    ✏️ " + entry.getKey() + ": " + String.join(", ", entry.getValue()));
        }

        log.debug("===============================================");
    }
}
