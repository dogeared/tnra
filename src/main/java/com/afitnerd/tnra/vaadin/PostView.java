package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PostRepository;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.PostService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@PageTitle("Posts - TNRA")
@Route(value = "posts", layout = MainLayout.class)
@CssImport("./styles/post-view.css")
public class PostView extends VerticalLayout implements AfterNavigationObserver {

    private final PostService postService;
    private final UserService userService;
    private final OidcUserService oidcUserService;
    private final PostRepository postRepository;
    private User currentUser;
    private Post currentPost;
    private List<Post> userPosts = new ArrayList<>();
    
    // UI Components
    private ComboBox<Post> postSelector;
    private Button startNewPostButton;
    private StatsView statsView;
    
    // Intro section
    private TextArea widwytkField;
    private TextField kryptoniteField;
    private TextArea whatAndWhenField;
    
    // Personal section
    private TextArea personalBestField;
    private TextArea personalWorstField;
    
    // Family section
    private TextArea familyBestField;
    private TextArea familyWorstField;
    
    // Work section
    private TextArea workBestField;
    private TextArea workWorstField;

    public PostView(OidcUserService oidcUserService, PostService postService, UserService userService, PostRepository postRepository) {
        this.oidcUserService = oidcUserService;
        this.postService = postService;
        this.userService = userService;
        this.postRepository = postRepository;
        
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setPadding(false);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (event.getLocation().getFirstSegment().equals("posts")) {
            initializeUser();
            createPostView();
        }
    }

    private void initializeUser() {
        if (oidcUserService.isAuthenticated()) {
            String email = oidcUserService.getEmail();
            currentUser = userService.getUserByEmail(email);
            
            if (currentUser == null) {
                Notification.show("User not found. Please contact support.", 5000, Notification.Position.MIDDLE);
                return;
            }
            
            // Load all posts for the user
            userPosts = postRepository.findByUser(currentUser);
            
            // Set current post to in-progress post or first available post
            Optional<Post> inProgressPost = userPosts.stream()
                    .filter(post -> post.getState() == PostState.IN_PROGRESS)
                    .findFirst();
            
            if (inProgressPost.isPresent()) {
                currentPost = inProgressPost.get();
            } else if (!userPosts.isEmpty()) {
                currentPost = userPosts.get(0);
            }
        } else {
            Notification.show("Authentication required.", 5000, Notification.Position.MIDDLE);
        }
    }

    private void createPostView() {
        // Clear existing content
        removeAll();
        
        // Header section with post selector and start new post button
        VerticalLayout headerSection = createHeaderSection();
        
        // Main content sections
        VerticalLayout contentSection = createContentSection();
        
        add(headerSection, contentSection);
    }

    private VerticalLayout createHeaderSection() {
        VerticalLayout header = new VerticalLayout();
        header.setAlignItems(Alignment.CENTER);
        header.setSpacing(false);
        header.setPadding(false);
        header.setWidth("100%");
        header.addClassName("post-header");

        H2 title = new H2("Daily Posts");
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD, "post-title");

        // Post selector and start new post button
        HorizontalLayout controlsRow = new HorizontalLayout();
        controlsRow.setAlignItems(Alignment.CENTER);
        controlsRow.setSpacing(true);
        controlsRow.setPadding(false);
        controlsRow.addClassName("post-controls");

        // Post selector
        postSelector = new ComboBox<>("Select Post");
        postSelector.setWidth("300px");
        postSelector.setItemLabelGenerator(this::generatePostLabel);
        
        // Set items to user posts only
        postSelector.setItems(userPosts);
        
        // Set initial selection to null (empty)
        postSelector.setValue(null);
        
