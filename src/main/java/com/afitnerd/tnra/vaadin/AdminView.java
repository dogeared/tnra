package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.GoToGuyPair;
import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.model.GroupSettings;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PersonalStatDefinitionRepository;
import com.afitnerd.tnra.repository.StatDefinitionRepository;
import com.afitnerd.tnra.service.GroupSettingsService;
import com.afitnerd.tnra.service.UserService;
import com.afitnerd.tnra.vaadin.presenter.CallChainPresenter;
import com.afitnerd.tnra.vaadin.presenter.VaadinAdminPresenter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@PageTitle("Admin Dashboard - TNRA")
@Route(value = "admin", layout = MainLayout.class)
@RolesAllowed("ADMIN")
@CssImport("./styles/admin-view.css")
public class AdminView extends VerticalLayout {

    private final VaadinAdminPresenter vaadinAdminPresenter;
    private final CallChainPresenter callChainPresenter;
    private final StatDefinitionRepository statDefinitionRepository;
    private final PersonalStatDefinitionRepository personalStatDefinitionRepository;
    private final UserService userService;
    private final GroupSettingsService groupSettingsService;
    private GoToGuySet workingSet;

    public AdminView(
        VaadinAdminPresenter vaadinAdminPresenter,
        CallChainPresenter callChainPresenter,
        StatDefinitionRepository statDefinitionRepository,
        PersonalStatDefinitionRepository personalStatDefinitionRepository,
        UserService userService,
        GroupSettingsService groupSettingsService
    ) {
        this.vaadinAdminPresenter = vaadinAdminPresenter;
        this.callChainPresenter = callChainPresenter;
        this.statDefinitionRepository = statDefinitionRepository;
        this.personalStatDefinitionRepository = personalStatDefinitionRepository;
        this.userService = userService;
        this.groupSettingsService = groupSettingsService;

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

        tabSheet.add("GTG", createGtgTabContent());
        tabSheet.add("Members", createMembersTabContent());
        tabSheet.add("Stats Config", createStatsConfigTabContent());
        tabSheet.add("Integrations", createIntegrationsTabContent());
        tabSheet.add("Build Info", createBuildInfoTabContent());

        add(tabSheet);
    }

    // ========================
    // Members Tab
    // ========================

    private VerticalLayout createMembersTabContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setSpacing(true);
        content.setPadding(true);

        H3 header = new H3("Group Members");
        header.addClassName("section-header");

        Paragraph description = new Paragraph(
            "Invite members by email. They must create a Keycloak account with the same email to log in."
        );
        description.addClassName("admin-subtitle");

        // Members grid — shows all users (active first, then inactive)
        Grid<User> membersGrid = new Grid<>();
        membersGrid.addColumn(User::getEmail).setHeader("Email").setFlexGrow(2);
        membersGrid.addColumn(this::formatMemberName).setHeader("Name").setFlexGrow(2);
        membersGrid.addColumn(this::formatMemberStatus).setHeader("Status").setFlexGrow(1);

        User currentUser = userService.getCurrentUser();
        membersGrid.addComponentColumn(user ->
            createMemberActionComponent(user, currentUser, membersGrid)
        ).setHeader("Actions").setFlexGrow(1);

        membersGrid.setWidth("100%");
        membersGrid.setMaxWidth("800px");

        refreshMembersGrid(membersGrid);

        // Invite button
        Button inviteBtn = new Button("Invite Member", VaadinIcon.PLUS.create());
        inviteBtn.addClassName("admin-button");
        inviteBtn.addClickListener(e -> openInviteMemberDialog(membersGrid));

