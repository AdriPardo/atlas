package com.atlas.domain.shared;

public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
