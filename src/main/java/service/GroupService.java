package service;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;
import model.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

@Stateless
public class GroupService {

    @PersistenceContext
    private EntityManager em;

    @EJB
    private NotificationService notificationService;

    public Group createGroup(String name, String description, User creator, boolean isOpen) {
        Group group = new Group(name, description, creator, isOpen);
        em.persist(group);
        return group;
    }

    public Group findById(Long id) {
        try {
            // Instead of detaching, let's use a fully managed entity and load all necessary collections
            // within the transaction
            
            // First get the basic entity
            Group group = em.find(Group.class, id);
            if (group == null) {
                return null;
            }
            
            // Now force-initialize all collections within the same transaction context
            
            // Load members
            em.createQuery(
                    "SELECT u FROM User u JOIN u.groups g WHERE g.id = :groupId",
                    User.class)
                    .setParameter("groupId", id)
                    .getResultList();
            
            // Load admins
            em.createQuery(
                    "SELECT u FROM User u JOIN u.adminGroups g WHERE g.id = :groupId",
                    User.class)
                    .setParameter("groupId", id)
                    .getResultList();
            
            // Load posts
            em.createQuery(
                    "SELECT p FROM GroupPost p WHERE p.group.id = :groupId",
                    GroupPost.class)
                    .setParameter("groupId", id)
                    .getResultList();
            
            // Load membership requests
            em.createQuery(
                    "SELECT r FROM MembershipRequest r WHERE r.group.id = :groupId",
                    MembershipRequest.class)
                    .setParameter("groupId", id)
                    .getResultList();
            
            // Explicitly initialize all collections by accessing them
            group.getMembers().size();
            group.getAdmins().size();
            group.getPosts().size();
            group.getMembershipRequests().size();
            
            // Debug info
            System.out.println("DEBUG - GroupService.findById:");
            System.out.println("Group ID: " + group.getId());
            System.out.println("Creator ID: " + (group.getCreator() != null ? group.getCreator().getId() : "null"));
            System.out.println("Admin count: " + group.getAdmins().size());
            System.out.println("Member count: " + group.getMembers().size());
            System.out.println("Post count: " + group.getPosts().size());
            
            return group;
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching group: " + e.getMessage(), e);
        }
    }

    public List<Group> findAll() {
        try {
            // First get all group IDs
            List<Long> groupIds = em.createQuery(
                    "SELECT g.id FROM Group g", Long.class)
                    .getResultList();
            
            // Use our improved findById method to load each group individually
            // with full relationship initialization
            List<Group> groups = new ArrayList<>();
            for (Long id : groupIds) {
                Group group = findById(id);
                if (group != null) {
                    groups.add(group);
                }
            }
            
            return groups;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching groups: " + e.getMessage(), e);
        }
    }

    public MembershipRequest requestToJoin(User user, Group group) {
        // First check if the group is detached (it might be if we used findById which now detaches)
        if (!em.contains(group)) {
            // Get a managed copy of the Group entity
            group = em.find(Group.class, group.getId());
        }
        
        // Check if user is already a member
        if (group.isMember(user)) {
            throw new IllegalStateException("User is already a member of this group");
        }

        if (group.isOpen()) {
            group.addMember(user);
            // No need to merge if we're working with a managed entity now
            notificationService.sendGroupJoinApprovedNotification(user, group.getId());
            return null;
        } else {
            MembershipRequest request = new MembershipRequest(user, group);
            em.persist(request);

            // Notify all group admins
            for (User admin : group.getAdmins()) {
                notificationService.sendGroupJoinRequestNotification(admin, user, group.getId());
            }

            return request;
        }
    }

    public void approveMembershipRequest(MembershipRequest request, User admin) {
        // Use our more reliable ID-based admin check
        if (admin != null && admin.getId() != null && 
            request != null && request.getGroup() != null && request.getGroup().getId() != null) {
            
            boolean isAdmin = isGroupAdmin(admin.getId(), request.getGroup().getId());
            if (!isAdmin) {
                throw new SecurityException("Only group admins can approve membership requests");
            }
        } else if (!request.getGroup().isAdmin(admin)) {
            // Fallback to the original check for backward compatibility
            throw new SecurityException("Only group admins can approve membership requests");
        }

        // Use the request we got, but make sure the group is fully loaded
        // by retrieving a fresh instance from the database
        Group group = findById(request.getGroup().getId());
        User user = em.find(User.class, request.getUser().getId());
        
        // Update the request status
        request.setStatus(MembershipRequest.RequestStatus.APPROVED);
        
        // Add the member to the group (using our fully loaded group)
        group.addMember(user);
        
        // Merge changes back
        em.merge(request);
        em.merge(group);

        notificationService.sendGroupJoinApprovedNotification(user, group.getId());
    }

    public void rejectMembershipRequest(MembershipRequest request, User admin) {
        // Use our more reliable ID-based admin check
        if (admin != null && admin.getId() != null && 
            request != null && request.getGroup() != null && request.getGroup().getId() != null) {
            
            boolean isAdmin = isGroupAdmin(admin.getId(), request.getGroup().getId());
            if (!isAdmin) {
                throw new SecurityException("Only group admins can reject membership requests");
            }
        } else if (!request.getGroup().isAdmin(admin)) {
            // Fallback to the original check for backward compatibility
            throw new SecurityException("Only group admins can reject membership requests");
        }

        // Get a fresh copy of the user
        User user = em.find(User.class, request.getUser().getId());
        
        // Update the request status
        request.setStatus(MembershipRequest.RequestStatus.REJECTED);
        em.merge(request);
        
        // Optionally send a notification about the rejection
        notificationService.sendNotification(
            user,
            model.Notification.NotificationType.GROUP_MEMBERSHIP_CHANGED,
            "Your request to join group " + request.getGroup().getName() + " was rejected",
            request.getGroup().getId(),
            "GROUP"
        );
    }

