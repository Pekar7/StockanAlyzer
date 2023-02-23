package com.example.stockanalyzer.controller;

import com.example.stockanalyzer.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock")
public class StockController {

    private final StockService stockService;

    @Autowired
    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @Operation(summary = "Get Stock Information by Ticker", tags = "Ticker")
    @GetMapping("getStock/{ticker}")
    public String getStock(@PathVariable String ticker) {
        return stockService.getStockByTicker(ticker);
    }
}