        postSelector.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                currentPost = e.getValue();
                loadPostData();
                updateReadOnlyState();
            } else {
                // Clear the form when no post is selected
                currentPost = null;
                clearFormData();
            }
        });

        // Start new post button
        startNewPostButton = new Button("Start New Post");
        startNewPostButton.addThemeName("primary");
        startNewPostButton.addClickListener(e -> startNewPost());
        
        // Check if there's already an in-progress post
        boolean hasInProgressPost = userPosts.stream()
                .anyMatch(post -> post.getState() == PostState.IN_PROGRESS);
        startNewPostButton.setEnabled(!hasInProgressPost);

        controlsRow.add(postSelector, startNewPostButton);
        header.add(title, controlsRow);
        return header;
    }

    private String generatePostLabel(Post post) {
        if (post == null) {
            return "Select a post...";
        } else if (post.getState() == PostState.IN_PROGRESS) {
            return "In Progress - Started " + formatDateTime(post.getStart());
        } else if (post.getFinish() != null) {
            return "Completed " + formatDateTime(post.getFinish());
        } else {
            return "Post " + post.getId() + " - Started " + formatDateTime(post.getStart());
        }
    }

    private void startNewPost() {
        try {
            Post newPost = postService.startPost(currentUser);
            userPosts.add(newPost);
            postSelector.setItems(userPosts);
            postSelector.setValue(newPost);
            currentPost = newPost;
            loadPostData();
            updateReadOnlyState();
            startNewPostButton.setEnabled(false);
            Notification.show("New post started!", 3000, Notification.Position.TOP_CENTER);
        } catch (Exception e) {
            Notification.show("Error starting new post: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private VerticalLayout createContentSection() {
        VerticalLayout content = new VerticalLayout();
        content.setWidth("100%");
        content.setMaxWidth("800px");
        content.setSpacing(true);
        content.setPadding(false);
        content.addClassName("post-content");

        // Intro section
        VerticalLayout introSection = createIntroSection();
        
        // Personal section
        VerticalLayout personalSection = createCategorySection("Personal", "personal");
        
        // Family section
        VerticalLayout familySection = createCategorySection("Family", "family");
        
        // Work section
        VerticalLayout workSection = createCategorySection("Work", "work");
        
        // Stats section
        VerticalLayout statsSection = createStatsSection();

        content.add(introSection, personalSection, familySection, workSection, statsSection);
        return content;
    }

    private VerticalLayout createIntroSection() {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(false);
        section.setPadding(false);
        section.addClassName("post-section");

        H3 sectionTitle = new H3("Intro");
        sectionTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD, "section-title");

        // WIDWYTK field
        widwytkField = new TextArea("What I Don't Want You To Know");
        widwytkField.setWidth("100%");
        widwytkField.setHeight("100px");
        widwytkField.addClassName("post-textarea");

        // Kryptonite field
        kryptoniteField = new TextField("Kryptonite");
        kryptoniteField.setWidth("100%");
        kryptoniteField.addClassName("post-textfield");

        // What and When field
        whatAndWhenField = new TextArea("What and When");
        whatAndWhenField.setWidth("100%");
        whatAndWhenField.setHeight("100px");
        whatAndWhenField.addClassName("post-textarea");

        section.add(sectionTitle, widwytkField, kryptoniteField, whatAndWhenField);
        return section;
    }

    private VerticalLayout createCategorySection(String title, String categoryType) {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(false);
        section.setPadding(false);
        section.addClassName("post-section");

        H3 sectionTitle = new H3(title);
        sectionTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD, "section-title");

        TextArea bestField = new TextArea("Best");
        bestField.setWidth("100%");
        bestField.setHeight("100px");
        bestField.addClassName("post-textarea");

        TextArea worstField = new TextArea("Worst");
        worstField.setWidth("100%");
        worstField.setHeight("100px");
        worstField.addClassName("post-textarea");

        // Store references based on category type
        switch (categoryType) {
            case "personal":
                personalBestField = bestField;
                personalWorstField = worstField;
                break;
            case "family":
                familyBestField = bestField;
                familyWorstField = worstField;
                break;
            case "work":
                workBestField = bestField;
                workWorstField = worstField;
                break;
        }

        section.add(sectionTitle, bestField, worstField);
        return section;
    }

    private VerticalLayout createStatsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(false);
        section.setPadding(false);
        section.addClassName("post-section");

        H3 sectionTitle = new H3("Stats");
        sectionTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD, "section-title");

        // Create embedded StatsView
        statsView = StatsView.createEmbedded(oidcUserService, postService, userService);
        statsView.setWidth("100%");

        section.add(sectionTitle, statsView);
        return section;
    }

    private void loadPostData() {
        if (currentPost == null) return;

        // Load intro data
        widwytkField.setValue(currentPost.getIntro().getWidwytk() != null ? currentPost.getIntro().getWidwytk() : "");
        kryptoniteField.setValue(currentPost.getIntro().getKryptonite() != null ? currentPost.getIntro().getKryptonite() : "");
        whatAndWhenField.setValue(currentPost.getIntro().getWhatAndWhen() != null ? currentPost.getIntro().getWhatAndWhen() : "");

        // Load personal data
        personalBestField.setValue(currentPost.getPersonal().getBest() != null ? currentPost.getPersonal().getBest() : "");
        personalWorstField.setValue(currentPost.getPersonal().getWorst() != null ? currentPost.getPersonal().getWorst() : "");

        // Load family data
        familyBestField.setValue(currentPost.getFamily().getBest() != null ? currentPost.getFamily().getBest() : "");
        familyWorstField.setValue(currentPost.getFamily().getWorst() != null ? currentPost.getFamily().getWorst() : "");

        // Load work data
        workBestField.setValue(currentPost.getWork().getBest() != null ? currentPost.getWork().getBest() : "");
        workWorstField.setValue(currentPost.getWork().getWorst() != null ? currentPost.getWork().getWorst() : "");

        // Update stats view with current post data
        if (statsView != null) {
            statsView.setPost(currentPost);
        }
    }
    
    private void clearFormData() {
        // Clear intro data
        widwytkField.setValue("");
        kryptoniteField.setValue("");
        whatAndWhenField.setValue("");

        // Clear personal data
        personalBestField.setValue("");
        personalWorstField.setValue("");

        // Clear family data
        familyBestField.setValue("");
        familyWorstField.setValue("");

        // Clear work data
        workBestField.setValue("");
        workWorstField.setValue("");

        // Set stats view to read-only when no post is selected
        if (statsView != null) {
            statsView.setReadOnly(true);
            statsView.setPost(null);
        }

        // Set all fields to read-only when no post is selected
        setAllFieldsReadOnly(true);
    }
    
    private void setAllFieldsReadOnly(boolean readOnly) {
        widwytkField.setReadOnly(readOnly);
        kryptoniteField.setReadOnly(readOnly);
        whatAndWhenField.setReadOnly(readOnly);
        
        personalBestField.setReadOnly(readOnly);
        personalWorstField.setReadOnly(readOnly);
        
        familyBestField.setReadOnly(readOnly);
        familyWorstField.setReadOnly(readOnly);
        
        workBestField.setReadOnly(readOnly);
        workWorstField.setReadOnly(readOnly);
        
        if (statsView != null) {
            statsView.setReadOnly(readOnly);
        }
    }

    private void updateReadOnlyState() {
        if (currentPost == null) return;

        boolean isReadOnly = currentPost.getState() == PostState.COMPLETE;
        
        // Set read-only state for all fields
        widwytkField.setReadOnly(isReadOnly);
        kryptoniteField.setReadOnly(isReadOnly);
        whatAndWhenField.setReadOnly(isReadOnly);
        
        personalBestField.setReadOnly(isReadOnly);
        personalWorstField.setReadOnly(isReadOnly);
        
        familyBestField.setReadOnly(isReadOnly);
        familyWorstField.setReadOnly(isReadOnly);
        
        workBestField.setReadOnly(isReadOnly);
        workWorstField.setReadOnly(isReadOnly);
        
        // Set read-only state for stats view
        if (statsView != null) {
            statsView.setReadOnly(isReadOnly);
        }
    }

    private String formatDateTime(Date date) {
        if (date == null) {
            return "Unknown";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a");
        return formatter.format(date);
    }
} 