        content.add(header, description, membersGrid, inviteBtn);
        return content;
    }

    String formatMemberName(User user) {
        String name = "";
        if (user.getFirstName() != null) name += user.getFirstName();
        if (user.getLastName() != null) name += (name.isEmpty() ? "" : " ") + user.getLastName();
        return name.isEmpty() ? "(not yet logged in)" : name;
    }

    String formatMemberStatus(User user) {
        return Boolean.TRUE.equals(user.getActive()) ? "Active" : "Inactive";
    }

    com.vaadin.flow.component.Component createMemberActionComponent(User user, User currentUser, Grid<User> membersGrid) {
        boolean isSelf = currentUser != null && currentUser.getId().equals(user.getId());
        if (isSelf) {
            return new Span();
        }
        boolean isActive = Boolean.TRUE.equals(user.getActive());
        Button actionBtn = new Button(isActive ? "Deactivate" : "Reactivate");
        if (isActive) {
            actionBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            actionBtn.addClickListener(e -> {
                userService.deactivateUser(user);
                refreshMembersGrid(membersGrid);
                AppNotification.success(getUserDisplayName(user) + " deactivated");
            });
        } else {
            actionBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            actionBtn.addClickListener(e -> {
                userService.reactivateUser(user);
                refreshMembersGrid(membersGrid);
                AppNotification.success(getUserDisplayName(user) + " reactivated");
            });
        }
        return actionBtn;
    }

    void refreshMembersGrid(Grid<User> grid) {
        grid.setItems(userService.getAllUsers());
    }

    void openInviteMemberDialog(Grid<User> membersGrid) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Invite Member");

        TextField emailField = new TextField("Email Address");
        emailField.setWidth("100%");
        emailField.setPlaceholder("member@example.com");

        Paragraph hint = new Paragraph(
            "The member will need to create a Keycloak account with this exact email address to log in."
        );
        hint.addClassName("admin-archived-note");

        dialog.add(emailField, hint);

        Button saveBtn = new Button("Invite", VaadinIcon.CHECK.create());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(e -> {
            String email = emailField.getValue().trim().toLowerCase();
            if (email.isEmpty() || !email.contains("@")) {
                AppNotification.error("Please enter a valid email address");
                return;
            }

            User existing = userService.getUserByEmail(email);
            if (existing != null) {
                AppNotification.error("A member with this email already exists");
                return;
            }

            userService.inviteUser(email);
            dialog.close();
            refreshMembersGrid(membersGrid);
            AppNotification.success("Member invited: " + email);
        });

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    // ========================
    // Stats Config Tab
    // ========================

    private VerticalLayout createStatsConfigTabContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setSpacing(true);
        content.setPadding(true);

        H3 header = new H3("Stats Configuration");
        header.addClassName("section-header");

        Paragraph description = new Paragraph(
            "Configure the stats your group tracks weekly. Members will see these in their post form."
        );
        description.addClassName("admin-subtitle");

        VerticalLayout statsList = new VerticalLayout();
        statsList.setSpacing(false);
        statsList.setPadding(false);
        statsList.setWidth("100%");
        statsList.setMaxWidth("600px");

        Paragraph archivedNote = new Paragraph(
            "Archived stats still appear on older posts but won't show in new post forms."
        );
        archivedNote.addClassName("admin-archived-note");

        refreshStatsList(statsList);

        Button addStatBtn = new Button("Add Stat", VaadinIcon.PLUS.create());
        addStatBtn.addClassName("admin-button");
        addStatBtn.addClickListener(e -> openAddStatDialog(statsList));

        content.add(header, description, statsList, archivedNote, addStatBtn);
        return content;
    }

    void refreshStatsList(VerticalLayout statsList) {
        statsList.removeAll();
        List<StatDefinition> allStats = statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc();

        if (allStats.isEmpty()) {
            Paragraph empty = new Paragraph("No stats configured yet. Add your first stat below.");
            empty.addClassName("admin-empty-state");
            statsList.add(empty);
            return;
        }

        for (int i = 0; i < allStats.size(); i++) {
            StatDefinition stat = allStats.get(i);
            statsList.add(createStatRow(stat, i, allStats.size(), statsList));
        }
    }

    private HorizontalLayout createStatRow(StatDefinition stat, int index, int totalCount, VerticalLayout statsList) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.addClassName("stat-row");

        if (stat.getArchived()) {
            row.addClassName("archived");
        }

        // Emoji + label
        Span emojiSpan = new Span(stat.getEmoji() != null ? stat.getEmoji() : "");
        emojiSpan.addClassName("stat-row-emoji");

        Span labelSpan = new Span(stat.getLabel());
        labelSpan.addClassName("stat-row-label");

        if (stat.getArchived()) {
            Span archivedBadge = new Span("archived");
            archivedBadge.addClassName("stat-row-archived-badge");
            row.add(emojiSpan, labelSpan, archivedBadge);
        } else {
            // Up/down buttons for reordering (use active-only index)
            List<StatDefinition> activeStats = statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc();
            int activeIndex = findActiveIndex(activeStats, stat);

            Button upBtn = new Button(VaadinIcon.ARROW_UP.create());
            upBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            upBtn.setEnabled(activeIndex > 0);
            upBtn.addClickListener(e -> moveStatUp(stat, statsList));

            Button downBtn = new Button(VaadinIcon.ARROW_DOWN.create());
            downBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            downBtn.setEnabled(activeIndex >= 0 && activeIndex < activeStats.size() - 1);
            downBtn.addClickListener(e -> moveStatDown(stat, statsList));

            // Archive button
            Button archiveBtn = new Button(VaadinIcon.CLOSE_SMALL.create());
            archiveBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            archiveBtn.setAriaLabel("Archive " + stat.getLabel());
            archiveBtn.addClickListener(e -> archiveStat(stat, statsList));

            row.add(emojiSpan, labelSpan, upBtn, downBtn, archiveBtn);
        }

        // Restore button for archived stats
        if (stat.getArchived()) {
            Button restoreBtn = new Button("Restore");
            restoreBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            restoreBtn.addClickListener(e -> restoreStat(stat, statsList));
            row.add(restoreBtn);
        }

        return row;
    }

    private int findActiveIndex(List<StatDefinition> activeStats, StatDefinition stat) {
        for (int i = 0; i < activeStats.size(); i++) {
            if (activeStats.get(i).getId().equals(stat.getId())) {
                return i;
            }
        }
        return -1;
    }

    void moveStatUp(StatDefinition stat, VerticalLayout statsList) {
        List<StatDefinition> activeStats = statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc();
        int index = findActiveIndex(activeStats, stat);
        if (index > 0) {
            StatDefinition prev = activeStats.get(index - 1);
            StatDefinition current = activeStats.get(index);
            int tempOrder = current.getDisplayOrder();
            current.setDisplayOrder(prev.getDisplayOrder());
            prev.setDisplayOrder(tempOrder);
            statDefinitionRepository.save(current);
            statDefinitionRepository.save(prev);
            refreshStatsList(statsList);
        }
    }

    void moveStatDown(StatDefinition stat, VerticalLayout statsList) {
        List<StatDefinition> activeStats = statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc();
        int index = findActiveIndex(activeStats, stat);
        if (index >= 0 && index < activeStats.size() - 1) {
            StatDefinition next = activeStats.get(index + 1);
            StatDefinition current = activeStats.get(index);
            int tempOrder = current.getDisplayOrder();
            current.setDisplayOrder(next.getDisplayOrder());
            next.setDisplayOrder(tempOrder);
            statDefinitionRepository.save(current);
            statDefinitionRepository.save(next);
            refreshStatsList(statsList);
        }
    }

    void archiveStat(StatDefinition stat, VerticalLayout statsList) {
        // Check if this is the last active stat
        List<StatDefinition> activeStats = statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc();
        if (activeStats.size() <= 1) {
            AppNotification.error("Cannot archive the last active stat. At least one stat is required.");
            return;
        }

        stat.setArchived(true);
        statDefinitionRepository.save(stat);
        refreshStatsList(statsList);
        AppNotification.success(stat.getLabel() + " archived");
    }

    void restoreStat(StatDefinition stat, VerticalLayout statsList) {
        stat.setArchived(false);
        // Put restored stat at the end of the active list
        List<StatDefinition> activeStats = statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc();
        int maxOrder = activeStats.stream()
            .mapToInt(StatDefinition::getDisplayOrder)
            .max()
            .orElse(-1);
        stat.setDisplayOrder(maxOrder + 1);
        statDefinitionRepository.save(stat);
        refreshStatsList(statsList);
        AppNotification.success(stat.getLabel() + " restored");
    }

    void openAddStatDialog(VerticalLayout statsList) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add New Stat");

        FormLayout formLayout = new FormLayout();

        TextField nameField = new TextField("Internal Name");
        nameField.setHelperText("Lowercase, no spaces (e.g., 'journaling')");
        nameField.setPattern("[a-z_]+");

        TextField labelField = new TextField("Display Label");
        labelField.setHelperText("What members see (e.g., 'Journaling')");

        TextField emojiField = new TextField("Emoji");
        emojiField.setHelperText("Single emoji (e.g., '📝')");
        emojiField.setMaxLength(10);
        emojiField.setWidth("80px");

        formLayout.add(nameField, labelField, emojiField);

        Button saveBtn = new Button("Add Stat", VaadinIcon.CHECK.create());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(e -> {
            String name = nameField.getValue().trim().toLowerCase();
            String label = labelField.getValue().trim();
            String emoji = emojiField.getValue().trim();

            if (name.isEmpty() || label.isEmpty()) {
                AppNotification.error("Name and label are required");
                return;
            }

            boolean globalNameExists = statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()
                .stream().anyMatch(s -> name.equals(s.getName()));
            boolean personalNameExists = personalStatDefinitionRepository.findByArchivedFalse()
                .stream().anyMatch(s -> name.equals(s.getName()));
            if (globalNameExists || personalNameExists) {
                AppNotification.error("A stat with name '" + name + "' already exists");
                return;
            }

            List<StatDefinition> activeStats = statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc();
            int nextOrder = activeStats.stream()
                .mapToInt(StatDefinition::getDisplayOrder)
                .max()
                .orElse(-1) + 1;

            StatDefinition newStat = new StatDefinition(name, label, emoji.isEmpty() ? null : emoji, nextOrder);
            statDefinitionRepository.save(newStat);

            refreshStatsList(statsList);
            dialog.close();
            AppNotification.success(label + " added");
        });

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        HorizontalLayout dialogButtons = new HorizontalLayout(saveBtn, cancelBtn);
        dialogButtons.setJustifyContentMode(JustifyContentMode.END);

        dialog.add(formLayout, dialogButtons);
        dialog.open();
    }

    // ========================
    // Integrations Tab
    // ========================

    VerticalLayout createIntegrationsTabContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setSpacing(true);
        content.setPadding(true);

        H3 header = new H3("Integrations");
        header.addClassName("section-header");

        Paragraph description = new Paragraph(
            "Configure integrations. Activity notifications include username, start time, finish time, and a link to the post. No post content is sent."
        );
        description.addClassName("admin-subtitle");

        H3 slackHeader = new H3("Slack");
        slackHeader.addClassName("section-header");

        GroupSettings settings = groupSettingsService.getSettings();

        TextField webhookUrlField = new TextField("Incoming Webhook URL");
        webhookUrlField.setPlaceholder("https://hooks.slack.com/services/...");
        webhookUrlField.setWidth("100%");
        webhookUrlField.setMaxWidth("600px");
        webhookUrlField.setValue(settings.getSlackWebhookUrl() != null ? settings.getSlackWebhookUrl() : "");

        Checkbox enabledCheckbox = new Checkbox("Enable Slack notifications");
        enabledCheckbox.setValue(settings.isSlackEnabled());

        Button saveBtn = new Button("Save", VaadinIcon.CHECK.create());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(e -> {
            settings.setSlackWebhookUrl(webhookUrlField.getValue().isBlank() ? null : webhookUrlField.getValue().trim());
            settings.setSlackEnabled(enabledCheckbox.getValue());
            groupSettingsService.save(settings);
            AppNotification.success("Slack settings saved");
        });

        content.add(header, description, slackHeader, webhookUrlField, enabledCheckbox, saveBtn);
        return content;
    }

    // ========================
    // Build Info Tab
    // ========================

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
        row.addClassName("info-row");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName("info-row-label");

        Span valueSpan = new Span(value != null ? value : "N/A");

        row.add(labelSpan, valueSpan);
        return row;
    }

    // ========================
    // GTG Tab (existing)
    // ========================

    private VerticalLayout createGtgTabContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setSpacing(true);
        content.setPadding(true);

        H3 gtgHeader = new H3("Go To Guy Management");
        gtgHeader.addClassName("section-header");

        VerticalLayout currentSetSection = createCurrentGTGSetSection();
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

        Button createNewSetBtn = new Button("Create New Go To Guy Set", VaadinIcon.PLUS.create());
        createNewSetBtn.addClassName("admin-button");

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
                workingSet = callChainPresenter.removePairFromSet(workingSet, pair);
                pairsGrid.setItems(workingSet.getGoToGuyPairs());
                AppNotification.success("Pair removed successfully");
            });
            return deleteBtn;
        }).setHeader("Actions").setWidth("120px").setFlexGrow(0);
        pairsGrid.setHeight("300px");

        Button addPairBtn = new Button("Add Pair", VaadinIcon.PLUS.create());
        addPairBtn.addClassName("admin-button");
        addPairBtn.setEnabled(false);
        addPairBtn.addClickListener(e -> openAddPairDialog(pairsGrid));

        contentSection.add(sectionHeader, pairsGrid, addPairBtn);

        createNewSetBtn.addClickListener(e -> {
            try {
                workingSet = callChainPresenter.createNewGoToGuySet(new java.util.ArrayList<>());
                createNewSetBtn.setEnabled(false);
                createNewSetBtn.addClassName("disabled");
                contentSection.setVisible(true);
                addPairBtn.setEnabled(true);

                AppNotification.success("New Go To Guy Set created");
            } catch (Exception ex) {
                AppNotification.error("Error creating Go To Guy Set: " + ex.getMessage());
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

        java.util.List<User> activeUsers = callChainPresenter.getAllActiveUsers();

        java.util.List<GoToGuyPair> currentPairs = workingSet.getGoToGuyPairs() != null
            ? workingSet.getGoToGuyPairs()
            : new java.util.ArrayList<>();

        java.util.List<User> availableCallers = activeUsers.stream()
            .filter(user -> currentPairs.stream()
                .noneMatch(pair -> pair.getCaller().getId().equals(user.getId())))
            .collect(java.util.stream.Collectors.toList());

        callerCombo.setItems(availableCallers);
        callerCombo.setItemLabelGenerator(this::getUserDisplayName);
        calleeCombo.setItemLabelGenerator(this::getUserDisplayName);

        callerCombo.addValueChangeListener(event -> {
            User selectedCaller = event.getValue();
            if (selectedCaller == null) {
                calleeCombo.setItems(activeUsers);
                return;
            }

            java.util.List<User> validCallees = activeUsers.stream()
                .filter(user -> {
                    if (user.getId().equals(selectedCaller.getId())) return false;
                    boolean alreadyBeingCalled = currentPairs.stream()
                        .anyMatch(pair -> pair.getCallee().getId().equals(user.getId()));
                    return !alreadyBeingCalled;
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
                workingSet = callChainPresenter.addPairToSet(workingSet, pair);
                pairsGrid.setItems(workingSet.getGoToGuyPairs());
                dialog.close();
                AppNotification.success("Pair added successfully");
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

    void showValidationError(User caller, User callee, java.util.List<GoToGuyPair> existingPairs) {
        String errorMessage;

        if (caller == null || callee == null) {
            errorMessage = "Both caller and callee must be selected";
        } else if (caller.getId().equals(callee.getId())) {
            errorMessage = "A person cannot call themselves";
        } else {
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

        AppNotification.error(errorMessage);
    }

    // ========================
    // Utilities
    // ========================

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
