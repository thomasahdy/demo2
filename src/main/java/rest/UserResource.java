// src/main/java/com/minisocial/rest/UserResource.java
package rest;

import model.User;
import service.UserService;
import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.persistence.NoResultException;
import java.util.HashMap;
import java.util.Map;


@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    public static class LoginDTO {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    @EJB
    private UserService userService;

    @POST
    @Path("/register")
    public Response register(@Valid User user) {
        System.out.println("Received: " + user);
        try {
            User registeredUser = userService.register(user);
            return Response.status(Response.Status.CREATED)
                    .entity(registeredUser)
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Registration failed: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/{email}")
    public Response getUser(@PathParam("email") String email) {
        try {
            User user = userService.findByEmail(email);
            return Response.ok(user).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("User not found")
                    .build();
        }
    }
    
    @GET
    public Response getUserByEmail(@QueryParam("email") String email) {
        if (email == null || email.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Email parameter is required")
                    .build();
        }
        
        try {
            User user = userService.findByEmail(email);
            return Response.ok(user).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("User not found")
                    .build();
        }
    }
    
    @POST
    @Path("/login")
    public Response login(LoginDTO loginDTO) {
        try {
            User user = userService.login(loginDTO);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Login successful.");
            response.put("token", user.getAuthToken());
            return Response.ok(response).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Invalid credentials"))
                    .build();
        }
    }

    @PUT
    @Path("/{userId}/update")
    public Response updateProfile(
            @PathParam("userId") Long userId,
            @Valid User updatedUser) {
        try {
            userService.updateProfile(userId, updatedUser);
            return Response.ok()
                    .entity(Map.of("message", "Profile updated successfully."))
                    .build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "User not found"))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", e.getMessage()))
                    .build();
        }
    }
}