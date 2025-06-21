package service;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import model.Comment;
import model.Post;
import model.User;

import java.util.List;

@Stateless
public class PostService {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Post createPost(String content, User user) {
        Post post = new Post(content, user);
        em.persist(post);
        return post;
    }

    @Transactional
    public Post editPost(Long postId, String newContent) {
        Post post = em.find(Post.class, postId);
        if (post != null) {
            post.setContent(newContent);
            
            // Initialize the user to avoid lazy loading issues
            post.getUser().getName(); // Access the user to initialize it
            // Initialize comments to avoid lazy loading issues
            post.getComments().size();
            
            return post;
        } else {
            throw new RuntimeException("Post not found");
        }
    }

    @Transactional
    public Post likePost(Long postId) {
        Post post = em.find(Post.class, postId);
        if (post != null) {
            post.incrementLikes();
            // Initialize the user to avoid lazy loading issues
            post.getUser().getName(); // Access the user to initialize it
            // Initialize comments to avoid lazy loading issues
            post.getComments().size();
            return post;
        } else {
            throw new RuntimeException("Post not found");
        }
    }

    @Transactional
    public Post addComment(Long postId, User user, String text) {
        Post post = em.find(Post.class, postId);
        if (post != null) {
            Comment comment = new Comment();
            comment.setText(text);
            comment.setPost(post);
            comment.setUser(user);
            em.persist(comment);
            
            post.addComment(comment);
            em.merge(post);
            
            // Initialize the user to avoid lazy loading issues
            post.getUser().getName(); // Access the user to initialize it
            // Initialize comments to avoid lazy loading issues
            post.getComments().size();
            
            return post;
        } else {
            throw new RuntimeException("Post not found");
        }
    }

    @Transactional
    public Post createPost(Post post) {
        em.persist(post);
        return post;
    }
}
