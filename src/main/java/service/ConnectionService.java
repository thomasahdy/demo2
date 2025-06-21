package service;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import model.FriendRequest;
import model.FriendRequestDTO;
import model.User;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.NoResultException;
import model.UserDTO;
import java.util.ArrayList;
import jakarta.ejb.EJB;


@Stateless
public class ConnectionService {

    @PersistenceContext
    private EntityManager em;
    
    @EJB
    private NotificationService notificationService;

    public void sendRequest(User sender, User receiver) {
        TypedQuery<FriendRequest> query = em.createQuery(
                "SELECT fr FROM FriendRequest fr WHERE " +
                        "(fr.sender = :sender AND fr.receiver = :receiver) OR " +
                        "(fr.sender = :receiver AND fr.receiver = :sender)",
                FriendRequest.class);
        query.setParameter("sender", sender);
        query.setParameter("receiver", receiver);

        List<FriendRequest> existingRequests = query.getResultList();
        if (!existingRequests.isEmpty()) {
            throw new IllegalStateException("A friend request already exists between these users");
        }

        FriendRequest request = new FriendRequest(sender, receiver);
        request.setStatus(FriendRequest.Status.PENDING);
        em.persist(request);
        
        // Send notification to receiver
        notificationService.sendFriendRequestNotification(receiver, sender);
    }

    public void acceptRequest(Long requestId) {
        FriendRequest request = em.find(FriendRequest.class, requestId);
        if (request != null && request.getStatus() == FriendRequest.Status.PENDING) {
            request.setStatus(FriendRequest.Status.ACCEPTED);
            em.merge(request);
            
            // Send notification to the sender that their request was accepted
            notificationService.sendNotification(
                request.getSender(),
                model.Notification.NotificationType.FRIEND_REQUEST,
                String.format("%s accepted your friend request", request.getReceiver().getName()),
                request.getReceiver().getId(),
                "USER"
            );
        } else {
            throw new IllegalStateException("Friend request not found or not pending");
        }
    }

    public void rejectRequest(Long requestId) {
        FriendRequest request = em.find(FriendRequest.class, requestId);
        if (request != null && request.getStatus() == FriendRequest.Status.PENDING) {
            request.setStatus(FriendRequest.Status.REJECTED);
            em.merge(request);
        } else {
            throw new IllegalStateException("Friend request not found or not pending");
        }
    }

    public List<UserDTO> getUserConnections(Long userId) {
        try {
            // First, find the user to confirm they exist
            User user = em.find(User.class, userId);
            if (user == null) {
                return new ArrayList<>();
            }
            
            // Find all accepted friend requests where this user is either sender or receiver
            TypedQuery<FriendRequest> query = em.createQuery(
                    "SELECT fr FROM FriendRequest fr " +
                            "WHERE (fr.sender.id = :userId OR fr.receiver.id = :userId) " +
                            "AND fr.status = :status",
                    FriendRequest.class);
            query.setParameter("userId", userId);
            query.setParameter("status", FriendRequest.Status.ACCEPTED);
            
            List<FriendRequest> friendRequests = query.getResultList();
            List<UserDTO> friends = new ArrayList<>();
            
            // For each request, add the other user (not the current user)
            for (FriendRequest request : friendRequests) {
                User friend;
                if (request.getSender().getId().equals(userId)) {
                    // If current user is sender, add receiver
                    friend = em.find(User.class, request.getReceiver().getId());
                } else {
                    // If current user is receiver, add sender
                    friend = em.find(User.class, request.getSender().getId());
                }
                
                if (friend != null) {
                    friends.add(UserDTO.fromEntity(friend));
                }
            }
            
            return friends;
        } catch (Exception e) {
            throw new RuntimeException("Error getting user connections: " + e.getMessage(), e);
        }
    }

    public List<FriendRequestDTO> getPendingRequests(Long userId) {
        TypedQuery<FriendRequest> query = em.createQuery(
                "SELECT DISTINCT fr FROM FriendRequest fr " +
                        "JOIN FETCH fr.sender " +
                        "JOIN FETCH fr.receiver " +
                        "WHERE fr.receiver.id = :userId AND fr.status = :status",
                FriendRequest.class);
        query.setParameter("userId", userId);
        query.setParameter("status", FriendRequest.Status.PENDING);

        return query.getResultList().stream()
                .map(FriendRequestDTO::fromEntity)
                .collect(Collectors.toList());
    }
}