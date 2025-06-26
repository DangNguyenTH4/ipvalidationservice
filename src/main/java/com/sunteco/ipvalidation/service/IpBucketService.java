package com.sunteco.ipvalidation.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sunteco.ipvalidation.constant.SystemKafkaTopic;
import com.sunteco.ipvalidation.exception.IpBlockedException;
import com.sunteco.ipvalidation.model.domain.BucketIpCache;
import com.sunteco.ipvalidation.model.request.BucketIpAllowRequest;
import com.sunteco.ipvalidation.model.request.BucketIpBlockRequest;
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

    public void add(BucketIpBlockRequest request) {
        Set<String> ips = new HashSet<>();
        Set<String> cidrs = new HashSet<>();
        for (String ip : request.getIps()) {
            if (ipV4RangeService.isValidCIDR(ip)) {
                cidrs.add(ip);
            } else {
                ips.add(ip);
            }
        }
        if (IpBucketRepository.bucketBlockedIps.containsKey(request.getBucket())) {
            BucketIpCache cache = IpBucketRepository.bucketBlockedIps.get(request.getBucket());
            cache.getIps().addAll(ips);
            cache.getCidrs().addAll(cidrs);
        } else {
            BucketIpCache cache = new BucketIpCache();
            cache.setIps(ips);
            cache.setCidrs(cidrs);
            IpBucketRepository.bucketBlockedIps.put(request.getBucket(), cache);
        }
        this.triggerBucketIpCacheUpdate(request.getBucket(), IpBucketRepository.bucketBlockedIps.get(request.getBucket()), "Deny");
    }

    public void remove(BucketIpBlockRequest request) {
        if (!IpBucketRepository.bucketBlockedIps.containsKey(request.getBucket())) {
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
        IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getCidrs().removeAll(cidrs);
        IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getIps().removeAll(ips);
        this.triggerBucketIpCacheUpdate(request.getBucket(), IpBucketRepository.bucketBlockedIps.get(request.getBucket()), "Deny");
    }

    public void update(BucketIpBlockRequest request) {
        if (!IpBucketRepository.bucketBlockedIps.containsKey(request.getBucket())) {
            return;
        }
        if (ipV4RangeService.isValidCIDR(request.getOldIp())) {
            IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getCidrs().remove(request.getOldIp());
        } else {
            IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getIps().remove(request.getOldIp());
        }
        if (ipV4RangeService.isValidCIDR(request.getNewIp())) {
            IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getCidrs().remove(request.getNewIp());
        } else {
            IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getIps().remove(request.getNewIp());
        }
        this.triggerBucketIpCacheUpdate(request.getBucket(), IpBucketRepository.bucketBlockedIps.get(request.getBucket()), "Deny");
    }

    public BucketIpAllowResponse list(BucketIpBlockRequest request) {
        BucketIpAllowResponse response = new BucketIpAllowResponse();
        response.setBucket(request.getBucket());
        if (IpBucketRepository.bucketBlockedIps.containsKey(request.getBucket())) {
            if(!CollectionUtils.isEmpty(IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getIps())) {
                response.setIps(IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getIps());
            }
            if(!CollectionUtils.isEmpty(IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getCidrs())) {
                response.getIps().addAll(IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getCidrs());
            }
        } else {
            response.setIps(new HashSet<>());
        }
        return response;
    }

    public BucketIpAllowResponse list(BucketIpAllowRequest request) {
        BucketIpAllowResponse response = new BucketIpAllowResponse();
        response.setBucket(request.getBucket());
        if (IpBucketRepository.bucketBlockIpsNotIn.containsKey(request.getBucket())) {
            if(!CollectionUtils.isEmpty(IpBucketRepository.bucketBlockIpsNotIn.get(request.getBucket()).getIps())) {
                response.setIps(IpBucketRepository.bucketBlockIpsNotIn.get(request.getBucket()).getIps());
            }
            if(!CollectionUtils.isEmpty(IpBucketRepository.bucketBlockIpsNotIn.get(request.getBucket()).getCidrs())) {
                response.getIps().addAll(IpBucketRepository.bucketBlockIpsNotIn.get(request.getBucket()).getCidrs());
            }
        } else {
            response.setIps(new HashSet<>());
        }
        return response;
    }

    public void updateBatch(BucketIpBlockRequest request) {

        if (!IpBucketRepository.bucketBlockedIps.containsKey(request.getBucket())) {
            BucketIpCache cache = new BucketIpCache(new HashSet<>(), new HashSet<>());
            IpBucketRepository.bucketBlockedIps.put(request.getBucket(), cache);
        }

        if (IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getCidrs() == null) {
            IpBucketRepository.bucketBlockedIps.get(request.getBucket()).setCidrs(new HashSet<>());
        }

        if (IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getIps() == null) {
            IpBucketRepository.bucketBlockedIps.get(request.getBucket()).setIps(new HashSet<>());
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
            IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getCidrs().removeAll(cidrsRemove);
            IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getIps().removeAll(ipsRemove);
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
            IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getCidrs().addAll(cidrsAdd);
            IpBucketRepository.bucketBlockedIps.get(request.getBucket()).getIps().addAll(ipsAdd);
        }

        this.triggerBucketIpCacheUpdate(request.getBucket(), IpBucketRepository.bucketBlockedIps.get(request.getBucket()), "Deny");

    }


    public void updateBatch(BucketIpAllowRequest request) {

        if (!IpBucketRepository.bucketBlockIpsNotIn.containsKey(request.getBucket())) {
            BucketIpCache cache = new BucketIpCache(new HashSet<>(), new HashSet<>());
            IpBucketRepository.bucketBlockIpsNotIn.put(request.getBucket(), cache);
        }

        if (IpBucketRepository.bucketBlockIpsNotIn.get(request.getBucket()).getCidrs() == null) {
            IpBucketRepository.bucketBlockIpsNotIn.get(request.getBucket()).setCidrs(new HashSet<>());
        }

        if (IpBucketRepository.bucketBlockIpsNotIn.get(request.getBucket()).getIps() == null) {
            IpBucketRepository.bucketBlockIpsNotIn.get(request.getBucket()).setIps(new HashSet<>());
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
            IpBucketRepository.bucketBlockIpsNotIn.get(request.getBucket()).getCidrs().removeAll(cidrsRemove);
            IpBucketRepository.bucketBlockIpsNotIn.get(request.getBucket()).getIps().removeAll(ipsRemove);
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
            IpBucketRepository.bucketBlockIpsNotIn.get(request.getBucket()).getCidrs().addAll(cidrsAdd);
            IpBucketRepository.bucketBlockIpsNotIn.get(request.getBucket()).getIps().addAll(ipsAdd);
        }

        this.triggerBucketIpCacheUpdate(request.getBucket(), IpBucketRepository.bucketBlockIpsNotIn.get(request.getBucket()), "Allow");

    }


    @Autowired
    private KafkaTemplate kafkaTemplate;
    private void triggerBucketIpCacheUpdate(String bucket, BucketIpCache cache, String type) {
        log.info("Bucket ip cache update triggered.");
        log.info("Bucket {}, ips {}", bucket, JacksonUtils.write(cache));
        //TODO push to kafka to handler at other service
        BucketKafka bucketKafka = new BucketKafka();
        bucketKafka.setBucket(bucket);
        bucketKafka.setCache(cache);
        bucketKafka.setServiceTransactionId(serviceSessionId);
        kafkaTemplate.send(SystemKafkaTopic.BUCKET_CACHE_IP_VALIDATION_UPDATE, JacksonUtils.write(bucketKafka));
    }

    public void handle(BucketIpBlockRequest request) {
        log.info("Handling bucket ip block request: {}.", JacksonUtils.write(request));
        if (request.getAction() == null || request.getAction().isEmpty()) {
            return;
        }
        BucketIpAllowResponse response = new BucketIpAllowResponse();
        response.setSuccess("SUCCESS");
        response.setBucket(request.getBucket());
        response.setRequestId(request.getRequestId());
        response.setAction(request.getAction());
        if ("add".equals(request.getAction())) {
            this.add(request);
        } else if ("remove".equals(request.getAction())) {
            this.remove(request);
        } else if ("update".equals(request.getAction())) {
            this.update(request);
        } else if ("list".equals(request.getAction())) {
            response = this.list(request);
            response.setSuccess("SUCCESS");
            response.setAction("list");
            response.setRequestId(request.getRequestId());
        } else if ("updateBatch".equals(request.getAction())) {
            this.updateBatch(request);
        } else if ("disable".equals(request.getAction())) {
            this.disable(request);
        }
        kafkaTemplate.send(SystemKafkaTopic.BUCKET_BLOCKED_IP_SETTING_RESULT, JacksonUtils.write(response));
    }

    public void handle(BucketIpAllowRequest request) {
        log.info("Handling bucket ip allow request: {}.", JacksonUtils.write(request));
        if (request.getAction() == null || request.getAction().isEmpty()) {
            return;
        }
        BucketIpAllowResponse response = new BucketIpAllowResponse();
        response.setSuccess("SUCCESS");
        response.setBucket(request.getBucket());
        response.setRequestId(request.getRequestId());
        response.setAction(request.getAction());
        if ("updateBatch".equals(request.getAction())) {
            this.updateBatch(request);
        } else if ("list".equals(request.getAction())) {
            response = this.list(request);
            response.setSuccess("SUCCESS");
            response.setAction("list");
            response.setRequestId(request.getRequestId());
        }else if ("disable".equals(request.getAction())) {
            this.disable(request);
        }
        kafkaTemplate.send(SystemKafkaTopic.BUCKET_ALLOWED_IP_SETTING_RESULT, JacksonUtils.write(response));
    }

    private void disable(BucketIpBlockRequest request) {
        if (!IpBucketRepository.bucketBlockedIps.containsKey(request.getBucket())) {
            return;
        }
        IpBucketRepository.bucketBlockedIps.remove(request.getBucket());
    }

    private void disable(BucketIpAllowRequest request) {
        if (!IpBucketRepository.bucketBlockIpsNotIn.containsKey(request.getBucket())) {
            return;
        }
        IpBucketRepository.bucketBlockIpsNotIn.remove(request.getBucket());
    }

    public void handle(BucketKafka bucketConfig) {
        log.info("Handle bucket kafka request: {}.", JacksonUtils.write(bucketConfig));

        if (serviceSessionId == null) {
            log.info("service session id not set, skip handling bucket ip allow request.");
        }
        if (!serviceSessionId.equals(bucketConfig.getServiceTransactionId())) {
            return;
        }
        if (bucketConfig.getBucket() == null || bucketConfig.getBucket().isEmpty()) {
            return;
        }
        if (bucketConfig.getCache() == null) {
            return;
        }
        BucketIpCache cache = new BucketIpCache();
        if (bucketConfig.getCache().getIps() != null) {
            cache.setIps(bucketConfig.getCache().getIps());
        }
        if (bucketConfig.getCache().getCidrs() != null) {
            cache.setCidrs(bucketConfig.getCache().getCidrs());
        }
        IpBucketRepository.bucketBlockedIps.put(bucketConfig.getBucket(), cache);
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BucketKafka{
        private String bucket;
        private BucketIpCache cache;
        private String serviceTransactionId;
        private String type; //block / allow
    }


    public boolean isBlocked(RequestDetectedInfo request) {
        try {
            checkIsBlockWithListBlockIps(request);
            checkIsBlockWithNotInIpAddress(request);
            return false;
        }catch (IpBlockedException e) {
            return true;
        }
    }

    private boolean checkIsBlockWithListBlockIps(RequestDetectedInfo request) {
        String ip = request.getIp();
        String bucket = request.getBucket();
        boolean blocked = false;
        if ("".equals(bucket)) {
            blocked = false; // khong detect duoc bucket
            return blocked;
        }
        // If not setup before, it mean allow all
        if (!IpBucketRepository.bucketBlockedIps.containsKey(bucket)) {
            log.debug("Not setup before so allow");
            blocked = false;
            return blocked;
        }


        if (IpBucketRepository.bucketBlockedIps.get(bucket).getIps() != null
                && IpBucketRepository.bucketBlockedIps.get(bucket).getIps().contains(ip)) {
            blocked = true;
            throw new IpBlockedException(String.format("IP %s is blocked because it contain in block list, when access bucket: %s",ip, bucket));
        }

        if(IpBucketRepository.bucketBlockedIps.get(bucket).getCidrs() != null) {
            for (String cidr : IpBucketRepository.bucketBlockedIps.get(bucket).getCidrs()) {
                if (ipV4RangeService.isInRange(ip, cidr)) {
                    throw new IpBlockedException(String.format("IP %s is blocked because it is part of cidr: %ss, when access bucket: %s",ip, cidr, bucket));
                }
            }
        }
        return false;
    }
    private boolean checkIsBlockWithNotInIpAddress(RequestDetectedInfo request) {
        String ip = request.getIp();
        String bucket = request.getBucket();
        //not set
        if(!IpBucketRepository.bucketBlockIpsNotIn.containsKey(bucket)) {
            return false;
        }

        if (IpBucketRepository.bucketBlockIpsNotIn.get(bucket).getIps() != null
                && IpBucketRepository.bucketBlockIpsNotIn.get(bucket).getIps().contains(ip)) {
            return false;
        }

        if(IpBucketRepository.bucketBlockIpsNotIn.get(bucket).getCidrs() != null) {
            for (String cidr : IpBucketRepository.bucketBlockIpsNotIn.get(bucket).getCidrs()) {
                if (ipV4RangeService.isInRange(ip, cidr)) {
                    return false;
                }
            }
        }

        throw new IpBlockedException(String.format("IP %s is blocked because not in list rejects, when access bucket: %s",ip, bucket));

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
