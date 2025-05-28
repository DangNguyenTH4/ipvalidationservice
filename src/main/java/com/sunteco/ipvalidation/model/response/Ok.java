package com.sunteco.ipvalidation.model.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ok implements AuthorizedResponse {
    public boolean authorized;
}
