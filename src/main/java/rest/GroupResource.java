package rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.RolesAllowed;
import model.*;import service.GroupService;import service.UserService;import java.util.List;import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;
import java.util.HashMap;

@Path("/groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GroupResource {

    @EJB
    private GroupService groupService;

    @EJB
    private UserService userService;

    @PersistenceContext
    private EntityManager em;

    public static class CreateGroupRequest {
        public String name;
        public String description;
        public boolean isOpen;
    }

    public static class GroupPostRequest {
        public String content;
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

    @POST
    @RolesAllowed({"USER", "ADMIN"})
    public Response createGroup(@HeaderParam("Authorization") String authHeader, CreateGroupRequest request) {
        try {
            User user = validateToken(authHeader);
            Group group = groupService.createGroup(request.name, request.description, user, request.isOpen);
            return Response.status(Response.Status.CREATED)
                    .entity(GroupDTO.fromEntity(group))
                    .build();
        } catch (NotAuthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus())
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error creating group: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @RolesAllowed({"USER", "ADMIN"})
    public Response getAllGroups() {
        List<Group> groups = groupService.findAll();
        List<GroupDTO> groupDTOs = groups.stream()
                .map(GroupDTO::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(groupDTOs).build();
    }

    @GET
    @Path("/{groupId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getGroup(@HeaderParam("Authorization") String token, @PathParam("groupId") Long groupId) {
        try {
            User user = validateToken(token);
            Group group = groupService.findById(groupId);

            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Group not found")
                        .build();
            }

            return Response.ok(GroupDTO.fromEntity(group)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error retrieving group: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/{groupId}/join")
    @RolesAllowed({"USER", "ADMIN"})
    public Response joinGroup(@HeaderParam("Authorization") String token, @PathParam("groupId") Long groupId) {
        try {
            User user = validateToken(token);
            Group group = groupService.findById(groupId);

            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Group not found")
                        .build();
            }

            MembershipRequest request = groupService.requestToJoin(user, group);

            if (request == null) {
                return Response.ok(GroupDTO.fromEntity(group)).build();
            } else {
                return Response.status(Response.Status.ACCEPTED)
                        .entity("Join request submitted and pending approval")
                        .build();
            }
        } catch (NotAuthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error joining group: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/{groupId}/leave")
    @RolesAllowed({"USER", "ADMIN"})
    public Response leaveGroup(@HeaderParam("Authorization") String token, @PathParam("groupId") Long groupId) {
        try {
            User user = validateToken(token);
            Group group = groupService.findById(groupId);

            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Group not found")
                        .build();
            }

            groupService.leaveGroup(user, group);
            return Response.ok(GroupDTO.fromEntity(group)).build();
        } catch (NotAuthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error leaving group: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/{groupId}/posts")
    @RolesAllowed({"USER", "ADMIN"})
    public Response createPost(
            @HeaderParam("Authorization") String token,
            @PathParam("groupId") Long groupId,
            GroupPostRequest postRequest) {
        try {
            User user = validateToken(token);
            Group group = groupService.findById(groupId);

            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Group not found")
                        .build();
            }

            GroupPost post = groupService.createPost(user, group, postRequest.content);
            return Response.status(Response.Status.CREATED)
                    .entity(GroupDTO.GroupPostDTO.fromEntity(post))
                    .build();
        } catch (NotAuthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error creating post: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/{groupId}/posts")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getGroupPosts(
            @HeaderParam("Authorization") String token,
            @PathParam("groupId") Long groupId) {
        try {
            User user = validateToken(token);
            Group group = groupService.findById(groupId);

            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Group not found")
                        .build();
            }

            List<GroupPost> posts = groupService.getGroupPosts(group, user);
            List<GroupDTO.GroupPostDTO> postDTOs = posts.stream()
                    .map(GroupDTO.GroupPostDTO::fromEntity)
                    .collect(Collectors.toList());
            return Response.ok(postDTOs).build();
        } catch (NotAuthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error retrieving posts: " + e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("/{groupId}/posts/{postId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response deletePost(
            @HeaderParam("Authorization") String token,
            @PathParam("groupId") Long groupId,
            @PathParam("postId") Long postId) {
        try {
            User user = validateToken(token);
            Group group = groupService.findById(groupId);

            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Group not found")
                        .build();
            }

            // Find the post directly from database instead of using the group's collection
            GroupPost post;
            try {
                post = em.createQuery(
                        "SELECT p FROM GroupPost p JOIN FETCH p.user JOIN FETCH p.group WHERE p.id = :postId AND p.group.id = :groupId", 
                        GroupPost.class)
                        .setParameter("postId", postId)
                        .setParameter("groupId", groupId)
                        .getSingleResult();
            } catch (jakarta.persistence.NoResultException e) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Post not found")
                        .build();
            }

            groupService.deletePost(user, post);
            return Response.ok("Post deleted successfully").build();
        } catch (NotAuthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error deleting post: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/{groupId}/members/{userId}/promote")
    @RolesAllowed({"USER", "ADMIN"})
    public Response promoteToAdmin(
            @HeaderParam("Authorization") String token,
            @PathParam("groupId") Long groupId,
            @PathParam("userId") Long userId) {
        try {
            User admin = validateToken(token);
            Group group = groupService.findById(groupId);
            User user = userService.findById(userId);

            if (group == null || user == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Group or user not found")
                        .build();
            }

            groupService.promoteToAdmin(admin, user, group);
            return Response.ok(GroupDTO.fromEntity(group)).build();
        } catch (NotAuthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error promoting user: " + e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("/{groupId}/members/{userId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response removeMember(
            @HeaderParam("Authorization") String token,
            @PathParam("groupId") Long groupId,
            @PathParam("userId") Long userId) {
        try {
            User admin = validateToken(token);
            Group group = groupService.findById(groupId);
            User user = userService.findById(userId);

            if (group == null || user == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Group or user not found")
                        .build();
            }

            groupService.removeFromGroup(admin, user, group);
            return Response.ok(GroupDTO.fromEntity(group)).build();
        } catch (NotAuthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error removing user: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/{groupId}/requests")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getPendingRequests(
            @HeaderParam("Authorization") String token,
            @PathParam("groupId") Long groupId) {
        try {
            User admin = validateToken(token);
            
            Group group = groupService.findById(groupId);
            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Group not found")
                        .build();
            }
            
            List<MembershipRequest> requests = groupService.getPendingRequestsByIds(groupId, admin.getId());
            
            // Convert to simple map structure to avoid circular reference serialization issues
            List<Map<String, Object>> simplifiedRequests = requests.stream()
                .map(req -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", req.getId());
                    map.put("userId", req.getUser().getId());
                    map.put("userName", req.getUser().getName());
                    map.put("groupId", req.getGroupId());
                    map.put("status", req.getStatus().toString());
                    map.put("createdAt", req.getCreatedAt().toString());
                    return map;
                })
                .collect(Collectors.toList());
                
            return Response.ok(simplifiedRequests).build();
        } catch (NotAuthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error retrieving requests: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/{groupId}/requests/{requestId}/approve")
    @RolesAllowed({"USER", "ADMIN"})
    public Response approveRequest(
            @HeaderParam("Authorization") String token,
            @PathParam("groupId") Long groupId,
            @PathParam("requestId") Long requestId) {
        try {
            User admin = validateToken(token);
            
            Group group = groupService.findById(groupId);
            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Group not found")
                        .build();
            }
            
            // Use a JOIN FETCH query to eagerly load both the group and user
            MembershipRequest request;
            try {
                request = em.createQuery(
                        "SELECT r FROM MembershipRequest r JOIN FETCH r.group JOIN FETCH r.user WHERE r.id = :requestId", 
                        MembershipRequest.class)
                        .setParameter("requestId", requestId)
                        .getSingleResult();
            } catch (jakarta.persistence.NoResultException e) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Request not found")
                        .build();
            }
            
            if (!request.getGroup().getId().equals(groupId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Request not found")
                        .build();
            }

            groupService.approveMembershipRequest(request, admin);
            return Response.ok(GroupDTO.fromEntity(group)).build();
        } catch (NotAuthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error approving request: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/{groupId}/requests/{requestId}/reject")
    @RolesAllowed({"USER", "ADMIN"})
    public Response rejectRequest(
            @HeaderParam("Authorization") String token,
            @PathParam("groupId") Long groupId,
            @PathParam("requestId") Long requestId) {
        try {
            User admin = validateToken(token);
            
            Group group = groupService.findById(groupId);
            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Group not found")
                        .build();
            }

            // Use a JOIN FETCH query to eagerly load both the group and user
            MembershipRequest request;
            try {
                request = em.createQuery(
                        "SELECT r FROM MembershipRequest r JOIN FETCH r.group JOIN FETCH r.user WHERE r.id = :requestId", 
                        MembershipRequest.class)
                        .setParameter("requestId", requestId)
                        .getSingleResult();
            } catch (jakarta.persistence.NoResultException e) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Request not found")
                        .build();
            }
            
            if (!request.getGroup().getId().equals(groupId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Request not found")
                        .build();
            }

            groupService.rejectMembershipRequest(request, admin);
            return Response.ok(GroupDTO.fromEntity(group)).build();
        } catch (NotAuthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error rejecting request: " + e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("/{groupId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response deleteGroup(
            @HeaderParam("Authorization") String token,
            @PathParam("groupId") Long groupId) {
        try {
            User admin = validateToken(token);
            Group group = groupService.findById(groupId);

            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Group not found")
                        .build();
            }

            groupService.deleteGroup(admin, group);
            return Response.ok("Group deleted successfully").build();
        } catch (NotAuthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error deleting group: " + e.getMessage())
                    .build();
        }
    }
}