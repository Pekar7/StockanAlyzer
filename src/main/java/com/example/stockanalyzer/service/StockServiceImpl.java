package com.example.stockanalyzer.service;

import com.example.stockanalyzer.model.Candle;
import com.example.stockanalyzer.model.NewsArticle;
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
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class StockServiceImpl implements StockService {

    private static final long CANDLE_PERIOD = 6;
    @Value("${news.token}")
    String apiKey;

    @Value("${news.tokenGoogle}")
    String apiKeyGoogle;

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
        LocalDateTime fiveMonthsAgo = LocalDateTime.now().minusMonths(CANDLE_PERIOD);
        Instant instantFiveMonthsAgo = fiveMonthsAgo.atZone(ZoneId.systemDefault()).toInstant();
        List<HistoricCandle> historicCandleList = api.getMarketDataService().getCandles(figi, instantFiveMonthsAgo, now, CandleInterval.CANDLE_INTERVAL_DAY).join(); //месяц

        List<Candle> candles = new ArrayList();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < historicCandleList.size(); i++) {
            candles.add(
                    new Candle(
                            simpleDateFormat.format(new Date(historicCandleList.get(i).getTime().getSeconds() * 1000)),
                            historicCandleList.get(i).getClose().getUnits() + historicCandleList.get(i).getClose().getNano() / Math.pow(10, 9),
                            historicCandleList.get(i).getVolume(),
                            historicCandleList.get(i).getOpen().getUnits() + historicCandleList.get(i).getClose().getNano() / Math.pow(10, 9),
                            historicCandleList.get(i).getHigh().getUnits() + historicCandleList.get(i).getHigh().getNano() / Math.pow(10, 9),
                            historicCandleList.get(i).getLow().getUnits() + historicCandleList.get(i).getLow().getNano() / Math.pow(10, 9)
                    ));
        }

        List<String[]> data = new ArrayList<String[]>();
        for (int i = 0; i < candles.size(); i++) {
            data.add(new String[]{
                    String.valueOf(candles.get(i).getSimpleDateFormat()),
                    String.valueOf(candles.get(i).getClose()),
                    String.valueOf(candles.get(i).getValue()),
                    String.valueOf(candles.get(i).getOpen()),
                    String.valueOf(candles.get(i).getHigh()),
                    String.valueOf(candles.get(i).getLow()),
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

    //Google News
    @Override
    public String getNewsFromGoogle(String companyName) throws IOException {
        String urlStr = "https://newsapi.org/v2/everything?q=" + companyName + "%20&apiKey=" + apiKeyGoogle;

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        int responseCode = conn.getResponseCode();

        List<String[]> data = new ArrayList<String[]>();

        if (responseCode != 200) {
            throw new RuntimeException("HttpResponseCode: " + responseCode);
        } else {
            String jsonString = "";
            Scanner scanner = new Scanner(url.openStream());
            while (scanner.hasNext()) {
                jsonString += scanner.nextLine();
            }
            scanner.close();
            JSONObject jsonObj = new JSONObject(jsonString);
            JSONArray jsonArray = jsonObj.getJSONArray("articles");

            List<NewsArticle> newsList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject articleObj = jsonArray.getJSONObject(i);
                String title = articleObj.getString("title");
                String description = articleObj.getString("description");
                String urlNews = articleObj.getString("url");
                String dateStr = articleObj.getString("publishedAt").substring(0, 10);
                LocalDate date = LocalDate.parse(dateStr);

                Pattern pattern = Pattern.compile("\\b"+companyName+"\\b");
                Matcher matcher = pattern.matcher(title);
                Matcher matcher2 = pattern.matcher(description);


                if (matcher.find() || matcher2.find()) {
                    newsList.add(new NewsArticle(title, description, date, urlNews));
                }
            }

            Map<LocalDate, List<String>> articlesByDate = new HashMap<>();
            for (NewsArticle article : newsList) {
                articlesByDate.computeIfAbsent(article.getDate(), k -> new ArrayList<>()).add(article.getTitle());
            }

            List<NewsArticle> sortedArticles = new ArrayList<>();
            for (Map.Entry<LocalDate, List<String>> entry : articlesByDate.entrySet()) {
                LocalDate date = entry.getKey();
                List<String> titles = entry.getValue();
                String title = String.join(", ", titles);
                NewsArticle article = new NewsArticle();
                article.setDate(date);
                article.setTitle(title);
                sortedArticles.add(article);
            }

            sortedArticles.sort(Comparator.comparing(NewsArticle::getDate).reversed());

            for (int i = 0; i < sortedArticles.size(); i++) {
                String[] news = new String[]{String.valueOf(sortedArticles.get(i).getDate()), sortedArticles.get(i).getTitle(), sortedArticles.get(i).getDescription(), sortedArticles.get(i).getUrlNews()};
                data.add(news);
            }

            try {
                CSVWriter writer = new CSVWriter(new FileWriter("src/main/resources/data/newsGoogle.csv"));
                for (String[] news : data) {
                    writer.writeNext(news);
                }
                writer.close();
            } catch (Exception e) {
                System.out.println("Ошибка записи данных: " + e.getMessage());
            }
        }
        String[] newestNews = data.get(data.size()-1);
        return "Свежая новость на:" + newestNews[0] + "\n\nНовость: \n" + newestNews[1] + "\n\nСсылка на новость: \n" + newestNews[3];
    }

}

/*
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
                Date date = dateFormat.parse(line[0]);
                dates.add(date);
                double value = Double.parseDouble(line[1]);
                values.add(value);
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

    //The Guardian
    @Override
    public String getNewsFromGuardian(String newUrl) throws IOException {
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
            CSVWriter writer = new CSVWriter(new FileWriter("src/main/resources/data/newsGuardian.csv"));
            for (String[] news : data) {
                writer.writeNext(news);
            }
            writer.close();
        } catch (Exception e) {
            System.out.println("Ошибка записи данных: " + e.getMessage());
        }

        String[] newestNews = data.get(results.length() - 1);
        return "Свежая новость: \nДата: \n" + newestNews[0] + "\nНовость: \n" + newestNews[1] + "\nСсылка на новость: \n" + newestNews[2];
    }
 */