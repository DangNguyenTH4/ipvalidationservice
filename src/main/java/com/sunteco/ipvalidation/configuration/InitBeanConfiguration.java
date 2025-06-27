package com.sunteco.ipvalidation.configuration;

import com.sunteco.ipvalidation.service.IpBucketService;
import com.sunteco.ipvalidation.service.IpDomainPolicyService;
import com.sunteco.ipvalidation.service.IpPolicyAbstractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class InitBeanConfiguration {
    @Value("${system.config.policybean:bucket}")
    private String policyBean;
    @Bean
    public IpPolicyAbstractService initIpPolicyAbstractService() {
        if (policyBean == null || policyBean.isEmpty() || policyBean.equals("bucket")) {
            return new IpBucketService();
        }else if(policyBean.equals("host")){
            return new IpDomainPolicyService();
        }
        log.warn("Policy Bean not configured, policy: {}", policyBean);
        return null;
    }
}
