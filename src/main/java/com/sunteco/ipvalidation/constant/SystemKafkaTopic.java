package com.sunteco.ipvalidation.constant;

public interface SystemKafkaTopic {
    String IP_VALIDATION_INSTANCE_INIT = "ip-validation-instance-init";
    String IP_VALIDATION_INSTANCE_INIT_HANDLE = "ip-validation-instance-init-handle";
    String BUCKET_ALLOWED_IP_SETTING = "bucket-allowed-ip-setting";
    String BUCKET_BLOCKED_IP_SETTING = "bucket-blocked-ip-setting";
    String BUCKET_ALLOWED_IP_SETTING_RESULT = "bucket-allowed-ip-setting-result";
    String BUCKET_BLOCKED_IP_SETTING_RESULT = "bucket-block-ip-setting-result";
    String BUCKET_CACHE_IP_VALIDATION_UPDATE = "bucket-cache-ip-validation-update";
    String BUCKET_CACHE_IP_VALIDATION_LIST = "bucket-cache-ip-validation-list";
}
