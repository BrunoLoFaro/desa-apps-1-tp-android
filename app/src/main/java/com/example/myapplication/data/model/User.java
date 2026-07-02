package com.example.myapplication.data.model;

public class User {
    public final long id;
    public final String email;
    public final String firstName;
    public final String lastName;

    public User(long id, String email, String firstName, String lastName) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
