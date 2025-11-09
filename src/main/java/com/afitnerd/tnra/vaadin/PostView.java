package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.vaadin.presenter.VaadinPostPresenter;
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
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Optional;

@PageTitle("Posts - TNRA")
@Route(value = "posts", layout = MainLayout.class)
@CssImport("./styles/post-view.css")
public class PostView extends VerticalLayout implements AfterNavigationObserver {

    private final VaadinPostPresenter vaadinPostPresenter;
    private User currentUser;
    private User selectedUser;
    private Post currentPost;
    
    // Server-side pagination fields
    private int currentPage = 0;
    private int pageSize = 10;
    private Page<Post> currentPageData;
    
    // UI Components
    private ComboBox<User> userSelector;
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
    
    private boolean isUpdating = false;
    
    // Vaadin Binder for data binding (replaces manual field syncing)
    // is the true param for nested properties needed?
    private Binder<Post> postBinder = new Binder<>(Post.class, true);

    public PostView(VaadinPostPresenter vaadinPostPresenter) {
        this.vaadinPostPresenter = vaadinPostPresenter;

        setSizeFull();
        addClassName("post-view");
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        initializeUser();
        createPostView();

        // Initialize data binding after all form fields are created
        setupDataBinding();

        // Only load post data if we have a current post and we're not showing completed posts
        // (in which case the user should select from dropdown)
        if (currentPost != null && !showingCompletedPosts) {
            loadPostData();
            updateReadOnlyState();
        }
    }

    private void initializeUser() {
        // TODO implement and handle exceptions
        currentUser = vaadinPostPresenter.initializeUser();
        selectedUser = currentUser; // Default to current authenticated user
        Optional<Post> inProgressPost = vaadinPostPresenter.getOptionalInProgressPost(currentUser);
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
    }

    private void loadCurrentPage() {
        // Only load completed posts (not in-progress posts)
        Pageable pageable = PageRequest.of(currentPage, pageSize, Sort.by(Sort.Direction.DESC, "finish"));
        currentPageData = vaadinPostPresenter.getCompletedPostsPage(selectedUser, pageable);
    }

    private void createPostView() {
        // Clear existing content
        removeAll();
        
        // Header section with post selector and start new post button
        VerticalLayout headerSection = createHeaderSection();
        
        // Main content sections
        VerticalLayout contentSection = createContentSection();
        
        // Footer section for in-progress posts
        VerticalLayout footerSection = createFooterSection();
        
        add(headerSection, contentSection, footerSection);
        
        // If no current post is selected, clear form data and set fields to read-only
        if (currentPost == null) {
            clearFormData();
        }
    }

