package model;

public class GroupPostRequest {
    private String content;
    
    // Default constructor required for JSON binding
    public GroupPostRequest() {
    }
    
    public GroupPostRequest(String content) {
        this.content = content;
    }
    
    // Getters and setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
} 