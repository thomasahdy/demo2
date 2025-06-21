package rest;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import model.FriendRequestDTO;
import model.User;
import model.UserDTO;
import service.ConnectionService;
import service.UserService;
import java.util.List;
import java.util.Map;

@Path("/connections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Stateless
public class ConnectionResource {

    @EJB
    private ConnectionService connectionService;

    @EJB
    private UserService userService;

    @POST
    @Path("/requests")
    public Response sendRequest(
            @QueryParam("sender") String senderEmail,
            @QueryParam("receiver") String receiverEmail) {
        try {
            User sender = userService.findByEmail(senderEmail);
            User receiver = userService.findByEmail(receiverEmail);

            if (sender == null || receiver == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "User not found"))
                        .build();
            }

            connectionService.sendRequest(sender, receiver);
            return Response.status(Response.Status.CREATED)
                    .entity(Map.of("message", "Friend request sent successfully"))
                    .build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("message", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Failed to send friend request"))
                    .build();
        }
    }

    @PUT
    @Path("/requests/{id}/accept")
    public Response acceptRequest(@PathParam("id") Long requestId) {
        try {
            connectionService.acceptRequest(requestId);
            return Response.ok(Map.of("message", "Friend request accepted"))
                    .build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/requests/{id}/reject")
    public Response rejectRequest(@PathParam("id") Long requestId) {
        try {
            connectionService.rejectRequest(requestId);
            return Response.ok(Map.of("message", "Friend request rejected"))
                    .build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/friends/{userId}")
    public Response getUserConnections(@PathParam("userId") Long userId) {
        try {
            List<UserDTO> connections = connectionService.getUserConnections(userId);
            return Response.ok(connections).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                        "message", "Failed to get connections",
                        "error", e.getMessage(),
                        "userId", userId
                    ))
                    .build();
        }
    }

    @GET
    @Path("/requests/pending/{userId}")
    public Response getPendingRequests(@PathParam("userId") Long userId) {
        try {
            List<FriendRequestDTO> pendingRequests = connectionService.getPendingRequests(userId);
            return Response.ok(pendingRequests).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Failed to get pending requests"))
                    .build();
        }
    }
}