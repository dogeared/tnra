package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.GoToGuyPair;
import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.vaadin.presenter.CallChainPresenter;
import com.afitnerd.tnra.vaadin.presenter.VaadinAdminPresenter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@PageTitle("Admin Dashboard - TNRA")
@Route(value = "admin", layout = MainLayout.class)
@RolesAllowed("ADMIN")
@CssImport("./styles/admin-view.css")
public class AdminView extends VerticalLayout {

    private final VaadinAdminPresenter vaadinAdminPresenter;
    private final CallChainPresenter callChainPresenter;
    private GoToGuySet workingSet;

    public AdminView(VaadinAdminPresenter vaadinAdminPresenter, CallChainPresenter callChainPresenter) {
        this.vaadinAdminPresenter = vaadinAdminPresenter;
        this.callChainPresenter = callChainPresenter;
        
        addClassName("admin-view");
        setSizeFull();
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        
        createHeader();
        createContent();
    }

    private void createHeader() {
        H2 title = new H2("Admin Dashboard");
        title.addClassName("admin-title");
        
        Paragraph subtitle = new Paragraph("Administrative tools and system management");
        subtitle.addClassName("admin-subtitle");
        
        add(title, subtitle);
    }

    private void createContent() {
        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();
        
        // Create GTG tab with existing admin content
        VerticalLayout gtgContent = createGtgTabContent();
        tabSheet.add("GTG", gtgContent);

        // Create Build Info tab
        VerticalLayout buildInfoContent = createBuildInfoTabContent();
        tabSheet.add("Build Info", buildInfoContent);

        add(tabSheet);
    }
    
    private VerticalLayout createBuildInfoTabContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setSpacing(true);
        content.setPadding(true);

        H3 header = new H3("Build Information");
        header.addClassName("section-header");

        Div infoCard = new Div();
        infoCard.addClassName("system-info");

        infoCard.add(createInfoRow("Git Tag", vaadinAdminPresenter.getGitTag()));
        infoCard.add(createInfoRow("Git Commit", vaadinAdminPresenter.getGitCommitId()));
        infoCard.add(createInfoRow("Git Branch", vaadinAdminPresenter.getGitBranch()));
        infoCard.add(createInfoRow("Spring Boot", vaadinAdminPresenter.getSpringBootVersion()));
        infoCard.add(createInfoRow("Vaadin", vaadinAdminPresenter.getVaadinVersion()));
        infoCard.add(createInfoRow("Java", vaadinAdminPresenter.getJavaVersion()));
        infoCard.add(createInfoRow("Build Time", vaadinAdminPresenter.getBuildTime()));

