package service;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import model.User;
import model.Post;
import rest.UserResource.LoginDTO;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Stateless
public class UserService {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public User register(User user) {
        user.setPassword(hashPassword(user.getPassword()));
        em.persist(user);
        return user;
    }

    public User findByEmail(String email) {
        return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getSingleResult();
    }

    public User findById(Long id) {
        return em.find(User.class, id);
    }

    public List<Post> getFeedPosts(Long userId) {
        User user = findById(userId);
        if (user == null) {
            throw new NoResultException("User not found");
        }

        // Get posts from the user and their friends
        List<Post> posts = em.createQuery(
                        "SELECT DISTINCT p FROM Post p " +
                        "WHERE p.user = :user " +
                        "OR p.user IN (SELECT f FROM User u JOIN u.friends f WHERE u = :user) " +
                        "ORDER BY p.createdAt DESC", Post.class)
                .setParameter("user", user)
                .setMaxResults(50)  // Limit to latest 50 posts
                .getResultList();
                
        // Initialize posts by loading their associations individually
        for (Post post : posts) {
            // Manually load the user and comments to avoid lazy loading issues
            if (!em.contains(post)) continue;
            
            // Load user fully
            if (post.getUser() != null) {
                post.getUser().getName();
            }
            
            // Load comments and their users
            em.createQuery(
                "SELECT c FROM Comment c JOIN FETCH c.user WHERE c.post.id = :postId", 
                model.Comment.class)
                .setParameter("postId", post.getId())
                .getResultList();
                
            // Force initialization
            post.getComments().size();
        }
        
        return posts;
    }

    @Transactional
    public User login(LoginDTO loginDTO) {
        try {
            User user = findByEmail(loginDTO.getEmail());

            if (!verifyPassword(loginDTO.getPassword(), user.getPassword())) {
                throw new SecurityException("Invalid password");
            }

            String token = generateAuthToken();
            user.setAuthToken(token);
            em.merge(user);

            return user;
        } catch (NoResultException e) {
            throw new SecurityException("Invalid credentials");
        }
    }

    public User findByAuthToken(String token) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.authToken = :token", User.class)
                    .setParameter("token", token)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Transactional
    public User updateProfile(Long userId, User updatedUser) {
        User existingUser = em.find(User.class, userId);
        if (existingUser == null) {
            throw new NoResultException("User not found");
        }

        if (updatedUser.getName() != null) {
            existingUser.setName(updatedUser.getName());
        }
        if (updatedUser.getBio() != null) {
            existingUser.setBio(updatedUser.getBio());
        }
        if (updatedUser.getEmail() != null) {
            existingUser.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getPassword() != null) {
            existingUser.setPassword(hashPassword(updatedUser.getPassword()));
        }

        return em.merge(existingUser);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    private boolean verifyPassword(String inputPassword, String storedHash) {
        String inputHash = hashPassword(inputPassword);
        return inputHash.equals(storedHash);
    }

    private String generateAuthToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}