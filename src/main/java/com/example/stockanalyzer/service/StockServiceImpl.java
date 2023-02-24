package com.example.stockanalyzer.service;

import com.example.stockanalyzer.model.Candle;
import com.opencsv.CSVWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.Instrument;
import ru.tinkoff.piapi.core.InvestApi;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service
public class StockServiceImpl implements StockService {

    private final InvestApi api;

    @Autowired
    public StockServiceImpl(InvestApi api) {
        this.api = api;
    }

    @Override
    public String getStockByTicker(String ticker) {
        Instrument instrument = api.getInstrumentsService().getInstrumentByFigi(ticker).join();
        instrument.getName();
        return "Hello it ";
    }

    @Override
    public List<Candle> getCandleByFigi(String figi) {
        Instant now = Instant.now();
        LocalDateTime fiveMonthsAgo = LocalDateTime.now().minusMonths(5);
        Instant instantFiveMonthsAgo = fiveMonthsAgo.atZone(ZoneId.systemDefault()).toInstant();
        List<HistoricCandle> historicCandleList = api.getMarketDataService().getCandles(figi, instantFiveMonthsAgo, now, CandleInterval.CANDLE_INTERVAL_DAY).join();

        List<Candle> candles = new ArrayList();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < historicCandleList.size(); i++)
        {
            candles.add(
                    new Candle(historicCandleList.get(i).getClose().getUnits() + historicCandleList.get(i).getClose().getNano()/Math.pow(10, 9),
                                historicCandleList.get(i).getHigh().getUnits() + historicCandleList.get(i).getHigh().getNano()/Math.pow(10, 9),
                                historicCandleList.get(i).getLow().getUnits() + historicCandleList.get(i).getLow().getNano()/Math.pow(10, 9),
                               historicCandleList.get(i).getClose().getUnits() + historicCandleList.get(i).getClose().getNano()/Math.pow(10, 9),
                                    simpleDateFormat.format(new Date(historicCandleList.get(i).getTime().getSeconds() * 1000))
                            ));
        }

        List<String[]> data = new ArrayList<String[]>();
        for (int i = 0; i < candles.size(); i++) {
            data.add(new String[]{
                    String.valueOf(candles.get(i).getOpen()),
                    String.valueOf(candles.get(i).getHigh()),
                    String.valueOf(candles.get(i).getLow()),
                    String.valueOf(candles.get(i).getClose())
            });
        }
        File file = new File("src/main/resources/data/candle.csv");
        try {
            FileWriter outputfile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputfile);
            writer.writeAll(data);
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return candles;
    }
}
