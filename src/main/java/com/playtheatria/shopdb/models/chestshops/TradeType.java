package com.playtheatria.shopdb.models.chestshops;

import com.playtheatria.shopdb.models.exceptions.ExceptionMessage;
import com.playtheatria.shopdb.models.exceptions.SDBIllegalArgumentException;

public enum TradeType {
    BUY,
    SELL;

    public static TradeType fromString(String s) {
        switch (s) {
            case "buy":
                return BUY;
            case "sell":
                return SELL;
            default:
                throw new SDBIllegalArgumentException(ExceptionMessage.INVALID_TRADE_TYPE);
        }
    }
}
