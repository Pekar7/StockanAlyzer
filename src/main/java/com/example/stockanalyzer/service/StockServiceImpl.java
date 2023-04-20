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

import lombok.SneakyThrows;
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
import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class StockServiceImpl implements StockService {

    private static final long CANDLE_PERIOD = 1;
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
    public List<NewsArticle> getNewsFromGoogle(String companyName) throws IOException, ParseException {
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

            List<NewsArticle> newsList = new ArrayList<>();
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
                    newsList.add(new NewsArticle(title, description, dateTime, urlNews));
                }
            }

            Comparator<NewsArticle> dateComparator = new Comparator<NewsArticle>() {
                @Override
                public int compare(NewsArticle o1, NewsArticle o2) {
                    return o1.getDate().compareTo(o2.getDate());
                }
            };

            newsList.sort(dateComparator);

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

//                data.add(new String[]{
//                        String.valueOf(dates.get(i)),
//                        String.valueOf(sentiment),
//                        String.valueOf(sentimentValue)
//                });

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
        List<SentimetModel> sentimentAnalysis = getSentimentAnalysis();

        Instant now = Instant.now();
        LocalDateTime fiveMonthsAgo = LocalDateTime.now().minusMonths(CANDLE_PERIOD);
        Instant instantFiveMonthsAgo = fiveMonthsAgo.atZone(ZoneId.systemDefault()).toInstant();
        List<HistoricCandle> historicCandleList = api.getMarketDataService().getCandles(figi, instantFiveMonthsAgo, now, CandleInterval.CANDLE_INTERVAL_4_HOUR).join(); //месяц

        List<Candle> candles = new ArrayList();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < historicCandleList.size(); i++) {
            Instant instant = Instant.ofEpochSecond(historicCandleList.get(i).getTime().getSeconds());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            candles.add(
                    new Candle(
                            formatter.format(localDateTime),
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
}