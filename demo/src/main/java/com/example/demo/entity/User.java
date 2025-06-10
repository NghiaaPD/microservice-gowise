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
     * @param id    user id
     * @param name  user name
     * @param email user email
     */
    public User(final Long id, final String name, final String email) {
        this.id = id;
        this.name = name;
        this.email = email;
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
     * @param id user id
     */
    public void setId(final Long id) {
        this.id = id;
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
    public void setName(final String name) {
        this.name = name;
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
    public void setEmail(final String email) {
        this.email = email;
    }
}
