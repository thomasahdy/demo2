package model;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

public class GroupDTO {
    private Long id;
    private String name;
    private String description;
    private boolean isOpen;
    private Long creatorId;
    private String creatorName;
    private int memberCount;
    private int adminCount;
    private int postCount;
    private List<Long> memberIds;
    private List<Long> adminIds;
    private List<GroupPostDTO> recentPosts;
    private int pendingRequestsCount;
    private LocalDateTime createdAt;

    public static class GroupPostDTO {
        private Long id;
        private String content;
        private Long authorId;
        private String authorName;
        private LocalDateTime createdAt;
        private int likeCount;
        private int commentCount;

        public static GroupPostDTO fromEntity(GroupPost post) {
            GroupPostDTO dto = new GroupPostDTO();
            dto.setId(post.getId());
            dto.setContent(post.getContent());
            dto.setAuthorId(post.getUser().getId());
            dto.setAuthorName(post.getUser().getName());
            dto.setCreatedAt(post.getCreatedAt());
            dto.setLikeCount(post.getLikes());
            dto.setCommentCount(post.getComments().size());
            return dto;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public Long getAuthorId() { return authorId; }
        public void setAuthorId(Long authorId) { this.authorId = authorId; }

        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public int getLikeCount() { return likeCount; }
        public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

        public int getCommentCount() { return commentCount; }
        public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    }

    public static GroupDTO fromEntity(Group group) {
        GroupDTO dto = new GroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setOpen(group.isOpen());
        
        try {
            // Safely access potential lazy-loaded relationships
            if (group.getCreator() != null) {
                dto.setCreatorId(group.getCreator().getId());
                dto.setCreatorName(group.getCreator().getName());
            }
            
            // Set counts with null checks
            dto.setMemberCount(group.getMembers() != null ? group.getMembers().size() : 0);
            dto.setAdminCount(group.getAdmins() != null ? group.getAdmins().size() : 0);
            dto.setPostCount(group.getPosts() != null ? group.getPosts().size() : 0);
    
            // Get member IDs with null check
            if (group.getMembers() != null) {
                dto.setMemberIds(group.getMembers().stream()
                    .map(User::getId)
                    .collect(Collectors.toList()));
            }
    
            // Get admin IDs with null check
            if (group.getAdmins() != null) {
                dto.setAdminIds(group.getAdmins().stream()
                    .map(User::getId)
                    .collect(Collectors.toList()));
            }
    
            // Get recent posts with null check (limit to 10)
            if (group.getPosts() != null) {
                dto.setRecentPosts(group.getPosts().stream()
                    .limit(10)
                    .map(GroupPostDTO::fromEntity)
                    .collect(Collectors.toList()));
            }
    
            // Count pending requests with null check
            if (group.getMembershipRequests() != null) {
                dto.setPendingRequestsCount((int) group.getMembershipRequests().stream()
                    .filter(r -> r.getStatus() == MembershipRequest.RequestStatus.PENDING)
                    .count());
            }
        } catch (Exception e) {
            // Log the error (or add a message) and continue with partial data
            System.err.println("Error while creating GroupDTO: " + e.getMessage());
        }

        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isOpen() { return isOpen; }
    public void setOpen(boolean open) { isOpen = open; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public int getAdminCount() { return adminCount; }
    public void setAdminCount(int adminCount) { this.adminCount = adminCount; }

    public int getPostCount() { return postCount; }
    public void setPostCount(int postCount) { this.postCount = postCount; }

    public List<Long> getMemberIds() { return memberIds; }
    public void setMemberIds(List<Long> memberIds) { this.memberIds = memberIds; }

    public List<Long> getAdminIds() { return adminIds; }
    public void setAdminIds(List<Long> adminIds) { this.adminIds = adminIds; }

    public List<GroupPostDTO> getRecentPosts() { return recentPosts; }
    public void setRecentPosts(List<GroupPostDTO> recentPosts) { this.recentPosts = recentPosts; }

    public int getPendingRequestsCount() { return pendingRequestsCount; }
    public void setPendingRequestsCount(int pendingRequestsCount) { this.pendingRequestsCount = pendingRequestsCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}