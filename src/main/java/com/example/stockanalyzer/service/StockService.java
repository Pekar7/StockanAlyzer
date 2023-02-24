package com.example.stockanalyzer.service;


import com.example.stockanalyzer.model.Candle;

import java.util.List;

public interface StockService {
    String getStockByTicker(String ticker);
    List<Candle> getCandleByFigi(String figi) throws Exception;
}
