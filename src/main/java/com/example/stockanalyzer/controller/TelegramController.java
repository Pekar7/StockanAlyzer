package com.example.stockanalyzer.controller;

import com.example.stockanalyzer.config.ApiConfig;
import com.example.stockanalyzer.service.StockService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
public class TelegramController extends TelegramLongPollingBot {
    final ApiConfig config;
    final StockService stockService;

    static final String HELP_TEXT = "Этот бот обрабатывает и выдает данные по акциям через Тикер.\nТикер — это краткое название финансового инструмента на бирже\n" +
            "Тикер можно найти тут: https://clck.ru/32ViCF\n\n";


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
//                    String appleNewsGuardian = "https://content.guardianapis.com/search?q=Apple%20Inc&section=technology&page-size=20&api-key=";
                    String appleNews = "Apple";
                    getInfoFigi(message.getChatId(), "BBG000B9XRY4", appleNews);
                    getBrownModel(message.getChatId(), "BBG000B9XRY4");
                    break;
                case "BBG000BVPV84": //amazon
//                    String amazonGuardian = "https://content.guardianapis.com/search?q=Amazon%20AND%20company&page-size=20&api-key=";
                    String amazonNews = "Amazon";
                    getInfoFigi(message.getChatId(), "BBG000BVPV84", amazonNews);
                    getBrownModel(message.getChatId(), "BBG000BVPV84");
                    break;
                case "BBG000N9MNX3": //tesla
//                    String teslaNewsGuardian = "https://content.guardianapis.com/search?q=Tesla%20motors&page-size=20&api-key=";
                    String teslaNews = "Tesla";
                    getInfoFigi(message.getChatId(), "BBG000N9MNX3", teslaNews);
                    getBrownModel(message.getChatId(), "BBG000N9MNX3");
                    break;
                case "BBG000BPH459": //microsoft
//                    String microsoftGuardian = "https://content.guardianapis.com/search?q=Microsoft%20&page-size=20&api-key=";
                    String microsoftNews = "Microsoft";
                    getInfoFigi(message.getChatId(), "BBG000BPH459", microsoftNews);
                    getBrownModel(message.getChatId(), "BBG000BPH459");
                    break;
            }
        }
    }

    @SneakyThrows
    private void getInfoFigi(long chatId, String figi, String url) {
        String message = stockService.getStockByTicker(figi);
//        String news = stockService.getNewsFromGuardian(url);
        String news = stockService.getNewsFromGoogle(url);
        sendTextMessage(chatId, message+"\n"+news);
    }

    private void getBrownModel(long chatId, String figi) {
        stockService.getCandleByFigi(figi);
//        Double priceBrown = stockService.getAnalysisBrown();
//        Double price = stockService.getPriceStock(figi);
//        if (priceBrown > price) {
//            sendTextMessage(chatId, "Модель Брауна прогнозирует рост цены на следующий месяц \u2714");
//        } else if (priceBrown < price) {
//            sendTextMessage(chatId, "Модель Брауна прогнозирует падание цены на следующий месяц \u274C");
//        } else {
//            sendTextMessage(chatId, "Модель Брауна прогнозирует стагнацию цены на следующий месяц");
//        }
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
        String answer = "Привет, " + firstName + "! \nЯ бот @MisisServiceBot, который умеет анализировать и погнозировать акции технических компаний, таких как:" +
                " Apple, Tesla Motors, Microsoft, Amazon.";
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
