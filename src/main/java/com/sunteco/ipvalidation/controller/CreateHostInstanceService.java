package com.sunteco.ipvalidation.controller;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class CreateHostInstanceService {
    public static Map<String, Object> create() {
        Map<String, Object> params = new LinkedHashMap<>();

//        params.put("ProjectId", "org-h4xlbw");
        params.put("VPCId", "uvnet-1avb95ux1g5x");
        params.put("SubnetId", "subnet-1avb96p5t6vp");
        params.put("SecurityMode", "Firewall");
        params.put("SecurityGroupId", 186638);
        params.put("NetworkInterface[0][EIP][OperatorName]", "International");
        params.put("NetworkInterface[0][EIP][Bandwidth]", 1);
        params.put("NetworkInterface[0][EIP][PayMode]", "Bandwidth");
        params.put("MachineType", "O");
        params.put("MinimalCpuPlatform", "Intel/Auto");
        params.put("CPU", "1");
        params.put("Memory", "1024");
//        params.put("LoginMode", "Password");
//        params.put("Password", "Test@123Test@123");
        params.put("ImageId", "uimage-cb1hut");
        params.put("NetCapability", "Normal");
        params.put("HotplugFeature", false);
        params.put("Features[UNI]", false);
        params.put("Disks[0][IsBoot]", true);
        params.put("Disks[0][Type]", "CLOUD_RSSD");
        params.put("Disks[0][Size]", "20");
        params.put("Disks[0][BackupType]", "NONE");
        params.put("AutoDataDiskInit", "On");
        params.put("Tag", "Default");
        params.put("ChargeType", "Dynamic");
        params.put("Quantity", "1");
        params.put("Region", "vn-sng");
        params.put("Zone", "vn-sng-01");
        params.put("UHostName", "UHost");
        params.put("SecurityReinforce", false);
        params.put("ImageName", "高内核Ubuntu 18.04 64bits");
        params.put("ImageType", "Base");
        params.put("OsName", "高内核Ubuntu 18.04 64位");
        params.put("Name", "test");
        params.put("Action", "CreateUHostTemplate");
//        params.put("_user", "cottontang8@gmail.com");
//        params.put("_timestamp", "1747276140325");
        return params;
    }
}
