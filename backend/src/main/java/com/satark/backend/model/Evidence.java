package com.satark.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evidence {

    private String claim;

    private String status;

    private String details;

    private String sourceUrl;
}