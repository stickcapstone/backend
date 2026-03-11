package com.veriweb.veriweb_backend.dto.analysis;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AnalyzeRequest {

    @NotBlank(message = "URL을 입력해주세요.")
    private String url;
}
