package com.afitnerd.tnra.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@DiscriminatorValue("PERSONAL")
public class PersonalStatDefinition extends StatDefinition {

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public PersonalStatDefinition() {}

    public PersonalStatDefinition(String name, String label, String emoji, int displayOrder, User user) {
        super(name, label, emoji, displayOrder);
        this.user = user;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
