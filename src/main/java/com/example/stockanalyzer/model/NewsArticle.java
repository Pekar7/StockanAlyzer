package com.example.stockanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsArticle {
    private String title;
    private String description;
    private LocalDate date;
    private String urlNews;
}