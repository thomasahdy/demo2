package model;

import java.time.LocalDateTime;

public class FriendRequestDTO {
    private Long id;
    private String senderEmail;
    private String receiverEmail;
    private FriendRequest.Status status;
    private LocalDateTime createdAt;

    public static FriendRequestDTO fromEntity(FriendRequest request) {
        FriendRequestDTO dto = new FriendRequestDTO();
        dto.setId(request.getId());
        dto.setSenderEmail(request.getSender().getEmail());
        dto.setReceiverEmail(request.getReceiver().getEmail());
        dto.setStatus(request.getStatus());
        dto.setCreatedAt(request.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getReceiverEmail() { return receiverEmail; }
    public void setReceiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; }

    public FriendRequest.Status getStatus() { return status; }
    public void setStatus(FriendRequest.Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}