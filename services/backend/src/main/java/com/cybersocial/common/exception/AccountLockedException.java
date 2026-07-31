package com.cybersocial.common.exception;

public class AccountLockedException extends RuntimeException {

    private final String reason;

    public AccountLockedException(String reason) {
        super("Tài khoản của bạn đã bị khóa");
        this.reason = reason == null || reason.isBlank()
                ? "Không có lý do được cung cấp."
                : reason.trim();
    }

    public String getReason() {
        return reason;
    }
}
