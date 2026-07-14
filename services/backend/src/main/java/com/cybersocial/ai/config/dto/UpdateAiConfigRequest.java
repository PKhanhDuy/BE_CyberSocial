package com.cybersocial.ai.config.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record UpdateAiConfigRequest(
        @NotNull Boolean enabled,

        @NotEmpty(message = "Cần ít nhất một mốc tương tác.")
        List<@NotNull @Positive(message = "Mốc tương tác phải là số nguyên dương.") Integer> tierThresholds,

        @NotNull(message = "Dữ liệu nhập vào phải là số nguyên (tính theo phút).")
        @Min(value = 1, message = "Chu kỳ chống dội phải lớn hơn hoặc bằng 1 phút.")
        Integer debounceMinutes,

        @NotNull(message = "Dữ liệu nhập vào phải là số nguyên.")
        @Min(value = 0, message = "Giá trị thiết lập không hợp lệ. Vui lòng chỉ nhập tỷ lệ phần trăm từ 0% đến 100%.")
        @Max(value = 100, message = "Giá trị thiết lập không hợp lệ. Vui lòng chỉ nhập tỷ lệ phần trăm từ 0% đến 100%.")
        Integer fakeThresholdPercent
) {
}
