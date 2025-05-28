package com.sunteco.ipvalidation.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@Slf4j
public class IpV4RangeService {
    public boolean isInRange(String ip, String ipRange) {
        log.debug("Checking if ip {} is with range {}", ip, ipRange);
        SubnetUtils utils = new SubnetUtils(ipRange);
        boolean isInRange = utils.getInfo().isInRange(ip);
        log.debug("isInRange {}", isInRange);
        return isInRange;
    }

//    public static void main(String[] args) {
//        log.info("IsValid: {}", isValidCIDR("127.0.0.1/20"));
//        log.info("IsValid: {}", isInRange("127.0.0.255", "127.0.0.1/31"));
//    }
    public boolean isValidCIDR(String cidr) {
        try {
            new SubnetUtils(cidr); // sẽ throw nếu sai định dạng
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * IP OR RANGE CHECKER
     */
    // Regex cho IP
    private static final String IPV4_REGEX =
            "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.|$)){4}$";

    // Regex cho CIDR
    private static final String CIDR_REGEX =
            "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.|$)){4}/(\\d|[1-2]\\d|3[0-2])$";

    // Regex cho IP Range: ip - ip
    private static final String RANGE_REGEX =
            "^\\s*((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.|$)){4}\\s*-\\s*((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.|$)){4}\\s*$";

    public static String detectType(String input) {
        if (Pattern.matches(IPV4_REGEX, input)) {
            return "IP";
        } else if (Pattern.matches(CIDR_REGEX, input)) {
            return "CIDR";
        } else if (Pattern.matches(RANGE_REGEX, input)) {
            return "IP_RANGE";
        } else {
            return "UNKNOWN";
        }
    }

}