    private VerticalLayout createHeaderSection() {
        VerticalLayout header = new VerticalLayout();
        header.addClassName("post-header");
        header.addClassName("sticky-header");

        // Create appropriate header based on current view mode
        if (showingCompletedPosts) {
            header.add(createNoInProgressPostHeader());
        } else {
            // Check if there's an in-progress post when not explicitly showing completed posts
            boolean hasInProgressPost = vaadinPostPresenter.getOptionalInProgressPost(currentUser).isPresent();
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
        
        controlsLayout.add(showCompletedPostsButton, dateSpan);
        return controlsLayout;
    }

    private VerticalLayout createNoInProgressPostHeader() {
        VerticalLayout controlsLayout = new VerticalLayout();
        controlsLayout.addClassName("post-controls");

        // Check if there's an in-progress post
        Optional<Post> inProgressPost = vaadinPostPresenter.getOptionalInProgressPost(currentUser);
        
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

        // Create horizontal layout for user and post selectors
        HorizontalLayout selectorsLayout = new HorizontalLayout();
        selectorsLayout.addClassName("selectors-layout");
        selectorsLayout.setSpacing(true);
        selectorsLayout.setAlignItems(Alignment.BASELINE);

        // User selector
        userSelector = new ComboBox<>("User");
        userSelector.addClassName("user-selector");
        userSelector.setItemLabelGenerator(user -> {
            if (user == null) {
                return "Select a user...";
            }
            return user.getFirstName() + " " + user.getLastName();
        });

        // Get all active users and set them (already sorted by first name)
        userSelector.setItems(vaadinPostPresenter.getAllActiveUsers());

        // Set default to current authenticated user
        userSelector.setValue(selectedUser);

        userSelector.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                selectedUser = e.getValue();
                // Reset to first page when user changes
                currentPage = 0;
                // Reload completed posts for the selected user
                loadCurrentPage();
                updatePaginationControls();
                updatePostSelector();
            }
        });

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
                clearFormData();
            }
        });

        // Add selectors to horizontal layout
        selectorsLayout.add(userSelector, postSelector);

        // Create pagination controls
        VerticalLayout paginationLayout = createPaginationControls();

        completedLayout.add(selectorsLayout, paginationLayout);
        return completedLayout;
    }

    private void showCompletedPosts() {
        showingCompletedPosts = true;
        
        // Load completed posts if not already loaded
        if (currentPageData == null) {
            loadCurrentPage();
        }
        
        // Clear current post and form data
        clearFormData();
        
        // Recreate header with completed posts view
        recreateHeader();
    }

    private void switchToInProgressPost() {
        showingCompletedPosts = false;
        
        // Set current post to in-progress post BEFORE recreating header
        Optional<Post> inProgressPost = vaadinPostPresenter.getOptionalInProgressPost(currentUser);
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
        
        // Find and remove the existing footer
        getChildren().filter(component -> component.getElement().hasAttribute("class") && 
                            component.getElement().getAttribute("class").contains("post-footer"))
                    .findFirst()
                    .ifPresent(this::remove);
        
        // Create and add new header
        VerticalLayout newHeader = createHeaderSection();
        addComponentAsFirst(newHeader);
        
        // Create and add new footer (will be hidden if not needed)
        VerticalLayout newFooter = createFooterSection();
        add(newFooter);
        
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
            Post newPost = vaadinPostPresenter.startPost(currentUser);
            
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
            vaadinPostPresenter.finishPost(currentUser);
            
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

    private VerticalLayout createFooterSection() {
        VerticalLayout footer = new VerticalLayout();
        footer.addClassName("post-footer");
        
        // Only create and show footer for in-progress posts
        if (!showingCompletedPosts && currentPost != null && currentPost.getState() == PostState.IN_PROGRESS) {
            // Create finish post button
            finishPostButton = new Button("Finish Post");
            finishPostButton.addThemeName("primary");
            finishPostButton.addClassName("finish-post-button");
            finishPostButton.setEnabled(false); // Initially disabled
            finishPostButton.addClickListener(e -> finishPost());
            
            footer.add(finishPostButton);
            footer.setAlignItems(Alignment.CENTER);
            footer.setVisible(true);
        } else {
            // Hide footer completely when not needed
            footer.setVisible(false);
        }
        
        return footer;
    }

    private VerticalLayout createIntroSection() {
        VerticalLayout section = new VerticalLayout();
        section.addClassName("post-section");

        H3 sectionTitle = new H3("Intro");
        sectionTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD, "section-title");

        // WIDWYTK field
        widwytkField = new TextArea("What I Don't Want You To Know");
        widwytkField.addClassName("post-textarea");

        // Kryptonite field
        kryptoniteField = new TextField("Kryptonite");
        kryptoniteField.addClassName("post-textfield");

        // What and When field
        whatAndWhenField = new TextArea("What and When");
        whatAndWhenField.addClassName("post-textarea");

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

        // Store references based on category type (Binder handles value change listeners)
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
        section.addClassName("post-section");

        H3 sectionTitle = new H3("Stats");
        sectionTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD, "section-title");

        // Create embedded StatsView
        statsView = StatsView.createEmbedded(vaadinPostPresenter);
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
            // Load all form data using Binder (replaces all manual field setting)
            postBinder.setBean(currentPost);

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
        // TODO - is this fragile? Without this, the value change listener writes empty values to the database
        // because of the next line when we setBean to null
        // clear current post
        currentPost = null;

        // Clear all form data using Binder (replaces all manual field clearing)
        postBinder.setBean(null);

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

    /**
     * Sets up data binding for all form sections using Vaadin Binder.
     * This demonstrates the recommended approach vs manual field syncing.
     */
    private void setupDataBinding() {
        // Set up validation and save-on-change for intro fields
        postBinder.forField(widwytkField).bind("intro.widwytk");
            
        postBinder.forField(kryptoniteField).bind("intro.kryptonite");
            
        postBinder.forField(whatAndWhenField).bind("intro.whatAndWhen");
        
        // Set up validation and save-on-change for personal fields
        postBinder.forField(personalBestField).bind("personal.best");
            
        postBinder.forField(personalWorstField).bind("personal.worst");
        
        // Set up validation and save-on-change for family fields
        postBinder.forField(familyBestField).bind("family.best");
            
        postBinder.forField(familyWorstField).bind("family.worst");
        
        // Set up validation and save-on-change for work fields
        postBinder.forField(workBestField).bind("work.best");
            
        postBinder.forField(workWorstField).bind("work.worst");
        
        // Add value change listener for automatic saving
        postBinder.addValueChangeListener(event -> {
            if (currentPost != null && !isUpdating) {
                savePostChanges();
            }
        });
    }
    
    /**
     * Saves all form changes using Binder approach.
     */
    private void savePostChanges() {
        try {
            // Write current field values to the bean (even if validation fails)
            postBinder.writeBean(currentPost);
            
            // Save entire post in single transaction for immediate persistence
            currentPost = vaadinPostPresenter.savePost(currentPost);

            // Update finish button state
            updateFinishButtonState();
        } catch (Exception e) {
            Notification.show("Error saving post: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
        }
    }
    
    // NOTE: Manual debouncing methods removed - now using Vaadin Binder approach
    // See setupDataBinding() and savePostChanges() methods for the modern implementation
} 