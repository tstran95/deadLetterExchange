package com.vn.deadletter.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vn.deadletter.constant.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseEntity {
    private String code;
    private String status;
    private String description;

    public static ResponseEntity getResponse(String code, String status, String description) {
        return ResponseEntity.builder()
                            .code(code)
                            .status(status)
                            .description(description)
                            .build();
    }

    @JsonIgnore
    public static ResponseEntity errorResponse(){
        return ResponseEntity.builder()
                .code(Constant.ERROR_CODE)
                .status(Constant.FAIL)
                .build();
    }
}
