package com.example.stockanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NewsArticle {
    public String title;
    public String description;
    public String date;
    public String urlNews;
}