package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.*;

public class DeadMorose extends TelegramLongPollingBot {
    private List<Person> users = new ArrayList<>();
    private Map<Long, RegistrationStep> registrationSteps = new HashMap<>();
    private Timer timer = new Timer(); // Таймер для планирования задач
    private boolean isAssignmentScheduled = false; // Флаг для отслеживания, запланирована ли задача


    @Override
    public String getBotUsername() {
        return "dead_morose_bot";
    }

    @Override
    public String getBotToken() {
        return "6952128369:AAF4i6OjcZx_wIkxEGZytw-2EyUlaye9EVs";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String userMessage = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            String username = update.getMessage().getFrom().getUserName();
            String firstName = update.getMessage().getFrom().getFirstName();
            String lastName = update.getMessage().getFrom().getLastName();

            System.out.println("Пользователь: " + (username != null ? "@" + username : firstName + " " + lastName));
            System.out.println("Получено сообщение от пользователя с chatId " + chatId + ": " + userMessage);

            // Проверяем, если пользователь отправил команду /start
            if (userMessage.equals("/start")) {
                startRegistration(chatId);  // Начинаем процесс регистрации
                return;
            }

            // Проверка, зарегистрирован ли пользователь
            Person user = findUserByChatId(chatId);
            if (user == null) {
                // Создаем нового пользователя
                user = new Person(chatId);
                users.add(user);
            }

            // Получаем текущий этап пользователя
            RegistrationStep step = getCurrentStepForUser(chatId);

            switch (step) {
                case NAME:
                    // Сохраняем имя пользователя
                    user.setUsername(userMessage);
                    sendMessage(chatId, "Хо-хо-хо, приятно познакомиться! А где ты проживаешь?");
                    updateUserStep(chatId, RegistrationStep.COUNTRY);
                    sendCountrySelection(chatId); // Переход на выбор страны
                    break;

                case COUNTRY:
                    sendCountrySelection(chatId);
                    break;

                case ADDRESS:
                    handleAddressInput(chatId, userMessage);
                    break;

                case WISHES:
                    handleWishesInput(chatId, userMessage);
                    break;

                case CHANGE_ADDRESS: // Обработка изменения адреса
                    handleAddressChange(chatId, userMessage);
                    break;

                case CHANGE_WISHES: // Обработка изменения пожеланий
                    handleWishesChange(chatId, userMessage);
                    break;

                case COMPLETED:
                    sendMessage(chatId, "О-о-о! Ты уже зарегистрировался! Мои олени готовы к полету, подарки готовы!");
                    break;
            }

        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            int messageId = update.getCallbackQuery().getMessage().getMessageId();

            // Обработка нажатия на кнопки "ДА" и "НЕТ" для пользователей из России
            if (callbackData.equals("YES_ANYWHERE")) {
                Person user = findUserByChatId(chatId);
                if (user != null && user.getCountry() == Country.RUSSIA) {
                    user.setCanSendAnywhere(true); // Пользователь может отправлять куда угодно
                    proceedToAddressStep(chatId); // Переход к следующему шагу
                }
            } else if (callbackData.equals("NO_ANYWHERE")) {
                Person user = findUserByChatId(chatId);
                if (user != null && user.getCountry() == Country.RUSSIA) {
                    user.setCanSendAnywhere(false); // Пользователь ограничен Россией и Европой
                    proceedToAddressStep(chatId); // Переход к следующему шагу
                }
            }
            // Обработка нажатия на кнопки изменения адреса и пожеланий
            else if (callbackData.equals("CHANGE_ADDRESS")) {
                sendMessage(chatId, "Хо-хо-хо! Введи новый адрес: город, улица, дом, квартира, индекс.");
                updateUserStep(chatId, RegistrationStep.CHANGE_ADDRESS); // Переводим пользователя в этап изменения адреса
            } else if (callbackData.equals("CHANGE_WISHES")) {
                sendMessage(chatId, "Хо-хо-хо! Введи новые пожелания, чтобы я знал, что положить под ёлку.");
                updateUserStep(chatId, RegistrationStep.CHANGE_WISHES); // Переводим пользователя в этап изменения пожеланий
            } else {
                // Обработка выбора страны
                handleCountrySelection(chatId, messageId, callbackData);
            }
        }
    }
    private void proceedToAddressStep(long chatId) {
        sendMessage(chatId, "Отлично! Теперь введи свой адрес: город, улица, дом, квартира (если живешь в домике, можешь пропустить квартиру), индекс.");
        updateUserStep(chatId, RegistrationStep.ADDRESS);
    }


    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendCountrySelection(long chatId) {
        SendPhoto sendPhotoRequest = new SendPhoto();
        sendPhotoRequest.setChatId(String.valueOf(chatId));

        // Загрузить изображение
        sendPhotoRequest.setPhoto(new InputFile("https://imgur.com/a/e0AcOE4")); // Укажите правильный путь

        sendPhotoRequest.setCaption("Выбери, где ты находишься, дорогой друг! Санта должен знать, куда доставить подарок:");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка для России
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("Россия");
        button1.setCallbackData("RUSSIA");

        // Кнопка для Европы/Америки
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("Европа");
        button2.setCallbackData("EUROPE");

        // Кнопка для Америки
        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText("Америка");
        button3.setCallbackData("AMERICA");

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        rowInline1.add(button1);

        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        rowInline2.add(button2);

        List<InlineKeyboardButton> rowInline3 = new ArrayList<>();
        rowInline3.add(button3);

        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);
        rowsInline.add(rowInline3);

        markupInline.setKeyboard(rowsInline);
        sendPhotoRequest.setReplyMarkup(markupInline);

        try {
            execute(sendPhotoRequest);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCountrySelection(long chatId, int messageId, String callbackData) {
        Person user = findUserByChatId(chatId);
        if (user == null) {
            user = new Person(chatId);
            users.add(user);
        }

        if (callbackData.equals("RUSSIA")) {
            user.setCountry(Country.RUSSIA);
            sendYesNoQuestion(chatId, "Без разницы ли вам, куда отправлять подарки?");
        } else if (callbackData.equals("EUROPE")) {
            user.setCountry(Country.EUROPE);
            proceedToAddressStep(chatId);
        } else if (callbackData.equals("AMERICA")) {
            user.setCountry(Country.AMERICA);
            proceedToAddressStep(chatId);
        }



    }
    private void sendYesNoQuestion(long chatId, String question) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(question);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка "ДА"
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("ДА");
        yesButton.setCallbackData("YES_ANYWHERE");

        // Кнопка "НЕТ"
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("НЕТ");
        noButton.setCallbackData("NO_ANYWHERE");

        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add(yesButton);
        rowInline.add(noButton);

        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void startRegistration(long chatId) {
        if (getCurrentStepForUser(chatId) == RegistrationStep.COMPLETED) {
            sendMessage(chatId, "О-о-о! Ты уже в списке хороших ребят! Мы уже знаем твой адрес!");
            sendChangeOptions(chatId);
            return;
        }

        sendMessage(chatId, "Хо-хо-хо! Добро пожаловать! Как мне тебя называть? Введи свое имя:");
        updateUserStep(chatId, RegistrationStep.NAME);
    }

    private void handleAddressInput(long chatId, String userMessage) {
        Person user = findUserByChatId(chatId);
        if (user != null) {
            user.setAddress(userMessage);
            sendMessage(chatId, "О-о-о! Теперь расскажи мне, какие у тебя пожелания? Что ты хочешь найти под ёлкой?");
            updateUserStep(chatId, RegistrationStep.WISHES);
        }
    }


    private void handleWishesInput(long chatId, String userMessage) {
        Person user = findUserByChatId(chatId);
        if (user != null) {
            user.setWishes(userMessage);
            sendMessage(chatId, "Спасибо за твои пожелания! Санта учел их! Теперь жди подарка!");

            // Предлагаем изменить адрес или пожелания
            sendChangeOptions(chatId);

            updateUserStep(chatId, RegistrationStep.COMPLETED);

            // Планируем отправку адресов, если она еще не была запланирована
            if (!isAssignmentScheduled) {
                scheduleAddressAssignmentExample();
                isAssignmentScheduled = true; // Устанавливаем флаг
            }
        }
    }
    private void sendChangeOptions(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Если хочешь что-то изменить, то время еще есть! Хо-хо-хо");

        // Создаем кнопки
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка для изменения адреса
        InlineKeyboardButton changeAddressButton = new InlineKeyboardButton();
        changeAddressButton.setText("Изменить адрес");
        changeAddressButton.setCallbackData("CHANGE_ADDRESS");

        // Кнопка для изменения пожеланий
        InlineKeyboardButton changeWishesButton = new InlineKeyboardButton();
        changeWishesButton.setText("Изменить пожелания");
        changeWishesButton.setCallbackData("CHANGE_WISHES");

        // Каждая строка должна быть списком кнопок
        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        rowInline1.add(changeAddressButton);

        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        rowInline2.add(changeWishesButton);

        // Добавляем строки в общий список строк
        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);

        // Устанавливаем разметку с кнопками
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    // Метод для изменения адреса
    private void handleAddressChange(long chatId, String userMessage) {
        Person user = findUserByChatId(chatId);
        if (user != null) {
            user.setAddress(userMessage);
            sendMessage(chatId, "Твой адрес обновлен, хо-хо-хо!");
            sendChangeOptions(chatId); // Предлагаем снова изменить данные или оставить как есть
            updateUserStep(chatId, RegistrationStep.COMPLETED); // Возвращаем на этап завершения
        }
    }

    // Метод для изменения пожеланий
    private void handleWishesChange(long chatId, String userMessage) {
        Person user = findUserByChatId(chatId);
        if (user != null) {
            user.setWishes(userMessage);
            sendMessage(chatId, "Твои пожелания обновлены, хо-хо-хо!");
            sendChangeOptions(chatId); // Предлагаем снова изменить данные или оставить как есть
            updateUserStep(chatId, RegistrationStep.COMPLETED); // Возвращаем на этап завершения
        }
    }

    private void assignAddressesToUsers() {
        if (users.size() < 2) {
            sendMessageToAll("О-о-о! Недостаточно хороших ребят для обмена подарками. Зовите друзей!");
            return;
        }

        // Перемешиваем пользователей
        List<Person> shuffledUsers = new ArrayList<>(users);
        Collections.shuffle(shuffledUsers);

        for (Person giver : shuffledUsers) {
            // Получаем список доступных получателей для этого дарителя
            List<Person> eligibleReceivers = getEligibleReceivers(giver);

            if (eligibleReceivers.isEmpty()) {
                sendMessage(giver.getChatId(), "К сожалению, для тебя нет доступных получателей, соответствующих твоей стране.");
                continue;
            }

            // Перемешиваем список возможных получателей
            Collections.shuffle(eligibleReceivers);

            // Выбираем первого доступного получателя
            Person receiver = eligibleReceivers.get(0);

            // Отправляем имя, адрес и пожелания получателя дарителю
            sendMessage(giver.getChatId(), "Ты будешь тайным Сантой для " + receiver.getUsername() +
                    ", который живёт по адресу: " + receiver.getAddress() +
                    ". Он больше всего хотел бы: " + receiver.getWishes());
        }
    }

    public void scheduleAddressAssignment(Date assignmentTime) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                assignAddressesToUsers();
            }
        }, assignmentTime);
    }

    public void scheduleAddressAssignmentExample() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 12);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 36);
        calendar.set(Calendar.SECOND, 0);
        Date assignmentTime = calendar.getTime();

        Date now = new Date();
        if (assignmentTime.before(now)) {
            sendMessageToAll("Хо-хо-хо! Время для отправки подарков уже прошло! Давайте попробуем позже!");
            return;
        }
        scheduleAddressAssignment(assignmentTime);
    }

    private void updateUserStep(long chatId, RegistrationStep step) {
        registrationSteps.put(chatId, step);
    }

    private RegistrationStep getCurrentStepForUser(long chatId) {
        return registrationSteps.getOrDefault(chatId, RegistrationStep.COUNTRY);
    }

    private Person findUserByChatId(long chatId) {
        return users.stream().filter(user -> user.getChatId() == chatId).findFirst().orElse(null);
    }

    private void sendMessageToAll(String message) {
        for (Person user : users) {
            sendMessage(user.getChatId(), message);
        }
    }

    private List<Person> getEligibleReceivers(Person giver) {
        List<Person> eligibleReceivers = new ArrayList<>();

        for (Person receiver : users) {
            if (receiver.equals(giver)) {
                // Нельзя самому себе дарить подарок
                continue;
            }

            // Логика фильтрации на основе страны дарителя
            if (giver.getCountry() == Country.RUSSIA) {
                // Если пользователь из России и может отправлять в любую страну
                if (giver.canSendAnywhere()) {
                    eligibleReceivers.add(receiver);
                } else {
                    // Россия может дарить только в Россию и Европу
                    if (receiver.getCountry() == Country.RUSSIA || receiver.getCountry() == Country.EUROPE) {
                        eligibleReceivers.add(receiver);
                    }
                }
            } else if (giver.getCountry() == Country.AMERICA) {
                // Америка может дарить только в Америку и Европу
                if (receiver.getCountry() == Country.AMERICA || receiver.getCountry() == Country.EUROPE) {
                    eligibleReceivers.add(receiver);
                }
            } else if (giver.getCountry() == Country.EUROPE) {
                // Европа может дарить в любую страну
                eligibleReceivers.add(receiver);
            }
        }

        return eligibleReceivers;
    }

}

