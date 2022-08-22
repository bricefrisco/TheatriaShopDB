package com.playtheatria.shopdb.models.exceptions;

public class SDBUnauthorizedException extends RuntimeException {
    public SDBUnauthorizedException(String message) {
        super(message);
    }
}
