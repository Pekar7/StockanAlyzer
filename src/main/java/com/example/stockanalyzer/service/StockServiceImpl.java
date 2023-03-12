package com.example.stockanalyzer.service;

import com.example.stockanalyzer.model.Candle;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
public class StockServiceImpl implements StockService {

    @Value("${news.token}")
    String apiKey;

    private final InvestApi api;

    @Autowired
    public StockServiceImpl(InvestApi api) {
        this.api = api;
    }

    public Double getPriceStock(String figi) {
        Quotation instrumentPrice = api.getMarketDataService().getOrderBook(figi, 1).join().getLastPrice();
        return instrumentPrice.getUnits() + instrumentPrice.getNano() / Math.pow(10, 9);
    }

    @Override
    public String getStockByTicker(String figi) {
        Instrument instrument = api.getInstrumentsService().getInstrumentByFigi(figi).join();
        Double price = getPriceStock(figi);
        return "Акция компании: " + instrument.getName() + "\nТикер: " + instrument.getTicker() + " Figi: " + instrument.getFigi()
                + "\nЦена акции: " + price
                + " " + instrument.getCurrency() + "\n";

    }

    //Получить свечи по figi
    @Override
    public List<Candle> getCandleByFigi(String figi) {
        Instant now = Instant.now();
        LocalDateTime fiveMonthsAgo = LocalDateTime.now().minusMonths(24);
        Instant instantFiveMonthsAgo = fiveMonthsAgo.atZone(ZoneId.systemDefault()).toInstant();
        List<HistoricCandle> historicCandleList = api.getMarketDataService().getCandles(figi, instantFiveMonthsAgo, now, CandleInterval.CANDLE_INTERVAL_MONTH).join(); //месяц

        List<Candle> candles = new ArrayList();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < historicCandleList.size(); i++) {
            candles.add(
                    new Candle(historicCandleList.get(i).getClose().getUnits() + historicCandleList.get(i).getClose().getNano() / Math.pow(10, 9),
                            historicCandleList.get(i).getHigh().getUnits() + historicCandleList.get(i).getHigh().getNano() / Math.pow(10, 9),
                            historicCandleList.get(i).getLow().getUnits() + historicCandleList.get(i).getLow().getNano() / Math.pow(10, 9),
                            historicCandleList.get(i).getClose().getUnits() + historicCandleList.get(i).getClose().getNano() / Math.pow(10, 9),
                            simpleDateFormat.format(new Date(historicCandleList.get(i).getTime().getSeconds() * 1000))
                    ));
        }

        List<String[]> data = new ArrayList<String[]>();
        for (int i = 0; i < candles.size(); i++) {
            data.add(new String[]{
                    String.valueOf(candles.get(i).getClose()),
                    String.valueOf(candles.get(i).getSimpleDateFormat())
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

    // Модель Брауна
    @Override
    public Double getAnalysisBrown() {
        String csvFile = "src/main/resources/data/candle.csv";
        CSVReader reader;
        List<Double> values = new ArrayList<>();
        List<Date> dates = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            reader = new CSVReader(new FileReader(csvFile));
            String[] line;

            while ((line = reader.readNext()) != null) {
                double value = Double.parseDouble(line[0]);
                values.add(value);
                Date date = dateFormat.parse(line[1]);
                dates.add(date);
//                System.out.println(line[0] + " " + line[1]);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // задание параметров модели Брауна
        double alpha = 0.2;
        double initialForecast = values.get(0);
        double[] forecasts = new double[values.size()];

        // вычисление прогнозов на основе модели Брауна
        for (int i = 0; i < values.size(); i++) {
            if (i == 0) {
                forecasts[i] = initialForecast;
            } else {
                double value = values.get(i);
                double forecast = alpha * value + (1 - alpha) * forecasts[i - 1];
                forecasts[i] = forecast;
            }
        }


        List<String[]> data = new ArrayList<String[]>();
        for (int i = 0; i < values.size(); i++) {
            data.add(new String[]{
                    dateFormat.format(dates.get(i)),
                    String.valueOf(values.get(i)),
                    String.valueOf(forecasts[i]),
            });
        }

        LocalDate tomorrow = LocalDate.now().plusMonths(1); //добавление след периода
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedTomorrow = tomorrow.format(formatter);

        // прогноз на следующий период
        double nextForecast = alpha * values.get(values.size() - 1) + (1 - alpha) * forecasts[forecasts.length - 1];
        data.add(new String[]{formattedTomorrow, null, String.valueOf(nextForecast)});

        File file = new File("src/main/resources/data/methodBrown.csv");
        try {
            FileWriter outputfile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputfile);
            writer.writeAll(data);
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return nextForecast;
    }


    @Override
    public String getNews(String newUrl) throws IOException {
        URL obj = new URL(newUrl + apiKey);
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
        return "Свежая новость: \nДата: \n" + newestNews[0] + "\nНовость: \n" + newestNews[1] + "\nСсылка на новость: \n" + newestNews[2];
    }
}