        content.add(header, infoCard);
        return content;
    }

    private Div createInfoRow(String label, String value) {
        Div row = new Div();
        row.getStyle().set("padding", "0.5rem 0");
        row.getStyle().set("display", "flex");
        row.getStyle().set("gap", "1rem");

        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle().set("font-weight", "600");
        labelSpan.getStyle().set("min-width", "120px");
        labelSpan.getStyle().set("color", "var(--tnra-text-secondary)");

        Span valueSpan = new Span(value != null ? value : "N/A");

        row.add(labelSpan, valueSpan);
        return row;
    }

    private VerticalLayout createGtgTabContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setSpacing(true);
        content.setPadding(true);
        
        // Header section
        H3 gtgHeader = new H3("Go To Guy Management");
        gtgHeader.addClassName("section-header");
        
        // Current GTG Set display
        VerticalLayout currentSetSection = createCurrentGTGSetSection();
        
        // Create new GTG Set section
        VerticalLayout newSetSection = createNewGTGSetSection();
        
        content.add(gtgHeader, currentSetSection, newSetSection);
        return content;
    }
    
    private VerticalLayout createCurrentGTGSetSection() {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(false);
        
        H3 sectionHeader = new H3("Current Go To Guy Set");
        sectionHeader.addClassName("subsection-header");
        
        Grid<GoToGuyPair> currentGrid = new Grid<>(GoToGuyPair.class, false);
        currentGrid.addColumn(new TextRenderer<>(pair -> 
            getUserDisplayName(pair.getCaller()))).setHeader("Caller");
        currentGrid.addColumn(new TextRenderer<>(pair -> 
            getUserDisplayName(pair.getCallee()))).setHeader("Calls");
        currentGrid.setHeight("200px");
        
        // Load current data
        GoToGuySet currentSet = callChainPresenter.getCurrentGoToGuySet();
        if (currentSet != null && currentSet.getGoToGuyPairs() != null) {
            currentGrid.setItems(currentSet.getGoToGuyPairs());
        }
        
        section.add(sectionHeader, currentGrid);
        return section;
    }
    
    private VerticalLayout createNewGTGSetSection() {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(false);

        // Button to initiate creating new set - initially visible
        Button createNewSetBtn = new Button("Create New Go To Guy Set", VaadinIcon.PLUS.create());
        createNewSetBtn.addClassName("admin-button");

        // Section content - initially hidden
        VerticalLayout contentSection = new VerticalLayout();
        contentSection.setSpacing(true);
        contentSection.setPadding(false);
        contentSection.setVisible(false);

        H3 sectionHeader = new H3("Create New Go To Guy Set");
        sectionHeader.addClassName("subsection-header");

        Grid<GoToGuyPair> pairsGrid = new Grid<>(GoToGuyPair.class, false);
        pairsGrid.addColumn(new TextRenderer<>(pair ->
            getUserDisplayName(pair.getCaller()))).setHeader("Caller");
        pairsGrid.addColumn(new TextRenderer<>(pair ->
            getUserDisplayName(pair.getCallee()))).setHeader("Calls");
        pairsGrid.addComponentColumn(pair -> {
            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteBtn.addClickListener(e -> {
                // Remove pair from the working set and database
                workingSet = callChainPresenter.removePairFromSet(workingSet, pair);

                // Refresh grid
                pairsGrid.setItems(workingSet.getGoToGuyPairs());

                Notification.show("Pair removed successfully");
            });
            return deleteBtn;
        }).setHeader("Actions").setWidth("120px").setFlexGrow(0);
        pairsGrid.setHeight("300px");

        Button addPairBtn = new Button("Add Pair", VaadinIcon.PLUS.create());
        addPairBtn.addClassName("admin-button");
        addPairBtn.setEnabled(false); // Initially disabled
        addPairBtn.addClickListener(e -> openAddPairDialog(pairsGrid));

        contentSection.add(sectionHeader, pairsGrid, addPairBtn);

        // Click handler for the "Create New Go To Guy Set" button
        createNewSetBtn.addClickListener(e -> {
            try {
                // Create an empty GoToGuySet and save it to the database
                workingSet = callChainPresenter.createNewGoToGuySet(new java.util.ArrayList<>());

                createNewSetBtn.setEnabled(false);  // Disable the button
                createNewSetBtn.getStyle().set("opacity", "0.5"); // Make it visually greyed out
                contentSection.setVisible(true);     // Show the section
                addPairBtn.setEnabled(true);         // Enable Add Pair button

                Notification notification = Notification.show("New Go To Guy Set created");
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification notification = Notification.show("Error creating Go To Guy Set: " + ex.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        section.add(createNewSetBtn, contentSection);
        return section;
    }
    
    private void openAddPairDialog(Grid<GoToGuyPair> pairsGrid) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Go To Guy Pair");

        FormLayout formLayout = new FormLayout();

        ComboBox<User> callerCombo = new ComboBox<>("Caller");
        ComboBox<User> calleeCombo = new ComboBox<>("Callee");

        // Load active users
        java.util.List<User> activeUsers = callChainPresenter.getAllActiveUsers();

        // Get current pairs from the working set
        java.util.List<GoToGuyPair> currentPairs = workingSet.getGoToGuyPairs() != null
            ? workingSet.getGoToGuyPairs()
            : new java.util.ArrayList<>();

        // Filter callers: exclude users who are already assigned someone to call
        java.util.List<User> availableCallers = activeUsers.stream()
            .filter(user -> currentPairs.stream()
                .noneMatch(pair -> pair.getCaller().getId().equals(user.getId())))
            .collect(java.util.stream.Collectors.toList());

        callerCombo.setItems(availableCallers);

        callerCombo.setItemLabelGenerator(this::getUserDisplayName);
        calleeCombo.setItemLabelGenerator(this::getUserDisplayName);

        // Dynamic filtering of callee options based on selected caller
        callerCombo.addValueChangeListener(event -> {
            User selectedCaller = event.getValue();
            if (selectedCaller == null) {
                calleeCombo.setItems(activeUsers);
                return;
            }

            // Filter out invalid callee options based on the rules
            java.util.List<User> validCallees = activeUsers.stream()
                .filter(user -> {
                    // Rule 1: Can't call themselves
                    if (user.getId().equals(selectedCaller.getId())) {
                        return false;
                    }

                    // Rule 2: Can't call someone already being called by someone else
                    boolean alreadyBeingCalled = currentPairs.stream()
                        .anyMatch(pair -> pair.getCallee().getId().equals(user.getId()));
                    if (alreadyBeingCalled) {
                        return false;
                    }

                    return true;
                })
                .collect(java.util.stream.Collectors.toList());

            calleeCombo.setItems(validCallees);
            calleeCombo.clear();
        });

        formLayout.add(callerCombo, calleeCombo);

        Button saveBtn = new Button("Add Pair", VaadinIcon.CHECK.create());
        saveBtn.addClickListener(e -> {
            User caller = callerCombo.getValue();
            User callee = calleeCombo.getValue();

            if (callChainPresenter.validatePair(caller, callee, currentPairs)) {
                GoToGuyPair pair = new GoToGuyPair();
                pair.setCaller(caller);
                pair.setCallee(callee);

                // Add pair to the working set and save to database
                workingSet = callChainPresenter.addPairToSet(workingSet, pair);

                // Update grid with pairs from working set
                pairsGrid.setItems(workingSet.getGoToGuyPairs());

                dialog.close();
                Notification.show("Pair added successfully");
            } else {
                showValidationError(caller, callee, currentPairs);
            }
        });

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        HorizontalLayout dialogButtons = new HorizontalLayout(saveBtn, cancelBtn);
        dialogButtons.setJustifyContentMode(JustifyContentMode.END);

        dialog.add(formLayout, dialogButtons);
        dialog.open();
    }
    
    private void showValidationError(User caller, User callee, java.util.List<GoToGuyPair> existingPairs) {
        String errorMessage;

        if (caller == null || callee == null) {
            errorMessage = "Both caller and callee must be selected";
        } else if (caller.getId().equals(callee.getId())) {
            // Rule 1: A person can't call themselves
            errorMessage = "A person cannot call themselves";
        } else {
            // Rule 2: Check if callee is already being called by someone else
            java.util.Optional<GoToGuyPair> calleeAlreadyAssigned = existingPairs.stream()
                .filter(pair -> pair.getCallee().getId().equals(callee.getId()))
                .findFirst();

            if (calleeAlreadyAssigned.isPresent()) {
                errorMessage = getUserDisplayName(callee) + " is already being called by " +
                    getUserDisplayName(calleeAlreadyAssigned.get().getCaller());
            } else {
                errorMessage = "Invalid pair configuration";
            }
        }

        Notification notification = Notification.show(errorMessage);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private String getUserDisplayName(User user) {
        if (user == null) return "Unknown";
        
        if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) {
            String displayName = user.getFirstName();
            if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) {
                displayName += " " + user.getLastName();
            }
            return displayName;
        }
        
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            return user.getEmail().split("@")[0];
        }
        
        return "User #" + user.getId();
    }
}
