package com.sunteco.ipvalidation.constant;

public class BucketConstant {
    public static enum BucketPolicyName {
        Allow, Deny
    }
    public interface BucketPolicyAction{
        String ADD = "add";
        String REMOVE = "remove";
        String UPDATE = "update";
        String LIST = "list";
        String UPDATE_BATCH = "updateBatch";
        String DISABLE = "disable";
    }
}
