package com.example.stockanalyzer.service;


import com.example.stockanalyzer.model.Candle;

import java.io.IOException;
import java.util.List;

public interface StockService {
    String getStockByTicker(String ticker);
    Double getPriceStock(String figi);
    List<Candle> getCandleByFigi(String figi);
    Double getAnalysisBrown();
    String getNews(String newUrl) throws IOException;
}
