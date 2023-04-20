package com.example.stockanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Value
public class SentimetModel {
    private LocalDateTime date;
    private String analyseText;
    private Integer rating;
}
