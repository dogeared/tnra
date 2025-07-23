package com.afitnerd.tnra.vaadin;

import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

import com.afitnerd.tnra.service.VaadinPostService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.PostService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.ZoneId;

@PageTitle("Posts - TNRA")
@Route(value = "posts", layout = MainLayout.class)
@CssImport("./styles/post-view.css")
public class PostView extends VerticalLayout implements AfterNavigationObserver {

    private final VaadinPostService vaadinPostService;
    private final PostService postService;
    private final UserService userService;
    private final OidcUserService oidcUserService;
    private User currentUser;
    private Post currentPost;
    
    // Server-side pagination fields
    private int currentPage = 0;
    private int pageSize = 10;
    private Page<Post> currentPageData;
    
    // UI Components
    private ComboBox<Post> postSelector;
    private Button startNewPostButton;
    private StatsView statsView;
    
    // Pagination UI Components
    private Button firstPageButton;
    private Button previousPageButton;
    private Button nextPageButton;
    private Button lastPageButton;
    private ComboBox<Integer> pageSizeSelector;
    private IntegerField pageNumberField;
    private Span pageInfoLabel;
    
    // View mode controls
    private Button showCompletedPostsButton;
    private Button switchToInProgressButton;
    private Button finishPostButton;
    private VerticalLayout completedPostsLayout;
    boolean showingCompletedPosts = false; // package-private for testing
    
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
    
    // Debounced update system
    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor();
    private boolean isUpdating = false;

    public PostView(
        VaadinPostService vaadinPostService,
        OidcUserService oidcUserService, PostService postService, UserService userService
    ) {
        this.vaadinPostService = vaadinPostService;
        this.oidcUserService = oidcUserService;
        this.postService = postService;
        this.userService = userService;

        setSizeFull();
        addClassName("post-view");
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (event.getLocation().getFirstSegment().equals("posts")) {
            initializeUser();
            createPostView();
            // Only load post data if we have a current post and we're not showing completed posts
            // (in which case the user should select from dropdown)
            if (currentPost != null && !showingCompletedPosts) {
                loadPostData();
                updateReadOnlyState();
            }
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
            
            // Check if there's an in-progress post
            Optional<Post> inProgressPost = postService.getOptionalInProgressPost(currentUser);
            
            if (inProgressPost.isPresent()) {
                // Show in-progress post by default
                currentPost = inProgressPost.get();
                showingCompletedPosts = false;
            } else {
                // No in-progress post, show completed posts
                loadCurrentPage();
                showingCompletedPosts = true;
                // Don't automatically select a post - let user choose from dropdown
                currentPost = null;
            }
        } else {
            Notification.show("Authentication required.", 5000, Notification.Position.MIDDLE);
        }
    }

    private void loadCurrentPage() {
        // Only load completed posts (not in-progress posts)
        Pageable pageable = PageRequest.of(currentPage, pageSize, Sort.by(Sort.Direction.DESC, "finish"));
        currentPageData = postService.getCompletedPostsPage(currentUser, pageable);
    }

    private void createPostView() {
        // Clear existing content
        removeAll();
        
        // Header section with post selector and start new post button
        VerticalLayout headerSection = createHeaderSection();
        
        // Main content sections
        VerticalLayout contentSection = createContentSection();
        
        add(headerSection, contentSection);
        
        // If no current post is selected, clear form data and set fields to read-only
        if (currentPost == null) {
            clearFormData();
        }
    }

    private VerticalLayout createHeaderSection() {
        VerticalLayout header = new VerticalLayout();
        header.addClassName("post-header");

        // Create appropriate header based on current view mode
        if (showingCompletedPosts) {
            header.add(createNoInProgressPostHeader());
        } else {
            // Check if there's an in-progress post when not explicitly showing completed posts
            boolean hasInProgressPost = postService.getOptionalInProgressPost(currentUser).isPresent();
            if (hasInProgressPost) {
                header.add(createInProgressPostHeader());
            } else {
                header.add(createNoInProgressPostHeader());
            }
        }

        return header;
    }

