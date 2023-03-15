package com.example.stockanalyzer;

import com.opencsv.CSVReader;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;


public class Main2 {
    public static void main(String[] args) throws Exception {

        CSVReader reader;

        List<Date> dates = new ArrayList<>();
        List<String> news = new ArrayList<>();
        String csvFile = "src/main/resources/data/news.csv";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            reader = new CSVReader(new FileReader(csvFile));
            String[] line;

            while ((line = reader.readNext()) != null) {
                Date date = dateFormat.parse(line[0]);
                dates.add(date);

                String text = line[1];
                news.add(text);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        for (int i = 0; i <news.size(); i++) {
            Annotation annotation = pipeline.process(news.get(i));

            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                String sentiment = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
                int sentimentValue = RNNCoreAnnotations.getPredictedClass(sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class));

                String sentimentRating;
                if (sentimentValue <= 1) {
                    sentimentRating = "very negative";
                } else if (sentimentValue == 2) {
                    sentimentRating = "negative";
                } else if (sentimentValue == 3) {
                    sentimentRating = "neutral";
                } else if (sentimentValue == 4) {
                    sentimentRating = "positive";
                } else {
                    sentimentRating = "very positive";
                }

                System.out.println("Sentiment: " + sentiment + " (" + sentimentRating + ")");
            }
        }
    }
}