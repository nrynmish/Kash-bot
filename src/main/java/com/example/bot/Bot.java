package com.example.bot;


import java.time.*;
import java.util.concurrent.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import java.util.*;

public class Bot extends TelegramLongPollingBot {

private static final Map<Long, Map<String, List<String>>> timetable = new HashMap<>();
private static final Map<Long, String> userChats = new HashMap<>();

@Override
public void onUpdateReceived(Update update) {

    if (!update.hasMessage() || !update.getMessage().hasText()) {
        return;
    }

    String text = update.getMessage().getText().toLowerCase();
    Long userId = update.getMessage().getFrom().getId();
    String chatId = update.getMessage().getChatId().toString();

    userChats.put(userId, chatId);

    if (text.equals("/start")) {
        handleStart(chatId);

    } else if (text.equals("/help")) {
        handleHelp(chatId);

    } else if (text.equals("/today")) {
        handleToday(userId, chatId);

    } else if (text.startsWith("/add")) {
        handleAdd(userId, chatId, text);

    } else if (isDayCommand(text)) {
        handleDay(userId, chatId, text.substring(1));

    } else {
        reply(chatId, "Unknown command. Type /help");
    }
}

private boolean isDayCommand(String text) {
    return List.of(
            "/monday", "/tuesday", "/wednesday",
            "/thursday", "/friday", "/saturday", "/sunday"
    ).contains(text);
}

private void startDailyReminder() {

    System.out.println("⏰ Daily reminder scheduler started");

    ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    long delay = secondsUntilNext8AM();
    long period = TimeUnit.DAYS.toSeconds(1);


    scheduler.scheduleAtFixedRate(() -> {
        sendTodaySchedule();   
    }, delay, period, TimeUnit.SECONDS);
}


private long secondsUntilNext8AM() {

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime next8am = now.withHour(8).withMinute(0).withSecond(0);

    if (now.isAfter(next8am)) {
        next8am = next8am.plusDays(1);
    }

    return Duration.between(now, next8am).getSeconds();
}

private void sendTodaySchedule() {

    String today = LocalDate.now().getDayOfWeek().name().toLowerCase();

    for (Long userId : timetable.keySet()) {

        String chatId = userChats.get(userId);
        if (chatId == null) {
            continue;
        }

        Map<String, List<String>> userTable = timetable.get(userId);
        if (userTable == null) continue;

        List<String> classes = userTable.get(today);
        if (classes == null || classes.isEmpty()) {
            continue;
        }

        StringBuilder msg = new StringBuilder("⏰ Today's Classes\n\n");
        for (String c : classes) {
            msg.append("• ").append(c).append("\n");
        }

        reply(chatId, msg.toString());
    }
}

private void handleDay(Long userId, String chatId, String day) {

    Map<String, List<String>> userTable =
            timetable.getOrDefault(userId, new HashMap<>());

    List<String> classes = userTable.get(day);

    if (classes == null || classes.isEmpty()) {
        reply(chatId,
                "📅 " + capitalize(day) + "\n\n" +
                "No classes saved yet.\n\n" +
                "Add one using:\n" +
                "/add " + day + " 9-10 Subject"
        );
        return;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("📅 ").append(capitalize(day)).append("\n\n");

    for (String c : classes) {
        sb.append("• ").append(c).append("\n");
    }

    reply(chatId, sb.toString());
}

private void handleAdd(Long userId, String chatId, String text) {

    String[] parts = text.split(" ", 4);

    if (parts.length < 4) {
        reply(chatId, "Usage:\n/add monday 9-10 Subject");
        return;
    }

    String day = parts[1];
    String time = parts[2];
    String subject = parts[3];

    timetable.putIfAbsent(userId, new HashMap<>());
    Map<String, List<String>> userTable = timetable.get(userId);

    userTable.putIfAbsent(day, new ArrayList<>());
    userTable.get(day).add(time + " " + subject);

    reply(chatId, "✅ Added to " + capitalize(day));
}

private void handleToday(Long userId, String chatId) {
    DayOfWeek today = LocalDate.now().getDayOfWeek();
    String day = today.name().toLowerCase();
    handleDay(userId, chatId, day);
}

private String capitalize(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1);
}

    private void handleStart(String chatId) {
        String welcome =
                "👋 Welcome to Kash Bot!\n\n" +
                "I am a simple Telegram bot built in Java.\n" +
                "Send me any message and I will reply.\n\n" +
                "Type /help to see what I can do.";

        reply(chatId, welcome);
    }

private void handleHelp(String chatId) {
    String help =
            "ℹ️ *Help Menu*\n\n" +
            "/start - Start the bot\n" +
            "/help - Show this help message\n\n" +
            "Send any text and I will echo it back.";

    reply(chatId, help);
}

private void reply(String chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText(text);
    message.setParseMode("Markdown");

    try {
        execute(message);
    } catch (TelegramApiException e) {
        e.printStackTrace();
    }
}
 @Override
 public String getBotUsername() {
	 return "Kashsoda11Bot";
 }

 @Override
 public String getBotToken() {
         return "8519572294:AAHqz9Sm_iJEAXp8UqTcCP8y31X6sA3ZrXA";
 }

 public static void main(String[] args) throws TelegramApiException {
  TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
  Bot bot = new Bot();
  botsApi.registerBot(bot);
  bot.startDailyReminder();
 }

}