    public void leaveGroup(User user, Group group) {
        if (!group.isMember(user)) {
            throw new IllegalStateException("User is not a member of this group");
        }

        if (group.getCreator().equals(user)) {
            throw new IllegalStateException("Group creator cannot leave the group");
        }

        group.removeMember(user);
        em.merge(group);

        notificationService.sendGroupMembershipChangedNotification(user, "removed", group.getId());
    }

    public GroupPost createPost(User user, Group group, String content) {
        if (!group.isMember(user)) {
            throw new SecurityException("Only group members can create posts");
        }

        GroupPost post = new GroupPost(content, user, group);
        em.persist(post);
        return post;
    }

    public void deletePost(User user, GroupPost post) {
        if (!post.getUser().equals(user) && !post.getGroup().isAdmin(user)) {
            throw new SecurityException("Only post creator or group admin can delete posts");
        }

        // Make sure we have a managed entity before removing
        if (!em.contains(post)) {
            // Get a managed copy from the database
            GroupPost managedPost = em.find(GroupPost.class, post.getId());
            if (managedPost != null) {
                em.remove(managedPost);
            } else {
                throw new IllegalArgumentException("Post not found with ID: " + post.getId());
            }
        } else {
            em.remove(post);
        }
    }

    public void promoteToAdmin(User admin, User user, Group group) {
        if (!group.isAdmin(admin)) {
            throw new SecurityException("Only group admins can promote members");
        }

        if (!group.isMember(user)) {
            throw new IllegalStateException("User must be a group member to be promoted");
        }

        group.addAdmin(user);
        em.merge(group);

        notificationService.sendGroupMembershipChangedNotification(user, "promoted to admin", group.getId());
    }

    public void removeFromGroup(User admin, User user, Group group) {
        if (!group.isAdmin(admin)) {
            throw new SecurityException("Only group admins can remove members");
        }

        if (user.equals(group.getCreator())) {
            throw new IllegalStateException("Cannot remove the group creator");
        }

        group.removeMember(user);
        em.merge(group);

        notificationService.sendGroupMembershipChangedNotification(user, "removed from group", group.getId());
    }

    public void deleteGroup(User admin, Group group) {
        if (!group.getCreator().equals(admin)) {
            throw new SecurityException("Only group creator can delete the group");
        }

        em.remove(group);
    }

    public List<GroupPost> getGroupPosts(Group group, User user) {
        if (!group.isMember(user)) {
            throw new SecurityException("Only group members can view posts");
        }

        return em.createQuery(
                        "SELECT p FROM GroupPost p WHERE p.group = :group ORDER BY p.createdAt DESC",
                        GroupPost.class)
                .setParameter("group", group)
                .getResultList();
    }

    /**
     * Check if a user is an admin of a group using direct database queries
     * instead of relying on entity comparisons
     */
    public boolean isGroupAdmin(Long userId, Long groupId) {
        // First check if user is the creator
        Long creatorId = em.createQuery(
                "SELECT g.creator.id FROM Group g WHERE g.id = :groupId", 
                Long.class)
                .setParameter("groupId", groupId)
                .getSingleResult();
                
        if (creatorId != null && creatorId.equals(userId)) {
            return true;
        }
        
        // Then check the admin relationship in the join table
        Long count = em.createQuery(
                "SELECT COUNT(a) FROM Group g JOIN g.admins a WHERE g.id = :groupId AND a.id = :userId",
                Long.class)
                .setParameter("groupId", groupId)
                .setParameter("userId", userId)
                .getSingleResult();
                
        return count > 0;
    }
    
    public List<MembershipRequest> getPendingRequestsByIds(Long groupId, Long userId) {
        // First verify admin status using direct ID check
        boolean isAdmin = isGroupAdmin(userId, groupId);
        
        if (!isAdmin) {
            throw new SecurityException("Only group admins can view pending requests");
        }
        
        // Get the requests directly using IDs with JOIN FETCH to eagerly load the User
        return em.createQuery(
                "SELECT r FROM MembershipRequest r JOIN FETCH r.user WHERE r.group.id = :groupId AND r.status = :status",
                MembershipRequest.class)
                .setParameter("groupId", groupId)
                .setParameter("status", MembershipRequest.RequestStatus.PENDING)
                .getResultList();
    }

    public List<MembershipRequest> getPendingRequests(Group group, User admin) {
        // Debug logging
        System.out.println("DEBUG - GroupService.getPendingRequests:");
        System.out.println("User ID: " + (admin != null ? admin.getId() : "null"));
        System.out.println("Group ID: " + (group != null ? group.getId() : "null"));
        
        // Use the more reliable ID-based method
        if (group != null && admin != null && group.getId() != null && admin.getId() != null) {
            return getPendingRequestsByIds(group.getId(), admin.getId());
        }
        
        // Fallback to original logic for backward compatibility
        if (!group.isAdmin(admin)) {
            throw new SecurityException("Only group admins can view pending requests");
        }

        return em.createQuery(
                "SELECT r FROM MembershipRequest r WHERE r.group = :group AND r.status = :status",
                MembershipRequest.class)
            .setParameter("group", group)
            .setParameter("status", MembershipRequest.RequestStatus.PENDING)
            .getResultList();
    }
}