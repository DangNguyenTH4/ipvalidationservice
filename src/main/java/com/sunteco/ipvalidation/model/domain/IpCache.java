package com.sunteco.ipvalidation.model.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IpCache {
    private Set<String> ips;
    private Set<String> cidrs;
//    private Set<String> ranges
    public IpCache(Set<String> ips) {
        this.ips = ips;
    }
}
