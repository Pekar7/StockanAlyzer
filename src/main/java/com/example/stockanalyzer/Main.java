package com.example.stockanalyzer;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

//        var instrumentFigi = "BBG000N9MNX3";
//
////        var token = "t.PhcEc86HoApW_TIr1E_DqfUB4W1ttPNyN-O1Z0ck_CkcwPIUTLpcu0ifBXAk_7AsZ3TevIeAL1dr8ayytbfHBg";
//        var token = "t.r1RjGdULtS1QIwg-k30CmN6RQA65yLseFYIckRXHgyQupL6Vcs0lvGLDsvhs1V3mGTDhKOJxwJ6HxbDyuygcuQ";
//        var api = InvestApi.create(token);
//        var a = api.getInstrumentsService().getInstrumentByFigi("BBG000N9MNX3");
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println(a.get().getName()+ " " + a.get().getTicker());
//
//        Account id = api.getUserService().getAccounts().get().get(0);
//        var b = api.getOperationsService().getPortfolio(id.getId()).get();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//
//        Instrument instrument = api.getInstrumentsService().getInstrumentByFigi(instrumentFigi).join();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println(instrument.getName() + " " + instrument.getTicker() + " " + instrument.getMinPriceIncrement().getUnits() + " " + instrument.getMinPriceIncrement().getNano() + " " +instrument.getName());
//
//        Instant now = Instant.now();
//        System.out.println("Текущее время: " + now);
//
//        LocalDateTime fiveMonthsAgo = LocalDateTime.now().minusMonths(5);
//        Instant instantFiveMonthsAgo = fiveMonthsAgo.atZone(ZoneId.systemDefault()).toInstant();
//        CompletableFuture<List<HistoricCandle>> sa = api.getMarketDataService().getCandles("BBG000B9XRY4", instantFiveMonthsAgo, now, CandleInterval.CANDLE_INTERVAL_DAY);
//
//        var context = api.getMarketDataService().getOrderBook("BBG000B9XRY4", 1).join();
//        double sum = 0;
//        sum = context.getLastPrice().getUnits() + context.getLastPrice().getNano()/Math.pow(10, 9);
//        System.out.println(sum);


//
//        String csvFile = "src/main/resources/data/candle.csv";
//        CSVReader reader;
//        List<Double> values = new ArrayList<>();
//        List<Date> dates = new ArrayList<>();
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//
//        try {
//            reader = new CSVReader(new FileReader(csvFile));
//            String[] line;
//
//            while ((line = reader.readNext()) != null) {
//                double value = Double.parseDouble(line[0]);
//                values.add(value);
//                Date date = dateFormat.parse(line[1]);
//                dates.add(date);
//                System.out.println(line[0] + " " + line[1]); // Предполагаем, что в файле первый столбец - числа, а второй - даты
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        // задание параметров модели Брауна
//        double alpha = 0.5;
//        double initialForecast = values.get(0);
//        double[] forecasts = new double[values.size()];
//
//        // вычисление прогнозов на основе модели Брауна
//        for (int i = 0; i < values.size(); i++) {
//            if (i == 0) {
//                forecasts[i] = initialForecast;
//            } else {
//                double value = values.get(i);
//                double forecast = alpha * value + (1 - alpha) * forecasts[i-1];
//                forecasts[i] = forecast;
//            }
//        }
//
//        // вывод результатов прогнозирования
//        System.out.println("Date\t\tActual\t\tForecast");
//        for (int i = 0; i < values.size(); i++) {
//            System.out.println(dateFormat.format(dates.get(i)) + "\t" + values.get(i) + "\t" + forecasts[i]);
//        }
//
//        LocalDate tomorrow = LocalDate.now().plusDays(1);
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        String formattedTomorrow = tomorrow.format(formatter);
//        System.out.println(formattedTomorrow);
//
//        // прогноз на следующий период
//        double nextForecast = alpha * values.get(values.size()-1) + (1 - alpha) * forecasts[forecasts.length-1];
//        System.out.println("Next forecast: " + formattedTomorrow + " " + nextForecast);
//


public class Main {
    public static void main(String[] args) throws IOException {
        String urlToRead = "https://content.guardianapis.com/search?q=Microsoft%20&page-size=20&api-key=f50e5516-d276-4a30-9515-55bf5a10b4ca";

        URL obj = new URL(urlToRead);
        InputStream is = obj.openConnection().getInputStream();

        // Читаем JSON-ответ от The Guardian
        String jsonText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(jsonText);
        JSONArray results = json.getJSONObject("response").getJSONArray("results");

        // Создаем компаратор для сравнения дат
        Comparator<String[]> dateComparator = new Comparator<String[]>() {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

            public int compare(String[] s1, String[] s2) {
                try {
                    Date date1 = format.parse(s1[0]);
                    Date date2 = format.parse(s2[0]);
                    return date1.compareTo(date2);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Неверный формат даты", e);
                }
            }
        };

        List<String[]> data = new ArrayList<String[]>();
        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);
            String date = result.getString("webPublicationDate");
            String title = result.getString("webTitle");
            String urlLink = result.getString("webUrl");
            String[] news = new String[]{date, title, urlLink};
            data.add(news);
        }

        for (String[] strings : data) {
            System.out.println(strings[0] + " " + strings[1] + " " + strings[2]);
        }

        Collections.sort(data, dateComparator);
        try {
            CSVWriter writer = new CSVWriter(new FileWriter("src/main/resources/data/news.csv"));
            for (String[] news : data) {
                writer.writeNext(news);
            }
            writer.close();
        } catch (Exception e) {
            System.out.println("Ошибка записи данных: " + e.getMessage());
        }

        String[] newestNews = data.get(results.length()-1);
        System.out.println("Свежая новость: \nДата: \n" + newestNews[0] + "\nНовость: \n" + newestNews[1] + "\nСсылка на новость: \n" + newestNews[2]);
    }



}

// TSLA ->  BBG000N9MNX3
// AAPL -> BBG000B9XRY4
// t.r1RjGdULtS1QIwg-k30CmN6RQA65yLseFYIckRXHgyQupL6Vcs0lvGLDsvhs1V3mGTDhKOJxwJ6HxbDyuygcuQ