package com.example.stockanalyzer;

import com.opencsv.CSVWriter;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        var instrumentFigi = "BBG000N9MNX3";

//        var token = "t.PhcEc86HoApW_TIr1E_DqfUB4W1ttPNyN-O1Z0ck_CkcwPIUTLpcu0ifBXAk_7AsZ3TevIeAL1dr8ayytbfHBg";
        var token = "t.r1RjGdULtS1QIwg-k30CmN6RQA65yLseFYIckRXHgyQupL6Vcs0lvGLDsvhs1V3mGTDhKOJxwJ6HxbDyuygcuQ";
        var api = InvestApi.create(token);
        var a = api.getInstrumentsService().getInstrumentByFigi("BBG000N9MNX3");
        Instrument instrument = api.getInstrumentsService().getInstrumentByFigi(instrumentFigi).join();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println(instrument.getName() + " " + instrument.getTicker() + " " + instrument.getMinPriceIncrement().getUnits() + " " + instrument.getMinPriceIncrement().getNano() + " " +instrument.getName());

        Instant now = Instant.now();
        System.out.println("Текущее время: " + now);

        LocalDateTime fiveMonthsAgo = LocalDateTime.now().minusMonths(5);
        Instant instantFiveMonthsAgo = fiveMonthsAgo.atZone(ZoneId.systemDefault()).toInstant();
        CompletableFuture<List<HistoricCandle>> sa = api.getMarketDataService().getCandles("BBG000B9XRY4", instantFiveMonthsAgo, now, CandleInterval.CANDLE_INTERVAL_DAY);


        List<String[]> data = new ArrayList<String[]>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sa.join().forEach(in -> System.out.println( "\n{" +in.getOpen().getUnits() + " " + in.getHigh().getUnits()
                +  " " + in.getLow().getUnits() + " " + in.getClose().getUnits() +" " + in.getVolume() + " " + sdf.format(new Date(in.getTime().getSeconds() * 1000))+ "}"));


    }
}

// TSLA ->  BBG000N9MNX3
// AAPL -> BBG000B9XRY4
// t.r1RjGdULtS1QIwg-k30CmN6RQA65yLseFYIckRXHgyQupL6Vcs0lvGLDsvhs1V3mGTDhKOJxwJ6HxbDyuygcuQ