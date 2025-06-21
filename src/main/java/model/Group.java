package model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "groups")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Group name is required")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToMany
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "group_admins",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> admins = new HashSet<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupPost> posts = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MembershipRequest> membershipRequests = new ArrayList<>();

    @Column(nullable = false)
    private boolean isOpen = true;

    // Constructors
    public Group() {}

    public Group(String name, String description, User creator, boolean isOpen) {
        this.name = name;
        this.description = description;
        this.creator = creator;
        this.admins.add(creator);
        this.members.add(creator);
        this.isOpen = isOpen;
    }

    // Utility methods
    public void addMember(User user) {
        members.add(user);
    }

    public void removeMember(User user) {
        members.remove(user);
        admins.remove(user);
    }

    public void addAdmin(User user) {
        if (members.contains(user)) {
            admins.add(user);
        }
    }

    public void removeAdmin(User user) {
        if (!user.equals(creator)) {
            admins.remove(user);
        }
    }

    public boolean isAdmin(User user) {
        if (user == null || user.getId() == null) return false;
        
        // First check if the user is the creator (always an admin)
        if (creator != null && creator.getId().equals(user.getId())) {
            return true;
        }
        
        // Then check the admins set
        for (User admin : admins) {
            if (admin.getId().equals(user.getId())) {
                return true;
            }
        }
        
        return false;
    }

    public boolean isMember(User user) {
        if (user == null || user.getId() == null) return false;
        
        // Check the members set by ID
        for (User member : members) {
            if (member.getId().equals(user.getId())) {
                return true;
            }
        }
        
        return false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Set<User> getMembers() { return members; }
    public void setMembers(Set<User> members) { this.members = members; }

    public Set<User> getAdmins() { return admins; }
    public void setAdmins(Set<User> admins) { this.admins = admins; }

    public List<GroupPost> getPosts() { return posts; }
    public void setPosts(List<GroupPost> posts) { this.posts = posts; }

    public boolean isOpen() { return isOpen; }
    public void setOpen(boolean open) { isOpen = open; }

    public List<MembershipRequest> getMembershipRequests() { return membershipRequests; }
    public void setMembershipRequests(List<MembershipRequest> requests) { this.membershipRequests = requests; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Group group = (Group) o;
        return id != null && id.equals(group.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}