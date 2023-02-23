package com.example.stockanalyzer.service;

import com.example.stockanalyzer.config.ApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;


import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ServiceBot extends TelegramLongPollingBot {
    final ApiConfig config;
    final StockService stockService;

    static final String HELP_TEXT = "Этот бот обрабатывает и выдает данные по акциям через Тикер.\nТикер — это краткое название финансового инструмента на бирже\n" +
            "Тикер можно найти тут: https://clck.ru/32ViCF\n\n";

    static final String TEXT_BIO = "Этот бот создал karen_ahper.\n\n" +
            "GitHub: Pekar7\nTelegram: @karen_ahper\nInstagram: https://instagram.com/karen_ahper?igshid=YmMyMTA2M2Y=\n\n"
            +"P.S у иеня есть твои данные :)";


    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Autowired
    public ServiceBot(ApiConfig config, StockService stockService) {
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

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
            SendMessage message = new SendMessage();
            message.setChatId(update.getMessage().getChatId());
            message.setText("Выберите акцию:");

            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

            List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText("Apple");
            button1.setCallbackData("1");

            InlineKeyboardButton button2 = new InlineKeyboardButton();
            button2.setText("Tesla Motors");
            button2.setCallbackData("2");

            keyboardButtonsRow1.add(button1);
            keyboardButtonsRow1.add(button2);

            List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();
            keyboardButtons.add(keyboardButtonsRow1);

            keyboardMarkup.setKeyboard(keyboardButtons);

            message.setReplyMarkup(keyboardMarkup);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (update.hasCallbackQuery()) {
            if (update.hasCallbackQuery()) {
                String callbackData = update.getCallbackQuery().getData();
                long chatId = update.getCallbackQuery().getMessage().getChatId();

                String messageText;

                switch (callbackData) {
                    case "1":
                        messageText = "You chose Option 1 " + stockService.getStockByTicker("BBG000B9XRY4");
                        sendMessage(chatId, messageText);
                        Update update1 = new Update();
                        Message message = update1.getMessage();

                        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

                        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
                        InlineKeyboardButton button1 = new InlineKeyboardButton();
                        button1.setText("Apple");
                        button1.setCallbackData("1");

                        InlineKeyboardButton button2 = new InlineKeyboardButton();
                        button2.setText("Tesla Motors");
                        button2.setCallbackData("2");

                        keyboardButtonsRow1.add(button1);
                        keyboardButtonsRow1.add(button2);

                        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();
                        keyboardButtons.add(keyboardButtonsRow1);

                        keyboardMarkup.setKeyboard(keyboardButtons);

                        message.setReplyMarkup(keyboardMarkup);

                        break;
                    case "2":
                        messageText = "You chose Option 2 " + stockService.getStockByTicker("BBG000N9MNX3");
                        break;
                    default:
                        messageText = "You chose Option 1";
                        break;
                }

                SendMessage answerMessage = new SendMessage();
                answerMessage.setChatId(chatId);
                answerMessage.setText(messageText);

                try {
                    execute(answerMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private void startCommandReceived(long chatId, String firstName) {
        String answer = "Привет, " + firstName + "! \nВас приветсвует виртуальный ассистент @MisisServiceBot. \nНаш бот позволяет получать информацию по ценным бумагам, используя Тикеры!"
                + "\nУкажите Тикер.\n\nНАПРИМЕР: TSLA или SBER\nНажмите /help чтобы найти список тикеров";
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