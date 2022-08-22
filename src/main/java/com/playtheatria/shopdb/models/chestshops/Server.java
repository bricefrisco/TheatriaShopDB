package com.playtheatria.shopdb.models.chestshops;

import com.playtheatria.shopdb.models.exceptions.ExceptionMessage;
import com.playtheatria.shopdb.models.exceptions.SDBIllegalArgumentException;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum Server {
    @JsonProperty("The_Ark")
    THE_ARK;

    public static Server fromString(String s) {
        if ("The_Ark".equals(s)) {
            return THE_ARK;
        }
        throw new SDBIllegalArgumentException(ExceptionMessage.INVALID_SERVER);
    }

    public static String toString(Server server) {
        if (server == null) return "";
        if (server == Server.THE_ARK) {
            return "The_Ark";
        }
        throw new SDBIllegalArgumentException(ExceptionMessage.INVALID_SERVER);
    }
}
