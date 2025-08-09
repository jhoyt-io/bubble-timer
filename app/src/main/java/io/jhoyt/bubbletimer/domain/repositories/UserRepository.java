package io.jhoyt.bubbletimer.domain.repositories;

import io.jhoyt.bubbletimer.domain.entities.User;

import java.util.List;

/**
 * Repository interface for User domain entities.
 * This interface defines the contract for user data access operations.
 * Implementations will handle the actual data storage and retrieval.
 */
public interface UserRepository {
    
    // Query methods
    
    /**
     * Get current authenticated user
     * @return Current user, or null if not authenticated
     */
    User getCurrentUser();
    
    /**
     * Get user by ID
     * @param id User ID
     * @return User with the specified ID, or null if not found
     */
    User getUserById(String id);
    
    /**
     * Get user by username
     * @param username Username
     * @return User with the specified username, or null if not found
     */
    User getUserByUsername(String username);
    
    /**
     * Get user by email
     * @param email Email address
     * @return User with the specified email, or null if not found
     */
    User getUserByEmail(String email);
    
    /**
     * Get all users
     * @return List of all users
     */
    List<User> getAllUsers();
    
    /**
     * Search users by username (partial match)
     * @param username Username to search for
     * @return List of users matching the username
     */
    List<User> searchUsersByUsername(String username);
    
    // Command methods
    
    /**
     * Save a new user
     * @param user User to save
     */
    void saveUser(User user);
    
    /**
     * Update an existing user
     * @param user User to update
     */
    void updateUser(User user);
    
    /**
     * Delete a user by ID
     * @param id User ID to delete
     */
    void deleteUser(String id);
    
    /**
     * Delete all users
     */
    void deleteAllUsers();
    
    // Authentication methods
    
    /**
     * Authenticate a user
     * @param username Username
     * @param password Password
     * @return Authenticated user, or null if authentication failed
     */
    User authenticateUser(String username, String password);
    
    /**
     * Logout current user
     */
    void logoutCurrentUser();
    
    /**
     * Check if user is authenticated
     * @return true if a user is currently authenticated
     */
    boolean isUserAuthenticated();
    
    /**
     * Update user's last login time
     * @param userId User ID
     */
    void updateUserLastLogin(String userId);
    
    // Observable methods (for reactive programming)
    
    /**
     * Observe current user
     * @return Observable current user
     */
    Observable<User> observeCurrentUser();
    
    /**
     * Observe user by ID
     * @param id User ID
     * @return Observable user
     */
    Observable<User> observeUserById(String id);
    
    /**
     * Simple Observable interface for reactive programming
     * This is a simplified version that can be implemented without Android dependencies
     */
    interface Observable<T> {
        /**
         * Subscribe to changes
         * @param observer Observer to notify of changes
         */
        void subscribe(Observer<T> observer);
        
        /**
         * Unsubscribe from changes
         * @param observer Observer to remove
         */
        void unsubscribe(Observer<T> observer);
        
        /**
         * Get current value
         * @return Current value
         */
        T getValue();
    }
    
    /**
     * Observer interface for reactive programming
     */
    interface Observer<T> {
        /**
         * Called when the observed value changes
         * @param value New value
         */
        void onChanged(T value);
    }
}
