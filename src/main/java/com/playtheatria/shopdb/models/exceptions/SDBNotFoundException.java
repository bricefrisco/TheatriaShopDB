package com.playtheatria.shopdb.models.exceptions;

public class SDBNotFoundException extends RuntimeException {
    public SDBNotFoundException(String message) {
        super(message);
    }
}
