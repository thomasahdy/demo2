package model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "group_posts")
@DiscriminatorValue("GROUP")
public class GroupPost extends Post {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @NotNull(message = "Group is required")
    private Group group;

    public GroupPost() {
        super();
    }

    public GroupPost(String content, User user, Group group) {
        super(content, user);
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return "GroupPost{" +
                "id=" + getId() +
                ", content='" + getContent() + '\'' +
                ", user=" + getUser().getId() +
                ", group=" + group.getId() +
                ", likes=" + getLikes() +
                ", comments=" + getComments().size() +
                ", createdAt=" + getCreatedAt() +
                ", updatedAt=" + getUpdatedAt() +
                '}';
    }
}