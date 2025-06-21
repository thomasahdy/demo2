package rest;

import jakarta.ejb.EJB;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.RolesAllowed;
import model.Comment;
import model.Post;
import model.User;
import service.PostService;
import service.UserService;
import java.util.List;
import java.time.LocalDateTime;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import service.NotificationService;

@Path("/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class PostResource {

    @EJB
    private PostService postService;

    @EJB
    private UserService userService;

    @EJB
    private NotificationService notificationService;

    @PersistenceContext
    private EntityManager em;

    public static class PostDTO {
        private String content;

        // Default constructor for JSON deserialization
        public PostDTO() {}

        // Constructor for convenience
        public PostDTO(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class PostResponseDTO {
        private Long id;
        private String content;
        private Long authorId;
        private String authorName;
        private int likes;
        private int commentCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static PostResponseDTO fromEntity(Post post) {
            PostResponseDTO dto = new PostResponseDTO();
            dto.setId(post.getId());
            dto.setContent(post.getContent());
            dto.setAuthorId(post.getUser().getId());
            dto.setAuthorName(post.getUser().getName());
            dto.setLikes(post.getLikes());
            dto.setCommentCount(post.getComments().size());
            dto.setCreatedAt(post.getCreatedAt());
            dto.setUpdatedAt(post.getUpdatedAt());
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

        public int getLikes() { return likes; }
        public void setLikes(int likes) { this.likes = likes; }

        public int getCommentCount() { return commentCount; }
        public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
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
    public Response createPost(@HeaderParam("Authorization") String token, PostDTO postDTO) {
        try {
            if (postDTO == null || postDTO.getContent() == null || postDTO.getContent().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Post content is required")
                        .build();
            }

            User user = validateToken(token);
            Post post = postService.createPost(postDTO.getContent(), user);
            return Response.status(Response.Status.CREATED)
                    .entity(PostResponseDTO.fromEntity(post))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error creating post: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/feed")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getFeed(@HeaderParam("Authorization") String token) {
        try {
            User user = validateToken(token);
            List<Post> posts = userService.getFeedPosts(user.getId());
            List<PostResponseDTO> postDTOs = posts.stream()
                    .map(PostResponseDTO::fromEntity)
                    .toList();
            return Response.ok(postDTOs).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error retrieving feed: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/{postId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getPost(@HeaderParam("Authorization") String token, @PathParam("postId") Long postId) {
        try {
            validateToken(token);
            Post post = em.find(Post.class, postId);
            if (post == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Post not found")
                        .build();
            }
            return Response.ok(PostResponseDTO.fromEntity(post)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error retrieving post: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/{postId}/edit")
    @RolesAllowed({"USER", "ADMIN"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Response editPost(
            @HeaderParam("Authorization") String token,
            @PathParam("postId") Long postId,
            PostDTO postDTO) {
        try {
            if (postDTO == null || postDTO.getContent() == null || postDTO.getContent().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Post content is required")
                        .build();
            }

            User user = validateToken(token);
            Post post = em.find(Post.class, postId);
            
            if (post == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Post not found")
                        .build();
            }
            
            // Check if the user is the owner of the post or an admin
            boolean isOwner = post.getUser().getId().equals(user.getId());
            boolean isAdmin = user.hasRole("ADMIN");
            
            if (!isOwner && !isAdmin) {
                String errorMsg = "Only post owner or admin can edit posts. " +
                        "Post owner ID: " + post.getUser().getId() + 
                        ", Current user ID: " + user.getId() + 
                        ", Has admin role: " + isAdmin;
                
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(errorMsg)
                        .build();
            }
            
            // Let the service handle the transaction
            Post updatedPost = postService.editPost(postId, postDTO.getContent());
            
            return Response.ok(PostResponseDTO.fromEntity(updatedPost)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error editing post: " + e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("/{postId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response deletePost(
            @HeaderParam("Authorization") String token,
            @PathParam("postId") Long postId) {
        try {
            User user = validateToken(token);
            Post post = em.find(Post.class, postId);

            if (post == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Post not found")
                        .build();
            }

            // Only post owner or admin can delete
            if (!post.getUser().equals(user) && !user.hasRole("ADMIN")) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Only post owner or admin can delete posts")
                        .build();
            }

            em.remove(post);
            return Response.ok("Post deleted successfully").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error deleting post: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/{postId}/like")
    @RolesAllowed({"USER", "ADMIN"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Response likePost(
            @HeaderParam("Authorization") String token,
            @PathParam("postId") Long postId) {
        try {
            User user = validateToken(token);
            
            // Use the service method to manage the transaction and get the updated post
            Post post = postService.likePost(postId);
            
            // Send notification to post owner
            if (!post.getUser().getId().equals(user.getId())) {
                notificationService.sendPostLikeNotification(post.getUser(), user, post.getId());
            }
            
            return Response.ok(PostResponseDTO.fromEntity(post)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error liking post: " + e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("/{postId}/like")
    @RolesAllowed({"USER", "ADMIN"})
    public Response unlikePost(
            @HeaderParam("Authorization") String token,
            @PathParam("postId") Long postId) {
        try {
            User user = validateToken(token);
            Post post = em.find(Post.class, postId);

            if (post == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Post not found")
                        .build();
            }

            post.decrementLikes();
            em.merge(post);
            return Response.ok(PostResponseDTO.fromEntity(post)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error unliking post: " + e.getMessage())
                    .build();
        }
    }

    public static class CommentDTO {
        private String content;

        public CommentDTO() {}

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    @POST
    @Path("/{postId}/comment")
    @RolesAllowed({"USER", "ADMIN"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Response addComment(
            @HeaderParam("Authorization") String token,
            @PathParam("postId") Long postId,
            CommentDTO commentDTO) {
        try {
            if (commentDTO == null || commentDTO.getContent() == null || commentDTO.getContent().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Comment content is required")
                        .build();
            }

            User user = validateToken(token);
            
            // Let the PostService handle the transaction
            Post updatedPost = postService.addComment(postId, user, commentDTO.getContent());
            
            // Send notification to post owner if the commenter is not the owner
            if (!updatedPost.getUser().getId().equals(user.getId())) {
                notificationService.sendPostCommentNotification(updatedPost.getUser(), user, updatedPost.getId());
            }
            
            return Response.ok(PostResponseDTO.fromEntity(updatedPost)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error adding comment: " + e.getMessage())
                    .build();
        }
    }
}