    private VerticalLayout createInProgressPostHeader() {
        VerticalLayout controlsLayout = new VerticalLayout();
        controlsLayout.addClassName("post-controls");

        // Create switch to completed posts button
        showCompletedPostsButton = new Button("Switch to completed posts");
        showCompletedPostsButton.addThemeName("secondary");
        showCompletedPostsButton.addClassName("switch-posts-button");
        showCompletedPostsButton.addClickListener(e -> showCompletedPosts());

        String startDate = "Post started " +
            DateTimeUtils.formatDateTime(currentPost.getStart());
        Span dateSpan = new Span(startDate);
        dateSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY, "stats-date");
        
        // Create finish post button
        finishPostButton = new Button("Finish Post");
        finishPostButton.addThemeName("primary");
        finishPostButton.addClassName("finish-post-button");
        finishPostButton.setEnabled(false); // Initially disabled
        finishPostButton.addClickListener(e -> finishPost());
        
        controlsLayout.add(showCompletedPostsButton, dateSpan, finishPostButton);
        return controlsLayout;
    }

    private VerticalLayout createNoInProgressPostHeader() {
        VerticalLayout controlsLayout = new VerticalLayout();
        controlsLayout.addClassName("post-controls");

        // Check if there's an in-progress post
        Optional<Post> inProgressPost = postService.getOptionalInProgressPost(currentUser);
        
        // Only create start new post button if there's no in-progress post
        if (!inProgressPost.isPresent()) {
            startNewPostButton = new Button("Start New Post");
            startNewPostButton.addThemeName("primary");
            startNewPostButton.addClassName("start-new-post-button");
            startNewPostButton.addClickListener(e -> startNewPost());
            controlsLayout.add(startNewPostButton);
        }

        // Create completed posts layout
        completedPostsLayout = createCompletedPostsLayout();
        
        // Add switch to in-progress button if there is an in-progress post
        if (inProgressPost.isPresent()) {
            switchToInProgressButton = new Button("Switch to in-progress post");
            switchToInProgressButton.addThemeName("secondary");
            switchToInProgressButton.addClassName("switch-posts-button");
            switchToInProgressButton.addClickListener(e -> switchToInProgressPost());
            completedPostsLayout.addComponentAsFirst(switchToInProgressButton);
        }
        
        controlsLayout.add(completedPostsLayout);
        
        return controlsLayout;
    }

    private VerticalLayout createCompletedPostsLayout() {
        VerticalLayout completedLayout = new VerticalLayout();
        completedLayout.addClassName("completed-posts-layout");

        // Post selector for completed posts
        postSelector = new ComboBox<>("Posts by Finished Date");
        postSelector.addClassName("post-selector");
        postSelector.setItemLabelGenerator(this::generatePostLabel);
        
        // Set items to current page posts (only completed posts)
        if (currentPageData != null) {
            postSelector.setItems(currentPageData.getContent());
        } else {
            postSelector.setItems(new ArrayList<>());
        }
        
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

        // Create pagination controls
        VerticalLayout paginationLayout = createPaginationControls();

        completedLayout.add(postSelector, paginationLayout);
        return completedLayout;
    }

    private void showCompletedPosts() {
        showingCompletedPosts = true;
        
        // Load completed posts if not already loaded
        if (currentPageData == null) {
            loadCurrentPage();
        }
        
        // Clear current post and form data
        currentPost = null;
        clearFormData();
        
        // Recreate header with completed posts view
        recreateHeader();
    }

    private void switchToInProgressPost() {
        showingCompletedPosts = false;
        
        // Set current post to in-progress post BEFORE recreating header
        Optional<Post> inProgressPost = postService.getOptionalInProgressPost(currentUser);
        if (inProgressPost.isPresent()) {
            currentPost = inProgressPost.get();
        }
        
        // Recreate header with in-progress post view
        recreateHeader();
        
        // Load post data if we have a current post
        if (currentPost != null) {
            loadPostData();
            updateReadOnlyState();
        }
        
        // Remove switch button
        if (switchToInProgressButton != null) {
            completedPostsLayout.remove(switchToInProgressButton);
        }
    }
    
    private void recreateHeader() {
        // Find and remove the existing header
        getChildren().filter(component -> component.getElement().hasAttribute("class") && 
                            component.getElement().getAttribute("class").contains("post-header"))
                    .findFirst()
                    .ifPresent(this::remove);
        
        // Create and add new header
        VerticalLayout newHeader = createHeaderSection();
        addComponentAsFirst(newHeader);
        
        // Update pagination and post selector if showing completed posts
        if (showingCompletedPosts) {
            updatePaginationControls();
            updatePostSelector();
            if (postSelector != null) {
                postSelector.setValue(null);
            }
        }
    }

    private VerticalLayout createPaginationControls() {
        VerticalLayout paginationLayout = new VerticalLayout();
        paginationLayout.setAlignItems(Alignment.CENTER);
        paginationLayout.setSpacing(false);
        paginationLayout.setPadding(false);
        paginationLayout.addClassName("pagination-controls");

        // Single row with all pagination controls
        HorizontalLayout paginationRow = new HorizontalLayout();
        paginationRow.setAlignItems(Alignment.CENTER);
        paginationRow.setSpacing(true);
        paginationRow.setPadding(false);
        paginationRow.addClassName("pagination-row");

        // First page button
        firstPageButton = new Button(VaadinIcon.FAST_BACKWARD.create());
        firstPageButton.setTooltipText("First page");
        firstPageButton.addClickListener(e -> goToFirstPage());
        firstPageButton.addClassName("pagination-button");

        // Previous page button
        previousPageButton = new Button(VaadinIcon.STEP_BACKWARD.create());
        previousPageButton.setTooltipText("Previous page");
        previousPageButton.addClickListener(e -> goToPreviousPage());
        previousPageButton.addClassName("pagination-button");

        // Page navigation with label and input field
        HorizontalLayout pageNavigationLayout = new HorizontalLayout();
        pageNavigationLayout.addClassName("page-navigation-layout");

        // Page label
        Span pageLabel = new Span("Page: ");
        pageLabel.addClassName("page-label");

        // Page number input field
        pageNumberField = new IntegerField();
        pageNumberField.setMin(1);
        pageNumberField.setValue(currentPage + 1);
        pageNumberField.addClassName("pagination-input");
        pageNumberField.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                int pageNumber = e.getValue();
                if (pageNumber >= 1) {
                    goToPage(pageNumber - 1);
                }
            }
        });

        // Page info label
        pageInfoLabel = new Span(" of " + (currentPageData != null ? currentPageData.getTotalPages() : 1));
        pageInfoLabel.addClassName("page-info");

        pageNavigationLayout.add(pageLabel, pageNumberField, pageInfoLabel);

        // Next page button
        nextPageButton = new Button(VaadinIcon.STEP_FORWARD.create());
        nextPageButton.setTooltipText("Next page");
        nextPageButton.addClickListener(e -> goToNextPage());
        nextPageButton.addClassName("pagination-button");

        // Last page button
        lastPageButton = new Button(VaadinIcon.FAST_FORWARD.create());
        lastPageButton.setTooltipText("Last page");
        lastPageButton.addClickListener(e -> goToLastPage());
        lastPageButton.addClassName("pagination-button");

        // Posts per page selector
        Span pageSizeLabel = new Span("per page:");
        pageSizeSelector = new ComboBox<>();
        pageSizeSelector.setItems(5, 10, 25);
        pageSizeSelector.setValue(10);
        pageSizeSelector.addClassName("per-page-selector");
        pageSizeSelector.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                currentPage = 0; // Reset to first page
                loadCurrentPage();
                updatePaginationControls();
                updatePostSelector();
            }
        });

        // Add all controls to the single row
        paginationRow.add(firstPageButton, previousPageButton, pageNavigationLayout, nextPageButton, lastPageButton, pageSizeLabel, pageSizeSelector);

        paginationLayout.add(paginationRow);
        updatePaginationControls();
        return paginationLayout;
    }

    private void updatePaginationControls() {
        if (currentPageData == null) return;
        
        int totalPages = currentPageData.getTotalPages();
        
        // Update button states
        firstPageButton.setEnabled(currentPage > 0);
        previousPageButton.setEnabled(currentPage > 0);
        nextPageButton.setEnabled(currentPage < totalPages - 1);
        lastPageButton.setEnabled(currentPage < totalPages - 1);

        // Update page number field
        pageNumberField.setValue(currentPage + 1);
        pageNumberField.setMax(totalPages);

        // Update page info label
        pageInfoLabel.setText("of " + totalPages);
    }

    private void updatePostSelector() {
        if (currentPageData != null) {
            postSelector.setItems(currentPageData.getContent());
        } else {
            postSelector.setItems(new ArrayList<>());
        }
        postSelector.setValue(null);
    }

    private void goToFirstPage() {
        if (currentPage > 0) {
            currentPage = 0;
            loadCurrentPage();
            updatePaginationControls();
            updatePostSelector();
        }
    }

    private void goToPreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            loadCurrentPage();
            updatePaginationControls();
            updatePostSelector();
        }
    }

    private void goToNextPage() {
        if (currentPageData != null && currentPage < currentPageData.getTotalPages() - 1) {
            currentPage++;
            loadCurrentPage();
            updatePaginationControls();
            updatePostSelector();
        }
    }

    private void goToLastPage() {
        if (currentPageData != null && currentPage < currentPageData.getTotalPages() - 1) {
            currentPage = currentPageData.getTotalPages() - 1;
            loadCurrentPage();
            updatePaginationControls();
            updatePostSelector();
        }
    }

    private void goToPage(int page) {
        if (currentPageData != null && page >= 0 && page < currentPageData.getTotalPages() && page != currentPage) {
            currentPage = page;
            loadCurrentPage();
            updatePaginationControls();
            updatePostSelector();
        }
    }

    private String generatePostLabel(Post post) {
        if (post == null) {
            return "Select a post...";
        } else if (post.getState() == PostState.IN_PROGRESS) {
            return "In Progress - Started " + DateTimeUtils.formatDateTime(post.getStart());
        } else if (post.getFinish() != null) {
            return DateTimeUtils.formatDateTime(post.getFinish());
        } else {
            return "Post " + post.getId() + " - Started " + DateTimeUtils.formatDateTime(post.getStart());
        }
    }

    private void startNewPost() {
        try {
            Post newPost = postService.startPost(currentUser);
            
            // Switch to in-progress post view
            currentPost = newPost;
            showingCompletedPosts = false;
            
            loadPostData();
            updateReadOnlyState();
            updateFinishButtonState();
            
            // Recreate header with in-progress post view
            recreateHeader();

            Notification.show("New post started!", 3000, Notification.Position.TOP_CENTER);
        } catch (Exception e) {
            Notification.show("Error starting new post: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }
    
    private void finishPost() {
        try {
            vaadinPostService.finishPost(currentUser);
            
            // Switch to completed posts view
            showingCompletedPosts = true;
            
            // Reload completed posts
            loadCurrentPage();
            
            // Clear form data
            clearFormData();
            
            // Recreate header with completed posts view
            recreateHeader();
            
            Notification.show("Post completed successfully!", 3000, Notification.Position.TOP_CENTER);
        } catch (Exception e) {
            Notification.show("Error finishing post: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private VerticalLayout createContentSection() {
        VerticalLayout content = new VerticalLayout();
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
        section.addClassName("post-section");

        H3 sectionTitle = new H3("Intro");
        sectionTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD, "section-title");

        // WIDWYTK field
        widwytkField = new TextArea("What I Don't Want You To Know");
        widwytkField.addClassName("post-textarea");
        widwytkField.addValueChangeListener(e -> debouncedUpdateIntro());

        // Kryptonite field
        kryptoniteField = new TextField("Kryptonite");
        kryptoniteField.addClassName("post-textfield");
        kryptoniteField.addValueChangeListener(e -> debouncedUpdateIntro());

        // What and When field
        whatAndWhenField = new TextArea("What and When");
        whatAndWhenField.addClassName("post-textarea");
        whatAndWhenField.addValueChangeListener(e -> debouncedUpdateIntro());

        section.add(sectionTitle, widwytkField, kryptoniteField, whatAndWhenField);
        return section;
    }

    private VerticalLayout createCategorySection(String title, String categoryType) {
        VerticalLayout section = new VerticalLayout();
        section.addClassName("post-section");

        H3 sectionTitle = new H3(title);
        sectionTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD, "section-title");

        TextArea bestField = new TextArea("Best");
        bestField.addClassName("post-textarea");

        TextArea worstField = new TextArea("Worst");
        worstField.addClassName("post-textarea");

        // Store references based on category type and add listeners
        switch (categoryType) {
            case "personal":
                personalBestField = bestField;
                personalWorstField = worstField;
                bestField.addValueChangeListener(e -> debouncedUpdatePersonal());
                worstField.addValueChangeListener(e -> debouncedUpdatePersonal());
                break;
            case "family":
                familyBestField = bestField;
                familyWorstField = worstField;
                bestField.addValueChangeListener(e -> debouncedUpdateFamily());
                worstField.addValueChangeListener(e -> debouncedUpdateFamily());
                break;
            case "work":
                workBestField = bestField;
                workWorstField = worstField;
                bestField.addValueChangeListener(e -> debouncedUpdateWork());
                worstField.addValueChangeListener(e -> debouncedUpdateWork());
                break;
        }

        section.add(sectionTitle, bestField, worstField);
        return section;
    }

    private VerticalLayout createStatsSection() {
        VerticalLayout section = new VerticalLayout();
        section.addClassName("post-section");

        H3 sectionTitle = new H3("Stats");
        sectionTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD, "section-title");

        // Create embedded StatsView
        statsView = StatsView.createEmbedded(oidcUserService, postService, userService);
        statsView.addClassName("stats-view");
        statsView.setOnStatsChanged(this::updateFinishButtonState);

        section.add(sectionTitle, statsView);
        return section;
    }

    private void loadPostData() {
        if (currentPost == null) return;

        // Temporarily disable updates while loading
        isUpdating = true;

        try {
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
        } finally {
            // Re-enable updates
            isUpdating = false;
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
        
        // Update finish button state
        updateFinishButtonState();
    }
    
    private void updateFinishButtonState() {
        if (currentPost == null || currentPost.getState() == PostState.COMPLETE || showingCompletedPosts) {
            if (finishPostButton != null) {
                finishPostButton.setEnabled(false);
            }
            return;
        }
        
        // Button should exist when editing in-progress post
        if (finishPostButton == null) {
            return;
        }
        
        // Check if all intro fields have values
        boolean introComplete = !isEmpty(widwytkField.getValue()) && 
                               !isEmpty(kryptoniteField.getValue()) && 
                               !isEmpty(whatAndWhenField.getValue());
        
        // Check if all personal fields have values
        boolean personalComplete = !isEmpty(personalBestField.getValue()) && 
                                  !isEmpty(personalWorstField.getValue());
        
        // Check if all family fields have values
        boolean familyComplete = !isEmpty(familyBestField.getValue()) && 
                                !isEmpty(familyWorstField.getValue());
        
        // Check if all work fields have values
        boolean workComplete = !isEmpty(workBestField.getValue()) && 
                              !isEmpty(workWorstField.getValue());
        
        // Check if all stats have values (0 or greater)
        boolean statsComplete = statsView != null && statsView.areAllStatsSet();
        
        // Enable finish button only if all sections are complete
        boolean allComplete = introComplete && personalComplete && familyComplete && workComplete && statsComplete;
        
        if (finishPostButton != null) {
            finishPostButton.setEnabled(allComplete);
        }
    }
    
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

//    private String formatDateTime(Date date) {
//        ZoneId displayZone = ZoneId.systemDefault();
//        if (
//            UI.getCurrent() != null &&
//            UI.getCurrent().getSession() != null &&
//            UI.getCurrent().getSession().getAttribute(ExtendedClientDetails.class) != null
//        ) {
//            displayZone =
//                ZoneId.of(UI.getCurrent().getSession().getAttribute(ExtendedClientDetails.class).getTimeZoneId());
//        }
//        return DateTimeUtils.formatDateTime(date, displayZone);
//    }
    
    // Debounced update methods
    private void debouncedUpdateIntro() {
        if (currentPost == null || isUpdating) return;
        
        debounceExecutor.schedule(() -> {
            try {
                isUpdating = true;
                
                // Create intro object with current values
                com.afitnerd.tnra.model.Intro intro = new com.afitnerd.tnra.model.Intro();
                intro.setWidwytk(widwytkField.getValue());
                intro.setKryptonite(kryptoniteField.getValue());
                intro.setWhatAndWhen(whatAndWhenField.getValue());
                
                // Update the post
                postService.replaceIntro(currentUser, intro);
                
                // Refresh current post
                currentPost = postService.getInProgressPost(currentUser);
                
                // Update finish button state
                updateFinishButtonState();
                
            } catch (Exception e) {
                Notification.show("Error saving intro: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
            } finally {
                isUpdating = false;
            }
        }, 1000, TimeUnit.MILLISECONDS);
    }
    
    private void debouncedUpdatePersonal() {
        if (currentPost == null || isUpdating) return;
        
        debounceExecutor.schedule(() -> {
            try {
                isUpdating = true;
                
                // Create personal object with current values
                com.afitnerd.tnra.model.Category personal = new com.afitnerd.tnra.model.Category();
                personal.setBest(personalBestField.getValue());
                personal.setWorst(personalWorstField.getValue());
                
                // Update the post
                postService.replacePersonal(currentUser, personal);
                
                // Refresh current post
                currentPost = postService.getInProgressPost(currentUser);
                
                // Update finish button state
                updateFinishButtonState();
                
            } catch (Exception e) {
                Notification.show("Error saving personal: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
            } finally {
                isUpdating = false;
            }
        }, 1000, TimeUnit.MILLISECONDS);
    }
    
    private void debouncedUpdateFamily() {
        if (currentPost == null || isUpdating) return;
        
        debounceExecutor.schedule(() -> {
            try {
                isUpdating = true;
                
                // Create family object with current values
                com.afitnerd.tnra.model.Category family = new com.afitnerd.tnra.model.Category();
                family.setBest(familyBestField.getValue());
                family.setWorst(familyWorstField.getValue());
                
                // Update the post
                postService.replaceFamily(currentUser, family);
                
                // Refresh current post
                currentPost = postService.getInProgressPost(currentUser);
                
                // Update finish button state
                updateFinishButtonState();
                
            } catch (Exception e) {
                Notification.show("Error saving family: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
            } finally {
                isUpdating = false;
            }
        }, 1000, TimeUnit.MILLISECONDS);
    }
    
    private void debouncedUpdateWork() {
        if (currentPost == null || isUpdating) return;
        
        debounceExecutor.schedule(() -> {
            try {
                isUpdating = true;
                
                // Create work object with current values
                com.afitnerd.tnra.model.Category work = new com.afitnerd.tnra.model.Category();
                work.setBest(workBestField.getValue());
                work.setWorst(workWorstField.getValue());
                
                // Update the post
                postService.replaceWork(currentUser, work);
                
                // Refresh current post
                currentPost = postService.getInProgressPost(currentUser);
                
                // Update finish button state
                updateFinishButtonState();
                
            } catch (Exception e) {
                Notification.show("Error saving work: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
            } finally {
                isUpdating = false;
            }
        }, 1000, TimeUnit.MILLISECONDS);
    }
} 