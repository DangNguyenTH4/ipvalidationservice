package com.sunteco.ipvalidation.service;

import com.sunteco.ipvalidation.constant.BucketConstant;
import com.sunteco.ipvalidation.constant.SystemKafkaTopic;
import com.sunteco.ipvalidation.exception.IpBlockedException;
import com.sunteco.ipvalidation.model.domain.BucketKafka;
import com.sunteco.ipvalidation.model.domain.IpCache;
import com.sunteco.ipvalidation.model.request.BucketIpAllowRequest;
import com.sunteco.ipvalidation.model.request.BucketIpBlockRequest;
import com.sunteco.ipvalidation.model.request.RequestDetectedInfo;
import com.sunteco.ipvalidation.model.response.BucketIpAllowResponse;
import com.sunteco.ipvalidation.repository.IpBucketRepository;
import com.sunteco.ipvalidation.repository.IpDomainPolicyRepository;
import com.sunteco.ipvalidation.utils.JacksonUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class IpDomainPolicyService implements IpPolicyAbstractService{
    @Autowired
    private IpV4RangeService ipV4RangeService;
    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Value("${system.endpoint.primary:s3.sunteco.cloud, localhost:8080}")
    private Set<String> primaryDomain ;
    @PostConstruct
    public void init() {
        log.info("Initializing IpBucketService, send signal get config");
        BucketKafka bucketKafka =  new BucketKafka();
        bucketKafka.setServiceTransactionId(InstanceSessionService.serviceSessionId);
        this.kafkaTemplate.send(SystemKafkaTopic.IP_VALIDATION_INSTANCE_INIT_BLOCK, JacksonUtils.write(bucketKafka));
        this.kafkaTemplate.send(SystemKafkaTopic.IP_VALIDATION_INSTANCE_INIT_ALLOW, JacksonUtils.write(bucketKafka));
    }
    @Override
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
        if (denyList().containsKey(request.getBucket())) {
            IpCache cache = denyList().get(request.getBucket());
            cache.getIps().addAll(ips);
            cache.getCidrs().addAll(cidrs);
        } else {
            IpCache cache = new IpCache();
            cache.setIps(ips);
            cache.setCidrs(cidrs);
            denyList().put(request.getBucket(), cache);
        }
        this.triggerBucketIpCacheUpdate(request.getBucket(), denyList().get(request.getBucket()), BucketConstant.BucketPolicyName.Deny.name());
    }
    @Override
    public void remove(BucketIpBlockRequest request) {
        if (!denyList().containsKey(request.getBucket())) {
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
        denyList().get(request.getBucket()).getCidrs().removeAll(cidrs);
        denyList().get(request.getBucket()).getIps().removeAll(ips);
        this.triggerBucketIpCacheUpdate(request.getBucket(), denyList().get(request.getBucket()), BucketConstant.BucketPolicyName.Deny.name());
    }
    @Override
    public void update(BucketIpBlockRequest request) {
        if (!denyList().containsKey(request.getBucket())) {
            return;
        }
        if (ipV4RangeService.isValidCIDR(request.getOldIp())) {
            denyList().get(request.getBucket()).getCidrs().remove(request.getOldIp());
        } else {
            denyList().get(request.getBucket()).getIps().remove(request.getOldIp());
        }
        if (ipV4RangeService.isValidCIDR(request.getNewIp())) {
            denyList().get(request.getBucket()).getCidrs().remove(request.getNewIp());
        } else {
            denyList().get(request.getBucket()).getIps().remove(request.getNewIp());
        }
        this.triggerBucketIpCacheUpdate(request.getBucket(), denyList().get(request.getBucket()), BucketConstant.BucketPolicyName.Deny.name());
    }
    @Override
    public BucketIpAllowResponse list(BucketIpBlockRequest request) {
        BucketIpAllowResponse response = new BucketIpAllowResponse();
        response.setBucket(request.getBucket());
        if (denyList().containsKey(request.getBucket())) {
            if(!CollectionUtils.isEmpty(denyList().get(request.getBucket()).getIps())) {
                response.setIps(denyList().get(request.getBucket()).getIps());
            }
            if(!CollectionUtils.isEmpty(denyList().get(request.getBucket()).getCidrs())) {
                response.getIps().addAll(denyList().get(request.getBucket()).getCidrs());
            }
        } else {
            response.setIps(new HashSet<>());
        }
        return response;
    }
    @Override
    public BucketIpAllowResponse list(BucketIpAllowRequest request) {
        BucketIpAllowResponse response = new BucketIpAllowResponse();
        response.setBucket(request.getBucket());
        if (allowList().containsKey(request.getBucket())) {
            if(!CollectionUtils.isEmpty(allowList().get(request.getBucket()).getIps())) {
                response.setIps(allowList().get(request.getBucket()).getIps());
            }
            if(!CollectionUtils.isEmpty(allowList().get(request.getBucket()).getCidrs())) {
                response.getIps().addAll(allowList().get(request.getBucket()).getCidrs());
            }
        } else {
            response.setIps(new HashSet<>());
        }
        return response;
    }
    @Override
    public void updateBatch(BucketIpBlockRequest request) {

        if (!denyList().containsKey(request.getBucket())) {
            IpCache cache = new IpCache(new HashSet<>(), new HashSet<>());
            denyList().put(request.getBucket(), cache);
        }

        if (denyList().get(request.getBucket()).getCidrs() == null) {
            denyList().get(request.getBucket()).setCidrs(new HashSet<>());
        }

        if (denyList().get(request.getBucket()).getIps() == null) {
            denyList().get(request.getBucket()).setIps(new HashSet<>());
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
            denyList().get(request.getBucket()).getCidrs().removeAll(cidrsRemove);
            denyList().get(request.getBucket()).getIps().removeAll(ipsRemove);
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
            denyList().get(request.getBucket()).getCidrs().addAll(cidrsAdd);
            denyList().get(request.getBucket()).getIps().addAll(ipsAdd);
        }

        this.triggerBucketIpCacheUpdate(request.getBucket(), denyList().get(request.getBucket()), BucketConstant.BucketPolicyName.Deny.name());

    }

    @Override
    public void updateBatch(BucketIpAllowRequest request) {

        if (!allowList().containsKey(request.getBucket())) {
            IpCache cache = new IpCache(new HashSet<>(), new HashSet<>());
            allowList().put(request.getBucket(), cache);
        }

        if (allowList().get(request.getBucket()).getCidrs() == null) {
            allowList().get(request.getBucket()).setCidrs(new HashSet<>());
        }

        if (allowList().get(request.getBucket()).getIps() == null) {
            allowList().get(request.getBucket()).setIps(new HashSet<>());
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
            allowList().get(request.getBucket()).getCidrs().removeAll(cidrsRemove);
            allowList().get(request.getBucket()).getIps().removeAll(ipsRemove);
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
            allowList().get(request.getBucket()).getCidrs().addAll(cidrsAdd);
            allowList().get(request.getBucket()).getIps().addAll(ipsAdd);
        }

        this.triggerBucketIpCacheUpdate(request.getBucket(), allowList().get(request.getBucket()), BucketConstant.BucketPolicyName.Allow.name());

    }

    private void triggerBucketIpCacheUpdate(String bucket, IpCache cache, String type) {
        log.info("Bucket ip cache update triggered.");
        log.info("Bucket {}, ips {}", bucket, JacksonUtils.write(cache));
        //TODO push to kafka to handler at other service
        BucketKafka bucketKafka = new BucketKafka();
        bucketKafka.setBucket(bucket);
        bucketKafka.setCache(cache);
        bucketKafka.setType(type);
        bucketKafka.setServiceTransactionId(InstanceSessionService.serviceSessionId);
        kafkaTemplate.send(SystemKafkaTopic.BUCKET_CACHE_IP_VALIDATION_UPDATE, JacksonUtils.write(bucketKafka));
    }
    @Override
    public void handleBlockedSetting(BucketIpBlockRequest request) {
        log.info("Handling bucket ip block request: {}.", JacksonUtils.write(request));
        if (request.getAction() == null || request.getAction().isEmpty()) {
            return;
        }
        BucketIpAllowResponse response = new BucketIpAllowResponse();
        response.setSuccess("SUCCESS");
        response.setBucket(request.getBucket());
        response.setRequestId(request.getRequestId());
        response.setAction(request.getAction());
        if (BucketConstant.BucketPolicyAction.ADD.equals(request.getAction())) {
            this.add(request);
        } else if (BucketConstant.BucketPolicyAction.REMOVE.equals(request.getAction())) {
            this.remove(request);
        } else if (BucketConstant.BucketPolicyAction.UPDATE.equals(request.getAction())) {
            this.update(request);
        } else if (BucketConstant.BucketPolicyAction.LIST.equals(request.getAction())) {
            response = this.list(request);
            response.setSuccess("SUCCESS");
            response.setAction(BucketConstant.BucketPolicyAction.LIST);
            response.setRequestId(request.getRequestId());
        } else if (BucketConstant.BucketPolicyAction.UPDATE_BATCH.equals(request.getAction())) {
            this.updateBatch(request);
        } else if (BucketConstant.BucketPolicyAction.DISABLE.equals(request.getAction())) {
            this.disable(request);
        }
        kafkaTemplate.send(SystemKafkaTopic.BUCKET_BLOCKED_IP_SETTING_RESULT, JacksonUtils.write(response));
    }
    @Override
    public void handleAllowedSetting(BucketIpAllowRequest request) {
        log.info("Handling bucket ip allow request: {}.", JacksonUtils.write(request));
        if (request.getAction() == null || request.getAction().isEmpty()) {
            return;
        }
        BucketIpAllowResponse response = new BucketIpAllowResponse();
        response.setSuccess("SUCCESS");
        response.setBucket(request.getBucket());
        response.setRequestId(request.getRequestId());
        response.setAction(request.getAction());
        if (BucketConstant.BucketPolicyAction.UPDATE_BATCH.equals(request.getAction())) {
            this.updateBatch(request);
        } else if (BucketConstant.BucketPolicyAction.LIST.equals(request.getAction())) {
            response = this.list(request);
            response.setSuccess("SUCCESS");
            response.setAction(BucketConstant.BucketPolicyAction.LIST);
            response.setRequestId(request.getRequestId());
        }else if (BucketConstant.BucketPolicyAction.DISABLE.equals(request.getAction())) {
            this.disable(request);
        }
        kafkaTemplate.send(SystemKafkaTopic.BUCKET_ALLOWED_IP_SETTING_RESULT, JacksonUtils.write(response));
    }

    private void disable(BucketIpBlockRequest request) {
        if (!denyList().containsKey(request.getBucket())) {
            return;
        }
        denyList().remove(request.getBucket());
    }

    private void disable(BucketIpAllowRequest request) {
        if (!allowList().containsKey(request.getBucket())) {
            return;
        }
        allowList().remove(request.getBucket());
    }
    @Override
    public void handleAllowInit(BucketKafka bucketConfig) {
        log.info("Handle allow ip bucket kafka request: {}.", JacksonUtils.write(bucketConfig));


        if (!InstanceSessionService.serviceSessionId.equals(bucketConfig.getServiceTransactionId())) {
            return;
        }
        if (bucketConfig.getBucket() == null || bucketConfig.getBucket().isEmpty()) {
            return;
        }
        if (bucketConfig.getCache() == null) {
            return;
        }
        IpCache cache = new IpCache();
        if (bucketConfig.getCache().getIps() != null) {
            cache.setIps(bucketConfig.getCache().getIps());
        }
        if (bucketConfig.getCache().getCidrs() != null) {
            cache.setCidrs(bucketConfig.getCache().getCidrs());
        }
        allowList().put(bucketConfig.getBucket(), cache);
    }
    @Override
    public void handleBlockedSetting(BucketKafka bucketConfig) {
        log.info("Handle blocked ip bucket kafka request: {}.", JacksonUtils.write(bucketConfig));

        if (!InstanceSessionService.serviceSessionId.equals(bucketConfig.getServiceTransactionId())) {
            return;
        }
        if (bucketConfig.getBucket() == null || bucketConfig.getBucket().isEmpty()) {
            return;
        }
        if (bucketConfig.getCache() == null) {
            return;
        }
        IpCache cache = new IpCache();
        if (bucketConfig.getCache().getIps() != null) {
            cache.setIps(bucketConfig.getCache().getIps());
        }
        if (bucketConfig.getCache().getCidrs() != null) {
            cache.setCidrs(bucketConfig.getCache().getCidrs());
        }
        denyList().put(bucketConfig.getBucket(), cache);
    }

    @Override
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
        String key = this.getKeyCheck(request);
        boolean blocked = false;
        if ("".equals(key)) {
            blocked = false; // khong detect duoc bucket
            return blocked;
        }
        // If not setup before, it mean allow all
        if (!denyList().containsKey(key)) {
            log.debug("Not setup before so allow");
            blocked = false;
            return blocked;
        }


        if (denyList().get(key).getIps() != null
                && denyList().get(key).getIps().contains(ip)) {
            blocked = true;
            throw new IpBlockedException(String.format("IP %s is blocked because it contain in block list, when access bucket: %s",ip, key));
        }

        if(denyList().get(key).getCidrs() != null) {
            for (String cidr : denyList().get(key).getCidrs()) {
                if (ipV4RangeService.isInRange(ip, cidr)) {
                    throw new IpBlockedException(String.format("IP %s is blocked because it is part of cidr: %ss, when access bucket: %s",ip, cidr, key));
                }
            }
        }
        return false;
    }
    private boolean checkIsBlockWithNotInIpAddress(RequestDetectedInfo request) {
        String ip = request.getIp();
        String key = this.getKeyCheck(request);
        //not set
        if(!allowList().containsKey(key)) {
            return false;
        }

        if (allowList().get(key).getIps() != null
                && allowList().get(key).getIps().contains(ip)) {
            return false;
        }

        if(allowList().get(key).getCidrs() != null) {
            for (String cidr : allowList().get(key).getCidrs()) {
                if (ipV4RangeService.isInRange(ip, cidr)) {
                    return false;
                }
            }
        }

        throw new IpBlockedException(String.format("IP %s is blocked because not in list rejects, when access bucket: %s",ip, key));

    }

    private String extractClientIp(HttpServletRequest request) {
        String xfwd = request.getHeader("x-forwarded-for");
        if (xfwd != null && !xfwd.isBlank()) {
            return xfwd.split(",")[0].trim(); // lấy IP đầu tiên
        }
        return request.getRemoteAddr();
    }

    @Override
    public RequestDetectedInfo detectRequestInfo(HttpServletRequest request) {
        log(request);
        String ip = extractClientIp(request);
        RequestDetectedInfo detectedInfo = new RequestDetectedInfo();
        detectedInfo.setIp(ip);
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
    private Map<String, IpCache> allowList(){
        return IpDomainPolicyRepository.allowList;
    }
    private Map<String, IpCache> denyList(){
        return IpDomainPolicyRepository.denyList;
    }
    private String getKeyCheck(RequestDetectedInfo request) {
        return request.getHost();
    }
}
