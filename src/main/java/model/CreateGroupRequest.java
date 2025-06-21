package model;

import jakarta.json.bind.annotation.JsonbProperty;

public class CreateGroupRequest {
    private String name;
    private String description;
    private boolean isOpen;

    // Default constructor required for JSON binding
    public CreateGroupRequest() {
    }
    
    public CreateGroupRequest(String name, String description, boolean isOpen) {
        this.name = name;
        this.description = description;
        this.isOpen = isOpen;
    }

    // Getters and setters
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }
    
    public String getDescription() { 
        return description; 
    }
    
    public void setDescription(String description) { 
        this.description = description; 
    }
    
    // Standard isOpen method
    public boolean isOpen() { 
        return isOpen; 
    }
    
    // Alternative getter for JSON binding
    @JsonbProperty("isOpen")
    public boolean getIsOpen() {
        return isOpen;
    }
    
    // Setter for boolean property
    @JsonbProperty("isOpen") 
    public void setIsOpen(boolean isOpen) { 
        this.isOpen = isOpen; 
    }
} 