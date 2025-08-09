package io.jhoyt.bubbletimer.domain.entities;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a user in the domain layer.
 * This is a pure domain entity with no Android framework dependencies.
 */
public class User {
    private final String id;
    private final String username;
    private final String email;
    private final boolean isAuthenticated;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;

    /**
     * Private constructor to enforce immutability
     */
    private User(String id, String username, String email, boolean isAuthenticated,
                 LocalDateTime lastLoginAt, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.isAuthenticated = isAuthenticated;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
    }

    /**
     * Create a new user
     * @param id User ID
     * @param username Username
     * @param email Email address
     * @return New User instance
     */
    public static User create(String id, String username, String email) {
        LocalDateTime now = LocalDateTime.now();
        return new User(id, username, email, false, null, now);
    }

    /**
     * Create a user from existing data
     * @param id User ID
     * @param username Username
     * @param email Email address
     * @param isAuthenticated Whether the user is authenticated
     * @param lastLoginAt Last login timestamp
     * @param createdAt Creation timestamp
     * @return User instance
     */
    public static User fromData(String id, String username, String email, boolean isAuthenticated,
                               LocalDateTime lastLoginAt, LocalDateTime createdAt) {
        return new User(id, username, email, isAuthenticated, lastLoginAt, createdAt);
    }

    /**
     * Mark the user as authenticated
     * @return New User instance with authenticated status
     */
    public User authenticate() {
        LocalDateTime now = LocalDateTime.now();
        return new User(id, username, email, true, now, createdAt);
    }

    /**
     * Mark the user as unauthenticated
     * @return New User instance with unauthenticated status
     */
    public User logout() {
        return new User(id, username, email, false, lastLoginAt, createdAt);
    }

    /**
     * Update the user's last login time
     * @return New User instance with updated last login time
     */
    public User updateLastLogin() {
        LocalDateTime now = LocalDateTime.now();
        return new User(id, username, email, isAuthenticated, now, createdAt);
    }

    // Getters

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public boolean isAuthenticated() { return isAuthenticated; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Object methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return isAuthenticated == user.isAuthenticated &&
               Objects.equals(id, user.id) &&
               Objects.equals(username, user.username) &&
               Objects.equals(email, user.email) &&
               Objects.equals(lastLoginAt, user.lastLoginAt) &&
               Objects.equals(createdAt, user.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email, isAuthenticated, lastLoginAt, createdAt);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", isAuthenticated=" + isAuthenticated +
                ", lastLoginAt=" + lastLoginAt +
                '}';
    }
}
