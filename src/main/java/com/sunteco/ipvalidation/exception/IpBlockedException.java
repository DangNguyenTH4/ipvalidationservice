package com.sunteco.ipvalidation.exception;

public class IpBlockedException extends RuntimeException{
    public IpBlockedException() {

    }
    public IpBlockedException(String message) {
        super(message);
    }
}
