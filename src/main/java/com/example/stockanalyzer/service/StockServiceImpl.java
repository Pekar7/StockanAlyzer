package com.example.stockanalyzer.service;

import com.example.stockanalyzer.model.Candle;
import com.example.stockanalyzer.model.NewsArticle;
import com.example.stockanalyzer.model.SentimetModel;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.util.Properties;

import org.apache.commons.math3.stat.regression.SimpleRegression;
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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class StockServiceImpl implements StockService {

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
        if ("Apple".equals(instrument.getName())) {
            return "\uD83C\uDF4F <b>Компания: " + instrument.getName() + "\n\uD83D\uDCB0 Цена: " + price + " $</b>\n";
        } else if ("Amazon.com".equals(instrument.getName())) {
            return "\uD83D\uDECD <b>Компания: " + instrument.getName() + "\n\uD83D\uDCB0 Цена: " + price + " $</b>\n";
        } else if ("Tesla Motors".equals(instrument.getName())) {
            return "\uD83D\uDE98 <b>Компания: " + instrument.getName() + "\n\uD83D\uDCB0 Цена: " + price + " $</b>\n";
        } else {
            return "\uD83E\uDD16 <b>Компания: " + instrument.getName() + "\n\uD83D\uDCB0 Цена: " + price + " $</b>\n";
        }
    }

    //Google News
    @Override
    public List<NewsArticle> getNewsFromGoogle(String companyName) throws IOException {

        String apiKey = "f50e5516-d276-4a30-9515-55bf5a10b4ca";
        String fromDate = LocalDate.now().minusMonths(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String toDate = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        int page = 1;
        int pageSize = 200;
        boolean hasMore = true;
        List<NewsArticle> newsListGuardian = new ArrayList<>();
        while (hasMore) {
            String urlStr = "https://content.guardianapis.com/search?q=" + companyName +"&section=technology|comppany"+ "&from-date=" + fromDate + "&to-date=" + toDate + "&page=" + page + "&page-size=" + pageSize + "&api-key=" + apiKey;

            try {
                URL url = new URL(urlStr);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Обработка ответа
                String json = response.toString();
                JSONObject obj = new JSONObject(json);
                JSONArray results = obj.getJSONObject("response").getJSONArray("results");


                // Выводим заголовки и ссылки на новости
                for (int i = 0; i < results.length(); i++) {
                    JSONObject article = results.getJSONObject(i);
                    String title = article.getString("webTitle");
                    String urlLink = article.getString("webUrl");
                    String dateParse = article.getString("webPublicationDate");
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    LocalDateTime dateTime = LocalDateTime.parse(dateParse, formatter);
                    newsListGuardian.add(new NewsArticle(title, title, dateTime, urlLink));
                }

                // Проверяем, есть ли еще результаты
                int currentPage = obj.getJSONObject("response").getInt("currentPage");
                int pages = obj.getJSONObject("response").getInt("pages");
                hasMore = currentPage < pages;
                page++;

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        String urlStr = "https://newsapi.org/v2/everything?q=" + companyName + "%20&apiKey=" + apiKeyGoogle;

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        int responseCode = conn.getResponseCode();

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

            List<NewsArticle> newsListGoogle = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject articleObj = jsonArray.getJSONObject(i);
                String title = articleObj.getString("title");
                String description = articleObj.getString("description");
                String urlNews = articleObj.getString("url");
                String dateTimeStr = articleObj.getString("publishedAt");
                LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);

                Pattern pattern = Pattern.compile("\\b" + companyName + "\\b");
                Matcher matcher = pattern.matcher(title);

                if (matcher.find()) {
                    newsListGoogle.add(new NewsArticle(title, description, dateTime, urlNews));
                }
            }

            Comparator<NewsArticle> dateComparator = new Comparator<NewsArticle>() {
                @Override
                public int compare(NewsArticle o1, NewsArticle o2) {
                    return o1.getDate().compareTo(o2.getDate());
                }
            };

            newsListGoogle.sort(dateComparator);
            newsListGuardian.sort(dateComparator);

            List<NewsArticle> newsList = new ArrayList<>();
            newsList.addAll(newsListGuardian);
            newsList.addAll(newsListGoogle);

            List<String[]> data = new ArrayList<String[]>();
            for (int i = 0; i < newsList.size(); i++) {
                if (String.valueOf(newsList.get(i).getDate()).length() == 16) {
                    data.add(new String[]{
                            newsList.get(i).getDate() + ":00",
                            String.valueOf(newsList.get(i).getDescription()),
                    });
                } else {
                    data.add(new String[]{
                            String.valueOf(newsList.get(i).getDate()),
                            String.valueOf(newsList.get(i).getDescription()),
                    });
                }
            }

            File file = new File("src/main/resources/data/news.csv");
            try {
                FileWriter outputfile = new FileWriter(file);
                CSVWriter writer = new CSVWriter(outputfile);
                writer.writeAll(data);
                writer.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return newsList;
        }
    }

    //Анализ новостей
    public static List<SentimetModel> getSentimentAnalysis() {
        CSVReader reader;
        List<LocalDateTime> dates = new ArrayList<>();
        List<String> news = new ArrayList<>();
        String csvFile = "src/main/resources/data/news.csv";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        try {
            reader = new CSVReader(new FileReader(csvFile));
            String[] line;

            while ((line = reader.readNext()) != null) {
                String dateString = line[0].replace("\"", "");
                LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
                dates.add(dateTime);

                String text = line[1];
                news.add(text);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        List<SentimetModel> sentimetList = new ArrayList<>();
        List<String[]> data = new ArrayList<String[]>();
        for (int i = 0; i < news.size(); i++) {
            Annotation annotation = pipeline.process(news.get(i));

            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                String sentiment = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
                int sentimentValue = RNNCoreAnnotations.getPredictedClass(sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class));

                if (String.valueOf(dates.get(i)).length() == 16) {
                    data.add(new String[]{
                            dates.get(i) + ":00",
                            sentiment,
                            String.valueOf(sentimentValue)
                    });
                } else {
                    data.add(new String[]{
                            String.valueOf(dates.get(i)),
                            sentiment,
                            String.valueOf(sentimentValue)
                    });
                }

                sentimetList.add(new SentimetModel(dates.get(i), sentiment, sentimentValue));
            }
        }

        File file = new File("src/main/resources/data/newsSentiment.csv");
        try {
            FileWriter outputfile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputfile);
            writer.writeAll(data);
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return sentimetList;
    }

    //Получить свечи по figi
    @Override
    public List<Candle> getCandleByFigi(String figi) {
//        getSentimentAnalysis();

        LocalDateTime localDateTime5 = LocalDateTime.now().minusDays(35);
        Instant now5 = localDateTime5.atZone(ZoneId.systemDefault()).toInstant();
        LocalDateTime months5 = localDateTime5.minusDays(7);
        Instant instant5 = months5.atZone(ZoneId.systemDefault()).toInstant();
        List<HistoricCandle> historicCandleList5 = api.getMarketDataService().getCandles(figi, instant5, now5, CandleInterval.CANDLE_INTERVAL_HOUR).join(); //месяц

        LocalDateTime localDateTime4 = LocalDateTime.now().minusDays(28);
        Instant now4 = localDateTime4.atZone(ZoneId.systemDefault()).toInstant();
        LocalDateTime months4 = localDateTime4.minusDays(7);
        Instant instant4 = months4.atZone(ZoneId.systemDefault()).toInstant();
        List<HistoricCandle> historicCandleList4 = api.getMarketDataService().getCandles(figi, instant4, now4, CandleInterval.CANDLE_INTERVAL_HOUR).join(); //месяц


        LocalDateTime localDateTime3 = LocalDateTime.now().minusDays(21);
        Instant now3 = localDateTime3.atZone(ZoneId.systemDefault()).toInstant();
        LocalDateTime months3 = localDateTime3.minusDays(7);
        Instant instant3 = months3.atZone(ZoneId.systemDefault()).toInstant();
        List<HistoricCandle> historicCandleList3 = api.getMarketDataService().getCandles(figi, instant3, now3, CandleInterval.CANDLE_INTERVAL_HOUR).join(); //месяц


        LocalDateTime localDateTime2 = LocalDateTime.now().minusDays(14);
        Instant now2 = localDateTime2.atZone(ZoneId.systemDefault()).toInstant();
        LocalDateTime months2 = localDateTime2.minusDays(7);
        Instant instant2 = months2.atZone(ZoneId.systemDefault()).toInstant();
        List<HistoricCandle> historicCandleList2 = api.getMarketDataService().getCandles(figi, instant2, now2, CandleInterval.CANDLE_INTERVAL_HOUR).join(); //месяц


        LocalDateTime localDateTime1 = LocalDateTime.now().minusDays(7);
        Instant now1 = localDateTime1.atZone(ZoneId.systemDefault()).toInstant();
        LocalDateTime months1 = localDateTime1.minusDays(7);
        Instant instant1 = months1.atZone(ZoneId.systemDefault()).toInstant();
        List<HistoricCandle> historicCandleList1 = api.getMarketDataService().getCandles(figi, instant1, now1, CandleInterval.CANDLE_INTERVAL_HOUR).join(); //месяц


        Instant now = Instant.now();
        LocalDateTime oneMonthsAgo = LocalDateTime.now().minusDays(7);
        Instant instantOneMonthsAgo = oneMonthsAgo.atZone(ZoneId.systemDefault()).toInstant();
        List<HistoricCandle> historicCandleListNow = api.getMarketDataService().getCandles(figi, instantOneMonthsAgo, now, CandleInterval.CANDLE_INTERVAL_HOUR).join(); //месяц

        List<HistoricCandle> allHistoricCandle = new ArrayList<>();
        allHistoricCandle.addAll(historicCandleList5);
        allHistoricCandle.addAll(historicCandleList4);
        allHistoricCandle.addAll(historicCandleList3);
        allHistoricCandle.addAll(historicCandleList2);
        allHistoricCandle.addAll(historicCandleList1);
        allHistoricCandle.addAll(historicCandleListNow);

        List<Candle> candles = new ArrayList();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < allHistoricCandle.size(); i++) {
            Instant instant = Instant.ofEpochSecond(allHistoricCandle.get(i).getTime().getSeconds());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            candles.add(
                    new Candle(
                            formatter.format(localDateTime),
                            allHistoricCandle.get(i).getClose().getUnits() + allHistoricCandle.get(i).getClose().getNano() / Math.pow(10, 9),
                            allHistoricCandle.get(i).getVolume(),
                            allHistoricCandle.get(i).getOpen().getUnits() + allHistoricCandle.get(i).getClose().getNano() / Math.pow(10, 9),
                            allHistoricCandle.get(i).getHigh().getUnits() + allHistoricCandle.get(i).getHigh().getNano() / Math.pow(10, 9),
                            allHistoricCandle.get(i).getLow().getUnits() + allHistoricCandle.get(i).getLow().getNano() / Math.pow(10, 9)
                    ));
        }

        List<String[]> data = new ArrayList<String[]>();
        for (int i = 0; i < candles.size(); i++) {
            data.add(new String[]{
                    String.valueOf(candles.get(i).getSimpleDateFormat()),
                    String.valueOf(candles.get(i).getOpen()),
                    String.valueOf(candles.get(i).getHigh()),
                    String.valueOf(candles.get(i).getLow()),
                    String.valueOf(candles.get(i).getClose()),
                    String.valueOf(candles.get(i).getValue())
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

    @Override
    public List<Double> getLineRegression(String figi) {

        Instant now = Instant.now();
        LocalDateTime months = LocalDateTime.now().minusMonths(6);
        Instant from = months.atZone(ZoneId.systemDefault()).toInstant();
        List<HistoricCandle> historicCandleList = api.getMarketDataService().getCandles(figi, from, now, CandleInterval.CANDLE_INTERVAL_DAY).join();

        List<Double> prices = new ArrayList<>();
        List<Long> timestamps = new ArrayList<>();

        for (int i = 0; i < historicCandleList.size(); i++) {
            Instant instant = Instant.ofEpochSecond(historicCandleList.get(i).getTime().getSeconds());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

            timestamps.add(localDateTime.toEpochSecond(ZoneOffset.UTC));
            prices.add(historicCandleList.get(i).getClose().getUnits() + historicCandleList.get(i).getClose().getNano() / Math.pow(10, 9));
        }

        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < prices.size(); i++) {
            regression.addData(timestamps.get(i), prices.get(i));
        }

        System.out.println("Slope: " + regression.getSlope());
        System.out.println("Intercept: " + regression.getIntercept());
        System.out.println("R-squared: " + regression.getRSquare());

        long nextMonthTimestamp = now.plus(Duration.ofDays(1)).toEpochMilli();
        double nextMonthPrice = regression.predict(nextMonthTimestamp);
        System.out.println("Predicted price for next month: " + nextMonthPrice);

        List<Double> params = new ArrayList<>();
        params.add(regression.getSlope());
        params.add(regression.getIntercept());
        params.add(regression.getRSquare());
        params.add(nextMonthPrice);
        return params;
    }
}