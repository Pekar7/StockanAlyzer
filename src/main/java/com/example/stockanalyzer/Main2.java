package com.example.stockanalyzer;

import com.opencsv.CSVReader;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

//Черновик для тональности новости
public class Main2 {
    public static void main(String[] args) throws Exception {

//        CSVReader reader;
//        List<LocalDateTime> dates = new ArrayList<>();
//        List<String> news = new ArrayList<>();
//        String csvFile = "src/main/resources/data/news.csv";
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
//        try {
//            reader = new CSVReader(new FileReader(csvFile));
//            String[] line;
//
//            while ((line = reader.readNext()) != null) {
//                String dateString = line[0].replace("\"", "");
//                LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
//                dates.add(dateTime);
//
//                String text = line[1];
//                news.add(text);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        Properties props = new Properties();
//        props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
//        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
//
//        for (int i = 0; i <news.size(); i++) {
//            Annotation annotation = pipeline.process(news.get(i));
//
//            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
//                String sentiment = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
//                int sentimentValue = RNNCoreAnnotations.getPredictedClass(sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class));
//
//                String sentimentRating;
//                if (sentimentValue <= 1) {
//                    sentimentRating = "very negative";
//                } else if (sentimentValue == 2) {
//                    sentimentRating = "negative";
//                } else if (sentimentValue == 3) {
//                    sentimentRating = "neutral";
//                } else if (sentimentValue == 4) {
//                    sentimentRating = "positive";
//                } else {
//                    sentimentRating = "very positive";
//                }
//
//                System.out.println("Дата: " + dates.get(i) + " Sentiment: " + sentiment + " (" + sentimentRating + ")");
//            }
//        }
        String API_KEY = "a776234139b642f2af8f611731f96073";
        String API_URL = "https://newsapi.org/v2/top-headlines?sources=google-news&apiKey=" + API_KEY;

        try {
            URL url = new URL("https://newsapi.org/v2/top-headlines?sources=google-news&apiKey=" + API_KEY);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();

            JSONObject response = new JSONObject(content.toString());
            JSONArray articles = response.getJSONArray("articles");

            for (int i = 0; i < articles.length(); i++) {
                JSONObject article = articles.getJSONObject(i);
                String title = article.getString("title");
                String description = article.getString("description");
                String urlToArticle = article.getString("url");
                System.out.println(title + " - " + description + " (" + urlToArticle + ")");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}