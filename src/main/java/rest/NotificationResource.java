package rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.RolesAllowed;
import model.Notification;
import model.User;
import service.NotificationService;
import service.UserService;
import java.util.List;
import java.time.LocalDateTime;

@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
public class NotificationResource {

    @EJB
    private NotificationService notificationService;

    @EJB
    private UserService userService;

    public static class NotificationDTO {
        private Long id;
        private String type;
        private String message;
        private Long entityId;
        private String entityType;
        private boolean read;
        private LocalDateTime createdAt;

        public static NotificationDTO fromEntity(Notification notification) {
            NotificationDTO dto = new NotificationDTO();
            dto.setId(notification.getId());
            dto.setType(notification.getType().toString());
            dto.setMessage(notification.getMessage());
            dto.setEntityId(notification.getEntityId());
            dto.setEntityType(notification.getEntityType());
            dto.setRead(notification.isRead());
            dto.setCreatedAt(notification.getCreatedAt());
            return dto;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Long getEntityId() { return entityId; }
        public void setEntityId(Long entityId) { this.entityId = entityId; }

        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }

        public boolean isRead() { return read; }
        public void setRead(boolean read) { this.read = read; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    private User validateToken(String authHeader) throws WebApplicationException {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new NotAuthorizedException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        User user = userService.findByAuthToken(token);
        if (user == null) {
            throw new NotAuthorizedException("Invalid token");
        }
        return user;
    }

    @GET
    @RolesAllowed({"USER", "ADMIN"})
    public Response getNotifications(@HeaderParam("Authorization") String token) {
        try {
            User user = validateToken(token);
            List<NotificationDTO> notifications = notificationService.getUserNotifications(user.getId())
                    .stream()
                    .map(NotificationDTO::fromEntity)
                    .toList();
            return Response.ok(notifications).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error retrieving notifications: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/unread")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getUnreadNotifications(@HeaderParam("Authorization") String token) {
        try {
            User user = validateToken(token);
            List<NotificationDTO> notifications = notificationService.getUnreadNotifications(user.getId())
                    .stream()
                    .map(NotificationDTO::fromEntity)
                    .toList();
            return Response.ok(notifications).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error retrieving unread notifications: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/{notificationId}/read")
    @RolesAllowed({"USER", "ADMIN"})
    public Response markAsRead(
            @HeaderParam("Authorization") String token,
            @PathParam("notificationId") Long notificationId) {
        try {
            User user = validateToken(token);
            Notification notification = notificationService.markAsRead(notificationId, user.getId());

            if (notification == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Notification not found")
                        .build();
            }

            return Response.ok(NotificationDTO.fromEntity(notification)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error marking notification as read: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/read-all")
    @RolesAllowed({"USER", "ADMIN"})
    public Response markAllAsRead(@HeaderParam("Authorization") String token) {
        try {
            User user = validateToken(token);
            notificationService.markAllAsRead(user.getId());
            return Response.ok("All notifications marked as read").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error marking all notifications as read: " + e.getMessage())
                    .build();
        }
    }
}