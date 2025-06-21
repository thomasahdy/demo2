package service;

import jakarta.ejb.Stateless;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import model.Notification;
import model.User;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.transaction.Transactional;
import java.util.List;

@Stateless
public class NotificationService {

    @PersistenceContext
    private EntityManager em;

    @Resource(lookup = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(lookup = "java:/jms/topic/NotificationTopic")
    private Topic notificationTopic;

    public void sendNotification(User user, Notification.NotificationType type, String message, Long entityId, String entityType) {
        // Create and persist notification
        Notification notification = new Notification(user, type, message, entityId, entityType);
        em.persist(notification);

        try {
            // Create JSON message
            JsonObject jsonMessage = Json.createObjectBuilder()
                    .add("userId", user.getId())
                    .add("type", type.toString())
                    .add("message", message)
                    .add("entityId", entityId != null ? entityId : -1)
                    .add("entityType", entityType != null ? entityType : "")
                    .add("timestamp", notification.getCreatedAt().toString())
                    .build();

            // Send using JMS
            try (Connection connection = connectionFactory.createConnection();
                 Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

                MessageProducer producer = session.createProducer(notificationTopic);
                TextMessage textMessage = session.createTextMessage(jsonMessage.toString());
                producer.send(textMessage);
            }
        } catch (Exception e) {
            // Log the error but don't fail the transaction
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }

    public void sendFriendRequestNotification(User recipient, User sender) {
        sendNotification(
                recipient,
                Notification.NotificationType.FRIEND_REQUEST,
                String.format("%s sent you a friend request", sender.getName()),
                sender.getId(),
                "USER"
        );
    }

    public void sendPostLikeNotification(User recipient, User liker, Long postId) {
        sendNotification(
                recipient,
                Notification.NotificationType.POST_LIKE,
                String.format("%s liked your post", liker.getName()),
                postId,
                "POST"
        );
    }

    public void sendPostCommentNotification(User recipient, User commenter, Long postId) {
        sendNotification(
                recipient,
                Notification.NotificationType.POST_COMMENT,
                String.format("%s commented on your post", commenter.getName()),
                postId,
                "POST"
        );
    }

    public void sendGroupJoinRequestNotification(User admin, User requester, Long groupId) {
        sendNotification(
                admin,
                Notification.NotificationType.GROUP_JOIN_REQUEST,
                String.format("%s requested to join your group", requester.getName()),
                groupId,
                "GROUP"
        );
    }

    public void sendGroupJoinApprovedNotification(User user, Long groupId) {
        sendNotification(
                user,
                Notification.NotificationType.GROUP_JOIN_APPROVED,
                "Your group join request was approved",
                groupId,
                "GROUP"
        );
    }

    public void sendGroupMembershipChangedNotification(User user, String action, Long groupId) {
        sendNotification(
                user,
                Notification.NotificationType.GROUP_MEMBERSHIP_CHANGED,
                String.format("Your group membership status has been %s", action),
                groupId,
                "GROUP"
        );
    }

    @Transactional
    public List<Notification> getUserNotifications(Long userId) {
        return em.createQuery(
                        "SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC",
                        Notification.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Transactional
    public List<Notification> getUnreadNotifications(Long userId) {
        return em.createQuery(
                        "SELECT n FROM Notification n WHERE n.user.id = :userId AND n.read = false ORDER BY n.createdAt DESC",
                        Notification.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Transactional
    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = em.createQuery(
                        "SELECT n FROM Notification n WHERE n.id = :notificationId AND n.user.id = :userId",
                        Notification.class)
                .setParameter("notificationId", notificationId)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (notification != null) {
            notification.setRead(true);
            em.merge(notification);
        }
        return notification;
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        em.createQuery(
                        "UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
                .setParameter("userId", userId)
                .executeUpdate();
    }
}