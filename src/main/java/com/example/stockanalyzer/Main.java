package com.example.stockanalyzer;


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


import com.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
//
//
//public class Main {
//    public static void main(String[] args) {
//        CSVReader reader;
//
//        List<Date> dat2es = new ArrayList<>();
//        List<Double> prices = new ArrayList<>();
//        String csvFile = "src/main/resources/data/candle.csv";
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//        try {
//            reader = new CSVReader(new FileReader(csvFile));
//            String[] line;
//
//            while ((line = reader.readNext()) != null) {
//                double value = Double.parseDouble(line[0]);
//                prices.add(value);
//                Date date = dateFormat.parse(line[1]);
//                dates.add(date);
////                System.out.println(line[0] + " " + line[1]);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        SimpleRegression regression = new SimpleRegression();
//        for (int i = 0; i < prices.size(); i++) {
//            regression.addData(i + 1, prices.get(i));
//        }
//
//        // print regression parameters
//        System.out.println("Slope: " + regression.getSlope());
//        System.out.println("Intercept: " + regression.getIntercept());
//        System.out.println("R-squared: " + regression.getRSquare());
//
//        // predict price for next month
//        double nextMonthPrice = regression.predict(prices.size() + 1);
//        System.out.println("Predicted price for next month: " + nextMonthPrice);
//    }
//}

//class Main{
//    public static void main(String[] args) throws IOException {
//        // загрузка данных из CSV файла
//        BufferedReader reader = new BufferedReader(new FileReader("stock_data.csv"));
//        Instances data = new Instances(reader);
//        reader.close();
//
//        // удаление столбца с датами
//        data.deleteAttributeAt(0);
//
//        // создание признака на основе анализа тональности новостей
//        int sentimentIndex = data.numAttributes() - 1;
//        for (int i = 0; i < data.numInstances(); i++) {
//            double sentiment = Double.parseDouble(data.instance(i).stringValue(sentimentIndex));
//            if (sentiment >= 0 && sentiment < 1.5) {
//                data.instance(i).setValue(sentimentIndex, "negative");
//            } else if (sentiment >= 1.5 && sentiment < 3.5) {
//                data.instance(i).setValue(sentimentIndex, "neutral");
//            } else {
//                data.instance(i).setValue(sentimentIndex, "positive");
//            }
//        }
//
//        // разделение на тренировочный и тестовый наборы
//        data.randomize(new Random());
//        int trainSize = (int) Math.round(data.numInstances() * 0.8);
//        int testSize = data.numInstances() - trainSize;
//        Instances trainData = new Instances(data, 0, trainSize);
//        Instances testData = new Instances(data, trainSize, testSize);
//
//        // обучение модели решающих деревьев
//        trainData.setClassIndex(trainData.numAttributes() - 1);
//        J48 decisionTree = new J48();
//        decisionTree.buildClassifier(trainData);
//
//        // вывод результатов
//        System.out.println("Training set size: " + trainData.numInstances());
//        System.out.println("Test set size: " + testData.numInstances());
//        System.out.println("Accuracy: " + evaluateClassifier(decisionTree, testData));
//    }
//
//    public static double evaluateClassifier(J48 classifier, Instances testData) throws Exception {
//        int numCorrect = 0;
//        for (int i = 0; i < testData.numInstances(); i++) {
//            double actualClass = testData.instance(i).classValue();
//            double predictedClass = classifier.classifyInstance(testData.instance(i));
//            if (actualClass == predictedClass) {
//                numCorrect++;
//            }
//        }
//        return (double) numCorrect / testData.numInstances();
//    }
//}

// TSLA ->  BBG000N9MNX3
// AAPL -> BBG000B9XRY4
// t.r1RjGdULtS1QIwg-k30CmN6RQA65yLseFYIckRXHgyQupL6Vcs0lvGLDsvhs1V3mGTDhKOJxwJ6HxbDyuygcuQ