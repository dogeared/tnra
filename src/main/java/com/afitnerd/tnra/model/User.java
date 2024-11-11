package com.afitnerd.tnra.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @JsonView(JsonViews.Sparse.class)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator="native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    @JsonView(JsonViews.Sparse.class)
    private String firstName;

    @JsonView(JsonViews.Sparse.class)
    private String lastName;

    @JsonView(JsonViews.Sparse.class)
    private String email;

    @JsonView(JsonViews.Sparse.class)
    private String profileImage;

    @JsonView(JsonViews.Sparse.class)
    private String phoneNumber;

    @JsonView(JsonViews.Sparse.class)
    private String textEmailSuffix;

    @JsonView(JsonViews.Full.class)
    private Boolean active;

    // TODO - need to uncouple this from slack
    @JsonView(JsonViews.Full.class)
    @Column(nullable = false)
    private String slackUsername;

    @JsonView(JsonViews.Full.class)
    @Column(nullable = false)
    private String slackUserId;

    @JsonView(JsonViews.Full.class)
    @Column(length = 4000)
    private String pqAccessToken;

    @JsonView(JsonViews.Full.class)
    @Column(length = 4000)
    private String pqRefreshToken;

    @JsonView(JsonViews.Full.class)
    @JsonIgnoreProperties("user")
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Post> posts;

    public User() {}

    public User(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public User(String slackUserId, String slackUsername) {
        this.slackUserId = slackUserId;
        this.slackUsername = slackUsername;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getTextEmailSuffix() {
        return textEmailSuffix;
    }

    public void setTextEmailSuffix(String textEmailSuffix) {
        this.textEmailSuffix = textEmailSuffix;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public String getSlackUsername() {
        return slackUsername;
    }

    public void setSlackUsername(String slackUsername) {
        this.slackUsername = slackUsername;
    }

    public String getSlackUserId() {
        return slackUserId;
    }

    public void setSlackUserId(String slackUserId) {
        this.slackUserId = slackUserId;
    }

    public String getPqAccessToken() {
        return pqAccessToken;
    }

    public void setPqAccessToken(String pqAccessToken) {
        this.pqAccessToken = pqAccessToken;
    }

    public String getPqRefreshToken() {
        return pqRefreshToken;
    }

    public void setPqRefreshToken(String pqRefreshToken) {
        this.pqRefreshToken = pqRefreshToken;
    }
}
