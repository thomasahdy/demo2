package service;

import jakarta.ejb.MessageDriven;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.ejb.ActivationConfigProperty;
import java.util.logging.Logger;
import java.util.logging.Level;

@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty(
                        propertyName = "destinationType",
                        propertyValue = "jakarta.jms.Topic"
                ),
                @ActivationConfigProperty(
                        propertyName = "destination",
                        propertyValue = "java:/jms/topic/NotificationTopic"
                ),
                @ActivationConfigProperty(
                        propertyName = "acknowledgeMode",
                        propertyValue = "Auto-acknowledge"
                )
        }
)
public class NotificationConsumer implements MessageListener {

    private static final Logger logger = Logger.getLogger(NotificationConsumer.class.getName());

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                String jsonMessage = ((TextMessage) message).getText();
                logger.info("Received notification: " + jsonMessage);

                // Here you could implement WebSocket broadcasting or other real-time notification mechanisms
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing notification message", e);
        }
    }
}