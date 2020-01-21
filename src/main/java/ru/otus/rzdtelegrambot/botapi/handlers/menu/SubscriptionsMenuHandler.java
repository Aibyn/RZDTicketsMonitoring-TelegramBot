package ru.otus.rzdtelegrambot.botapi.handlers.menu;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.otus.rzdtelegrambot.botapi.BotState;
import ru.otus.rzdtelegrambot.botapi.RZDTelegramBot;
import ru.otus.rzdtelegrambot.botapi.handlers.InputMessageHandler;
import ru.otus.rzdtelegrambot.botapi.handlers.callbackquery.CallbackQueryType;
import ru.otus.rzdtelegrambot.cache.UserDataCache;
import ru.otus.rzdtelegrambot.model.Car;
import ru.otus.rzdtelegrambot.model.UserTicketsSubscription;
import ru.otus.rzdtelegrambot.service.ReplyMessagesService;
import ru.otus.rzdtelegrambot.service.UserTicketsSubscriptionService;
import ru.otus.rzdtelegrambot.utils.Emojis;

import java.util.List;

/**
 * @author Sergei Viacheslaev
 */
@Component
public class SubscriptionsMenuHandler implements InputMessageHandler {
    private final String CARS_TICKETS_MESSAGE;
    private final String TRAIN_TICKETS_INFO;

    private UserTicketsSubscriptionService subscribeService;
    private RZDTelegramBot telegramBot;
    private UserDataCache userDataCache;
    private ReplyMessagesService messagesService;

    public SubscriptionsMenuHandler(UserTicketsSubscriptionService subscribeService,
                                    UserDataCache userDataCache,
                                    ReplyMessagesService messagesService,
                                    @Lazy RZDTelegramBot telegramBot) {
        this.subscribeService = subscribeService;
        this.messagesService = messagesService;
        this.userDataCache = userDataCache;
        this.telegramBot = telegramBot;

        CARS_TICKETS_MESSAGE = messagesService.getReplyText("subscription.carsTicketsInfo");
        TRAIN_TICKETS_INFO = messagesService.getReplyText("subscriptionMenu.trainTicketsInfo");
    }

    @Override
    public SendMessage handle(Message message) {
        List<UserTicketsSubscription> usersSubscriptions = subscribeService.getUsersSubscriptions(message.getChatId());

        if (usersSubscriptions.isEmpty()) {
            return messagesService.getReplyMessage(message.getChatId(), "reply.subscriptions.userHasNoSubscriptions");
        }

        for (UserTicketsSubscription subscription : usersSubscriptions) {
            StringBuilder carsInfo = new StringBuilder();
            List<Car> cars = subscription.getSubscribedCars();

            for (Car car : cars) {
                carsInfo.append(String.format(CARS_TICKETS_MESSAGE,
                        car.getCarType(), car.getFreeSeats(), car.getMinimalPrice()));
            }

            String subscriptionInfo = String.format(TRAIN_TICKETS_INFO,
                    Emojis.TRAIN, subscription.getTrainNumber(), subscription.getTrainName(), subscription.getStationDepart(), subscription.getStationArrival(),
                    Emojis.TIME_DEPART, subscription.getDateDepart(), carsInfo);

            //Посылаем кнопку "Отписаться" с ID подписки
            String callbackData = String.format("%s|%s", CallbackQueryType.UNSUBSCRIBE, subscription.getId());
            telegramBot.sendInlineKeyBoardMessage(message.getChatId(), subscriptionInfo, "Отписаться", callbackData);
        }

        userDataCache.setUsersCurrentBotState(message.getFrom().getId(), BotState.SHOW_MAIN_MENU);

        return messagesService.getSuccessReplyMessage(message.getChatId(), "reply.subscriptions.listLoaded");
    }

    @Override
    public BotState getHandlerName() {
        return BotState.SHOW_SUBSCRIPTIONS_MENU;
    }


}
