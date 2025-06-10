package com.example.demo.entity;

/**
 * User entity.
 */
public class User {
    /**
     * User id.
     */
    private Long id;
    /**
     * User name.
     */
    private String name;
    /**
     * User email.
     */
    private String email;

    /**
     * Default constructor.
     */
    public User() {
    }

    /**
     * Constructor with all fields.
     *
     * @param userId    user id
     * @param userName  user name
     * @param userEmail user email
     */
    public User(final Long userId,
            final String userName,
            final String userEmail) {
        this.id = userId;
        this.name = userName;
        this.email = userEmail;
    }

    /**
     * Get user id.
     *
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * Set user id.
     *
     * @param newId user id
     */
    public void setId(final Long newId) {
        this.id = newId;
    }

    /**
     * Get user name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Set user name.
     *
     * @param name user name
     */
    public void setName(final String newName) {
        this.name = newName;
    }

    /**
     * Get user email.
     *
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set user email.
     *
     * @param email user email
     */
    public void setEmail(final String newEmail) {
        this.email = newEmail;
    }
}
