package com.example.blogs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ModerateRequest {
    @NotBlank
    private String action; // "APPROVE" | "REJECT"
    private String note;
}
