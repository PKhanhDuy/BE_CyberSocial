package com.cybersocial.admin.dto;

public enum AdminVerdictDecision {
    /** Đồng ý với AI: dán nhãn cảnh báo công khai, giữ hiển thị bài viết. */
    CONFIRM_FAKE,
    /** Bác bỏ nhãn AI: gỡ nhãn nghi vấn, trả bài về hiển thị bình thường. */
    REJECT_LABEL
}
