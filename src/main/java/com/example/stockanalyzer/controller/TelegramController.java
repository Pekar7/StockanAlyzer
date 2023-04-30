package com.example.stockanalyzer.controller;

import com.example.stockanalyzer.config.ApiConfig;
import com.example.stockanalyzer.model.NewsArticle;
import com.example.stockanalyzer.service.StockService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Slf4j
@Component
public class TelegramController extends TelegramLongPollingBot {
    final ApiConfig config;
    final StockService stockService;

    private final String APPLE = "Apple";
    private final String TESLA = "Tesla";
    private final String MICROSOFT = "Microsoft";
    private final String AMAZON = "Amazon";

    static final String HELP_TEXT = "Этот бот обрабатывает и выдает данные по акциям через Тикер.\nТикер — это краткое название финансового инструмента на бирже\n" +
            "Тикер можно найти тут: https://clck.ru/32ViCF\n\n";


    private static final String API_KEY = "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";
    private static final String ENDPOINT = "https://api.openai.com/v1/";

    private OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Autowired
    public TelegramController(ApiConfig config, StockService stockService) {
        this.config = config;
        this.stockService = stockService;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "Активируйте бота"));
        listofCommands.add(new BotCommand("/help", "Информация от бота"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }


    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().equals("/start")) {
            // Приветствие при запуске бота
            startCommandReceived(update.getMessage().getChatId(), update.getMessage().getChat().getFirstName());
            // Отправка кнопок
            sendYesNoKeyboard(update.getMessage().getChatId());
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String callbackData = callbackQuery.getData();
            Message message = callbackQuery.getMessage();
            switch (callbackData) {
                case "BBG000B9XRY4": //apple
                    getInfoFigi(message.getChatId(), "BBG000B9XRY4", APPLE);
                    break;
                case "BBG000BVPV84": //amazon
                    getInfoFigi(message.getChatId(), "BBG000BVPV84", AMAZON);
                    break;
                case "BBG000N9MNX3": //tesla
                    getInfoFigi(message.getChatId(), "BBG000N9MNX3", TESLA);
                    break;
                case "BBG000BPH459": //microsoft
                    getInfoFigi(message.getChatId(), "BBG000BPH459", MICROSOFT);
                    break;
            }
        }
    }

    @SneakyThrows
    private void getInfoFigi(long chatId, String figi, String companyName) {
        String stockInformation = stockService.getStockByTicker(figi); // получение цены акции и ифнормации
        List<NewsArticle> news = stockService.getNewsFromGoogle(companyName); //сбор всех новостей и отправка в csv файл

        String newsInformation = getLastNewsInformation(news, companyName); //получение новостей
        sendTextMessage(chatId, stockInformation+"\n"+newsInformation); // отрпавка цены и новости

        List<Double> param = stockService.getLineRegression(figi); // линейная ригрессия
        sendTextMessage(chatId, "Прогнозируемая цена компании " + companyName + "на следующий месяц: " + param.get(3).toString() + "$ " +
                "\nПараметры: \nSlope (наклон) значение, которое показывает, насколько быстро растет или падает: " + param.get(0) +
                "\nIntercept - это значение, которое показывает, где линия регрессии пересекает ось: " + param.get(1) +
                "\nR-squared (коэффициент детерминации): " + param.get(2)
        );

        sendTextMessage(chatId, stockInformation+"\n"+newsInformation);

        stockService.getCandleByFigi(figi); // сбор данных свечей
        sendYesNoKeyboard(chatId);
    }

    private void sendPredictInformation(long chatId, Double predictPrice) {
        String text = "Прогнозируемая цена на следующий месяц: " + predictPrice.toString();
    }

    private String getLastNewsInformation(List<NewsArticle> news, String companyName) {
        String[] titles = news.get(news.size()-1).getTitle().split("\n");
        String[] urlNews = news.get(news.size()-1).getUrlNews().split("\n");

        LocalDateTime date = news.get(news.size()-1).getDate();
        String dateString = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        LocalDate parsedDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String formattedDate = parsedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy 'года'", new Locale("ru")));

        List<String> message = new ArrayList<>();
        for (int i = 0; i < titles.length; i++) {
            String title = titles[i];
            String urlArticle = urlNews[i];

            message.add("\uD83D\uDCA1 " + title + ". \n" +
                    "\uD83D\uDD17 <a href='" + urlArticle.trim() + "'>" + "Ссылка" + "</a> \n\n");
        }

        String text = "";
        for (int i = 0; i < message.size(); i++) {
            text += message.get(i);
        }

        return "<b>⚡ Последние новости от компании " + companyName + " было " + formattedDate + ":</b>\n\n" + text;
    }

    private void sendYesNoKeyboard(Long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton apple = new InlineKeyboardButton();
        apple.setText("Apple");
        apple.setCallbackData("BBG000B9XRY4");
        row.add(apple);

        InlineKeyboardButton amazon = new InlineKeyboardButton();
        amazon.setText("Amazon");
        amazon.setCallbackData("BBG000BVPV84");
        row.add(amazon);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton tesla = new InlineKeyboardButton();
        tesla.setText("Tesla");
        tesla.setCallbackData("BBG000N9MNX3");
        row2.add(tesla);

        InlineKeyboardButton microsoft = new InlineKeyboardButton();
        microsoft.setText("Microsoft");
        microsoft.setCallbackData("BBG000BPH459");
        row2.add(microsoft);


        keyboard.add(row);
        keyboard.add(row2);
        markup.setKeyboard(keyboard);

        sendKeyboardMessage(chatId, "Выберите акции компании из списка: ", markup);
    }

    private void sendKeyboardMessage(Long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(markup);
        sendTextMessage(message);
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.enableHtml(true);
        message.setParseMode(ParseMode.HTML);
        message.disableWebPagePreview();
        message.disableNotification();
        sendTextMessage(message);
    }

    private void sendTextMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void startCommandReceived(long chatId, String firstName) {
        String answer = "Привет, " + firstName + "! \uD83D\uDE42 \nЯ бот @MisisServiceBot который умеет анализировать и погнозировать курс акции технических компаний, таких как:" +
                "\n- Apple \uD83C\uDF4F  \n- Tesla Motors \uD83D\uDE98 \n- Microsoft \uD83E\uDD16 \n- Amazon \uD83D\uDECD";
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);

        try {
            execute(sendMessage);
        } catch (TelegramApiException ex) {
            log.error("Exception ", ex);
        }
    }
}
