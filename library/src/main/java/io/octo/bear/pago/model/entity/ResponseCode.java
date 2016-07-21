package io.octo.bear.pago.model.entity;

/**
 * Created by shc on 14.07.16.
 */

public enum ResponseCode {

    OK(0),
    USER_CANCELED(1),
    SERVICE_UNAVAILABLE(2),
    BILLING_UNAVAILABLE(3),
    ITEM_UNAVAILABLE(4),
    DEVELOPER_ERROR(5),
    ERROR(6),
    ITEM_ALREADY_OWNED(7),
    ITEM_NOT_OWNED(8);

    public final int code;

    ResponseCode(int code) {
        this.code = code;
    }

    public static ResponseCode getByCode(int code) {
        for (ResponseCode responseCode : values()) {
            if (responseCode.code == code) {
                return responseCode;
            }
        }

        throw new IllegalArgumentException("unsupported response code");
    }
}
