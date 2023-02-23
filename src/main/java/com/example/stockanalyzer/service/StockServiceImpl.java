package com.example.stockanalyzer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.Instrument;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.concurrent.CompletableFuture;


@Service
public class StockServiceImpl implements StockService {

    private final InvestApi api;

    @Autowired
    public StockServiceImpl(InvestApi api) {
        this.api = api;
    }

    @Override
    public String getStockByTicker(String ticker) {
        CompletableFuture<Instrument> instrumentsService = api.getInstrumentsService().getInstrumentByFigi(ticker);
        return "Hello it " + instrumentsService.join().getTicker();
    }
}
