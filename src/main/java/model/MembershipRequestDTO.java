package model;

import java.time.LocalDateTime;

public class MembershipRequestDTO {
    private Long id;
    private Long userId;
    private String userName;
    private Long groupId;
    private String groupName;
    private String status;
    private LocalDateTime createdAt;

    public static MembershipRequestDTO fromEntity(MembershipRequest request) {
        MembershipRequestDTO dto = new MembershipRequestDTO();
        dto.id = request.getId();
        
        if (request.getUser() != null) {
            dto.userId = request.getUser().getId();
            dto.userName = request.getUser().getName();
        }
        
        dto.groupId = request.getGroupId();
        if (request.getGroup() != null) {
            dto.groupName = request.getGroup().getName();
        }
        
        dto.status = request.getStatus().toString();
        dto.createdAt = request.getCreatedAt();
        
        return dto;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 