package com.example.stockanalyzer.service;


import com.example.stockanalyzer.model.Candle;
import com.example.stockanalyzer.model.NewsArticle;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public interface StockService {
    String getStockByTicker(String ticker);
    Double getPriceStock(String figi);
    List<Candle> getCandleByFigi(String figi); //
    List<Double> getLineRegression(String figi); // Линейная ригрессия
    List<NewsArticle> getNewsFromGoogle(String companyName) throws IOException, ParseException;
}
