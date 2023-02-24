package com.example.stockanalyzer.service;

import com.example.stockanalyzer.config.ApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;



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
            String message_text = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();
            if (message_text.equals("/start")) {
                SendMessage message = new SendMessage(); // Create a message object
                message.setChatId(chat_id);
                message.setText("Хотите ли вы добавить свое имя в предложение?");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                List<KeyboardRow> keyboard = new ArrayList<>();
                KeyboardRow row = new KeyboardRow();

                row.add(new KeyboardButton("Да"));
                row.add(new KeyboardButton("Нет"));
                keyboard.add(row);
                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                try {
                    execute(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (message_text.equals("Да")) {
                SendMessage message = new SendMessage(); // Create a message object
                message.setChatId(chat_id);
                message.setText("Как вас зовут?");
                try {
                    execute(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (message_text.equals("Нет")) {
                SendMessage message = new SendMessage(); // Create a message object
                message.setChatId(chat_id);
                message.setText("Хорошо, я не буду добавлять ваше имя в предложение.");
                try {
                    execute(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {
                String name = message_text;
                SendMessage message = new SendMessage(); // Create a message object
                message.setChatId(chat_id);
                message.setText("Привет, " + name + "! Как дела?");
                try {
                    execute(message); // Sending our message object to user
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