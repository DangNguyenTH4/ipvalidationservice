package com.sunteco.ipvalidation.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Error implements AuthorizedResponse {
    @JsonProperty("Code")
    public String Code = "AccessDenied";
    @JsonProperty("Message")
    public Object Message = "Blocked by IP validation";
    @JsonProperty("BucketName")
    public String BucketName;
    @JsonProperty("RequestId")
    public String RequestId;
    @JsonProperty("HostId")
    public String HostId;
}
