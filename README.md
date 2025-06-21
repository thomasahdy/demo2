# demo2 - Social Network Backend

## Project Description

demo2 is a backend application for a social network, built with Jakarta EE 11, Hibernate, and H2 in-memory database. It provides RESTful APIs for user management, posts, groups, notifications, and connections (friend requests), suitable for use as the backend of a modern social platform.

## Features
- User registration, login, and profile management
- Post creation, editing, deletion, liking, and commenting
- Group creation, joining, leaving, and group post management
- Friend (connection) requests and management
- Notification system for user activities
- Secure endpoints with token-based authentication

## REST API Overview

### User Endpoints (`/api/users`)
- `POST /register` — Register a new user
- `POST /login` — User login (returns token)
- `GET /{email}` — Get user by email
- `PUT /{userId}/update` — Update user profile

### Post Endpoints (`/api/posts`)
- `POST /` — Create a post (auth required)
- `GET /feed` — Get user feed (auth required)
- `GET /{postId}` — Get a specific post
- `POST /{postId}/edit` — Edit a post
- `DELETE /{postId}` — Delete a post
- `POST /{postId}/like` — Like a post
- `DELETE /{postId}/like` — Unlike a post
- `POST /{postId}/comment` — Add a comment

### Group Endpoints (`/api/groups`)
- `POST /` — Create a group
- `GET /` — List all groups
- `GET /{groupId}` — Get group details
- `POST /{groupId}/join` — Join a group
- `POST /{groupId}/leave` — Leave a group
- `POST /{groupId}/posts` — Create a group post
- `GET /{groupId}/posts` — List group posts
- `DELETE /{groupId}/posts/{postId}` — Delete a group post
- `POST /{groupId}/members/{userId}/promote` — Promote member to admin
- `DELETE /{groupId}/members/{userId}` — Remove member
- `GET /{groupId}/requests` — List pending join requests
- `POST /{groupId}/requests/{requestId}/approve` — Approve join request
- `POST /{groupId}/requests/{requestId}/reject` — Reject join request
- `DELETE /{groupId}` — Delete group

### Notification Endpoints (`/api/notifications`)
- `GET /` — List notifications
- `GET /unread` — List unread notifications
- `POST /{notificationId}/read` — Mark notification as read
- `POST /read-all` — Mark all as read

### Connection Endpoints (`/api/connections`)
- `POST /requests` — Send friend request
- `PUT /requests/{id}/accept` — Accept friend request
- `PUT /requests/{id}/reject` — Reject friend request
- `GET /friends/{userId}` — List user connections
- `GET /requests/pending/{userId}` — List pending friend requests

### Hello World Endpoint
- `GET /api/hello-world` — Returns "Hello, World!"

## Technologies Used
- **Jakarta EE 11** (REST, CDI, EJB)
- **Hibernate ORM**
- **H2 Database** (in-memory)
- **JUnit 5** (for testing)
- **Maven** (build tool)

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+

### Build & Run
1. Clone the repository:
   ```sh
   git clone <repo-url>
   cd demo2
   ```
2. Build the project:
   ```sh
   mvn clean package
   ```
3. Deploy the generated WAR (`target/demo2.war`) to a Jakarta EE compatible server (e.g., Payara, WildFly, Open Liberty).

### Configuration
- Database and JPA settings are in `src/main/resources/META-INF/persistence.xml` (uses in-memory H2 by default).
- Beans configuration in `src/main/resources/META-INF/beans.xml`.

## Project Structure
- `src/main/java/rest/` — REST API resources
- `src/main/java/model/` — JPA entities
- `src/main/java/service/` — Business logic
- `src/main/resources/` — Configuration files

## License
This project is for educational/demo purposes. 