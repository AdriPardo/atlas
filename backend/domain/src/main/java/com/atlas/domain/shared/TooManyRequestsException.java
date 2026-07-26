package com.atlas.domain.shared;

public class TooManyRequestsException extends DomainException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
