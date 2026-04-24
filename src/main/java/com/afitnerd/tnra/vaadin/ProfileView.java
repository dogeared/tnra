package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.PersonalStatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PersonalStatDefinitionRepository;
import com.afitnerd.tnra.repository.StatDefinitionRepository;
import com.afitnerd.tnra.service.FileStorageService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

@Route(value = "profile", layout = MainLayout.class)
@PermitAll
@CssImport("./styles/profile-view.css")
@PageTitle("Profile | TNRA")
public class ProfileView extends VerticalLayout {

    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final StatDefinitionRepository statDefinitionRepository;
    private final PersonalStatDefinitionRepository personalStatDefinitionRepository;
    private User currentUser;
    private VerticalLayout myStatsList;
    
    private TextField firstNameField;
    private TextField lastNameField;
    private TextField phoneNumberField;
    private Image profileImage;
    private Upload imageUpload;
    private Button saveButton;
    private Checkbox notifyNewPostsCheckbox;
    
    // Phone number validation pattern
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\(?([0-9]{3})\\)?[-.\\s]?([0-9]{3})[-.\\s]?([0-9]{4})$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("[^0-9]");

    public ProfileView(
        UserService userService, FileStorageService fileStorageService,
        StatDefinitionRepository statDefinitionRepository,
        PersonalStatDefinitionRepository personalStatDefinitionRepository
    ) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.statDefinitionRepository = statDefinitionRepository;
        this.personalStatDefinitionRepository = personalStatDefinitionRepository;
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("profile-view");
        
        initComponents();
        loadUserData();
    }

    private void initComponents() {
        // Header
        H2 header = new H2("Profile");
        header.addClassName("profile-title");
        add(header);

        // Profile Image Section
        VerticalLayout imageSection = new VerticalLayout();
        imageSection.setSpacing(true);
        imageSection.setPadding(false);
        imageSection.addClassName("profile-image-section");
        
        profileImage = new Image();
        profileImage.setWidth("120px");
        profileImage.setHeight("120px");
        profileImage.addClassName("profile-image");

        UI ui = UI.getCurrent();
        imageUpload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            if (ui != null) {
                ui.access(() -> processProfileImageUpload(metadata.fileName(), metadata.contentType(), data));
            } else {
                processProfileImageUpload(metadata.fileName(), metadata.contentType(), data);
            }
        }));
        imageUpload.setAcceptedFileTypes("image/*");
        imageUpload.setMaxFileSize(5 * 1024 * 1024); // 5MB limit
        imageUpload.setDropLabel(new Div(new Paragraph("Drop image here or click to upload")));
        imageUpload.setUploadButton(new Button("Upload Image"));
        
        imageSection.add(profileImage, imageUpload);
        
        // Form Fields
        firstNameField = new TextField("First Name");
        firstNameField.setWidth("100%");
        firstNameField.setMaxLength(50);
        
        lastNameField = new TextField("Last Name");
        lastNameField.setWidth("100%");
        lastNameField.setMaxLength(50);
        
        // Phone number field with formatting and validation
        phoneNumberField = createPhoneNumberField();
        
        // Notification Preferences
        H3 notifHeader = new H3("Email Notifications");
        notifyNewPostsCheckbox = new Checkbox("Notify me when someone posts a weekly update");
        notifyNewPostsCheckbox.setValue(true);

        VerticalLayout notifSection = new VerticalLayout(notifHeader, notifyNewPostsCheckbox);
        notifSection.setSpacing(true);
        notifSection.setPadding(false);

        // Save Button
        saveButton = new Button("Save Changes");
        saveButton.addClickListener(e -> saveProfile());

        // Layout
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSpacing(true);
        formLayout.setPadding(false);
        formLayout.addClassName("form-section");
        formLayout.add(firstNameField, lastNameField, phoneNumberField, notifSection, saveButton);
        
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSpacing(true);
        mainLayout.setPadding(false);
        mainLayout.add(imageSection, formLayout);
        mainLayout.setFlexGrow(1, formLayout);
        
        add(mainLayout);

        // My Stats section
        add(createMyStatsSection());
    }

    private VerticalLayout createMyStatsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(false);

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setAlignItems(Alignment.CENTER);
        H3 statsHeader = new H3("My Stats");
        Button addStatBtn = new Button("Add Stat", VaadinIcon.PLUS.create());
        addStatBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        addStatBtn.addClickListener(e -> openAddPersonalStatDialog());
        headerRow.add(statsHeader, addStatBtn);

        myStatsList = new VerticalLayout();
        myStatsList.setSpacing(false);
        myStatsList.setPadding(false);

        section.add(headerRow, myStatsList);
        return section;
    }

    void refreshMyStatsList() {
        if (myStatsList == null || currentUser == null) return;
        myStatsList.removeAll();

        java.util.List<PersonalStatDefinition> allStats = personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(currentUser);

        if (allStats.isEmpty()) {
            Paragraph empty = new Paragraph("No personal stats yet. Add your first to track something just for you.");
            myStatsList.add(empty);
            return;
        }

        java.util.List<PersonalStatDefinition> activeStats = allStats.stream().filter(s -> !s.getArchived()).toList();
        java.util.List<PersonalStatDefinition> archivedStats = allStats.stream().filter(PersonalStatDefinition::getArchived).toList();

        for (int i = 0; i < activeStats.size(); i++) {
            PersonalStatDefinition stat = activeStats.get(i);
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setAlignItems(Alignment.CENTER);

            Span emoji = new Span(stat.getEmoji() != null ? stat.getEmoji() : "");
            Span label = new Span(stat.getLabel());

            Button upBtn = new Button(VaadinIcon.ARROW_UP.create());
            upBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            upBtn.setEnabled(i > 0);
            upBtn.addClickListener(e -> movePersonalStatUp(stat));

            Button downBtn = new Button(VaadinIcon.ARROW_DOWN.create());
            downBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            downBtn.setEnabled(i < activeStats.size() - 1);
            downBtn.addClickListener(e -> movePersonalStatDown(stat));

            Button archiveBtn = new Button(VaadinIcon.CLOSE_SMALL.create());
            archiveBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            archiveBtn.addClickListener(e -> archivePersonalStat(stat));

            row.add(emoji, label, upBtn, downBtn, archiveBtn);
            myStatsList.add(row);
        }

        for (PersonalStatDefinition stat : archivedStats) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setAlignItems(Alignment.CENTER);
            row.getStyle().set("opacity", "0.5");

            Span emoji = new Span(stat.getEmoji() != null ? stat.getEmoji() : "");
            Span label = new Span(stat.getLabel());
            Span badge = new Span("archived");
            badge.getStyle().set("font-size", "0.8em").set("color", "gray");

            Button restoreBtn = new Button("Restore");
            restoreBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            restoreBtn.addClickListener(e -> restorePersonalStat(stat));

            row.add(emoji, label, badge, restoreBtn);
            myStatsList.add(row);
        }
    }

    void openAddPersonalStatDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Personal Stat");

        FormLayout form = new FormLayout();
        TextField nameField = new TextField("Internal Name");
        nameField.setHelperText("Lowercase, no spaces (e.g., 'guitar_practice')");
        nameField.setPattern("[a-z_]+");

        TextField labelField = new TextField("Display Label");
        labelField.setHelperText("What you see (e.g., 'Guitar Practice')");

        TextField emojiField = new TextField("Emoji");
        emojiField.setMaxLength(10);
        emojiField.setWidth("80px");

        form.add(nameField, labelField, emojiField);

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

            // Check collision with global stats (active or archived) — in-memory after encryption
            boolean globalNameExists = statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()
                .stream().anyMatch(s -> name.equals(s.getName()));
            if (globalNameExists) {
                AppNotification.error("A group stat named '" + name + "' already exists");
                return;
            }

            // Check collision with own personal stats — in-memory after encryption
            boolean personalNameExists = personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(currentUser)
                .stream().anyMatch(s -> name.equals(s.getName()));
            if (personalNameExists) {
                AppNotification.error("You already have a stat named '" + name + "'");
                return;
            }

            java.util.List<PersonalStatDefinition> activeStats = personalStatDefinitionRepository
                .findByUserAndArchivedFalseOrderByDisplayOrderAsc(currentUser);
            int nextOrder = activeStats.stream()
                .mapToInt(PersonalStatDefinition::getDisplayOrder)
                .max().orElse(-1) + 1;

            PersonalStatDefinition newStat = new PersonalStatDefinition(
                name, label, emoji.isEmpty() ? null : emoji, nextOrder, currentUser
            );
            personalStatDefinitionRepository.save(newStat);
            refreshMyStatsList();
            dialog.close();

            AppNotification.success(label + " added");
        });

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    void movePersonalStatUp(PersonalStatDefinition stat) {
        java.util.List<PersonalStatDefinition> activeStats = personalStatDefinitionRepository
            .findByUserAndArchivedFalseOrderByDisplayOrderAsc(currentUser);
        for (int i = 1; i < activeStats.size(); i++) {
            if (activeStats.get(i).getId().equals(stat.getId())) {
                PersonalStatDefinition prev = activeStats.get(i - 1);
                int temp = stat.getDisplayOrder();
                stat.setDisplayOrder(prev.getDisplayOrder());
                prev.setDisplayOrder(temp);
                personalStatDefinitionRepository.save(stat);
                personalStatDefinitionRepository.save(prev);
                break;
            }
        }
        refreshMyStatsList();
    }

    void movePersonalStatDown(PersonalStatDefinition stat) {
        java.util.List<PersonalStatDefinition> activeStats = personalStatDefinitionRepository
            .findByUserAndArchivedFalseOrderByDisplayOrderAsc(currentUser);
        for (int i = 0; i < activeStats.size() - 1; i++) {
            if (activeStats.get(i).getId().equals(stat.getId())) {
                PersonalStatDefinition next = activeStats.get(i + 1);
                int temp = stat.getDisplayOrder();
                stat.setDisplayOrder(next.getDisplayOrder());
                next.setDisplayOrder(temp);
                personalStatDefinitionRepository.save(stat);
                personalStatDefinitionRepository.save(next);
                break;
            }
        }
        refreshMyStatsList();
    }

    void archivePersonalStat(PersonalStatDefinition stat) {
        stat.setArchived(true);
        personalStatDefinitionRepository.save(stat);
        refreshMyStatsList();
        AppNotification.success(stat.getLabel() + " archived");
    }

    void restorePersonalStat(PersonalStatDefinition stat) {
        // Check if a global stat now has this name — in-memory after encryption
        boolean globalNameExists = statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()
            .stream().anyMatch(s -> stat.getName().equals(s.getName()));
        if (globalNameExists) {
            AppNotification.error("Can't restore: a group stat named '" + stat.getName() + "' now exists");
            return;
        }

        stat.setArchived(false);
        java.util.List<PersonalStatDefinition> activeStats = personalStatDefinitionRepository
            .findByUserAndArchivedFalseOrderByDisplayOrderAsc(currentUser);
        int maxOrder = activeStats.stream()
            .mapToInt(PersonalStatDefinition::getDisplayOrder)
            .max().orElse(-1);
        stat.setDisplayOrder(maxOrder + 1);
        personalStatDefinitionRepository.save(stat);
        refreshMyStatsList();
        AppNotification.success(stat.getLabel() + " restored");
    }

    void processProfileImageUpload(String fileName, String contentType, byte[] data) {
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            if (currentUser.getProfileImage() != null && !currentUser.getProfileImage().isEmpty()) {
                fileStorageService.deleteFile(currentUser.getProfileImage());
            }

            String storedFileName = fileStorageService.storeFile(inputStream, fileName, contentType);
            currentUser.setProfileImage(storedFileName);
            userService.saveUser(currentUser);

            String imageUrl = fileStorageService.getFileUrl(storedFileName);
            profileImage.setSrc(imageUrl);

            AppNotification.success("Image uploaded successfully");
        } catch (IOException e) {
            AppNotification.error("Error uploading image");
        }
    }
    
    private TextField createPhoneNumberField() {
        TextField phoneField = new TextField("Phone Number");
        phoneField.setWidth("100%");
        phoneField.setMaxLength(20);
        phoneField.setPlaceholder("(555) 123-4567");
        
        // Set value change mode to EAGER for real-time formatting
        phoneField.setValueChangeMode(ValueChangeMode.EAGER);
        phoneField.setValueChangeTimeout(100);
        
        // Add value change listener for formatting
        phoneField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value != null && !value.trim().isEmpty()) {
                String formatted = formatPhoneNumber(value);
                if (!formatted.equals(value)) {
                    // Update the field with formatted value
                    phoneField.setValue(formatted);
                }
                
                // Validate and show error if invalid
                if (!isValidPhoneNumber(formatted)) {
                    phoneField.setErrorMessage("Please enter a valid phone number");
                    phoneField.setInvalid(true);
                } else {
                    phoneField.setErrorMessage(null);
                    phoneField.setInvalid(false);
                }
            } else {
                phoneField.setErrorMessage(null);
                phoneField.setInvalid(false);
            }
        });
        
        return phoneField;
    }
    
    String formatPhoneNumber(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        
        // Remove all non-digit characters
        String digitsOnly = DIGITS_ONLY.matcher(input).replaceAll("");
        
        // Format based on number of digits
        if (digitsOnly.length() == 0) {
            return "";
        } else if (digitsOnly.length() <= 3) {
            return digitsOnly;
        } else if (digitsOnly.length() <= 6) {
            return "(" + digitsOnly.substring(0, 3) + ") " + digitsOnly.substring(3);
        } else if (digitsOnly.length() <= 10) {
            return "(" + digitsOnly.substring(0, 3) + ") " + digitsOnly.substring(3, 6) + "-" + digitsOnly.substring(6);
        } else {
            // If more than 10 digits, truncate to 10
            return "(" + digitsOnly.substring(0, 3) + ") " + digitsOnly.substring(3, 6) + "-" + digitsOnly.substring(6, 10);
        }
    }
    
    boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return true; // Allow empty phone numbers
        }
        return PHONE_PATTERN.matcher(phoneNumber).matches();
    }
    
    String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return "";
        }
        // Remove all formatting and return digits only
        return DIGITS_ONLY.matcher(phoneNumber).replaceAll("");
    }

    private void loadUserData() {
        currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            firstNameField.setValue(currentUser.getFirstName() != null ? currentUser.getFirstName() : "");
            lastNameField.setValue(currentUser.getLastName() != null ? currentUser.getLastName() : "");
            
            // Format the phone number when loading
            String phoneNumber = currentUser.getPhoneNumber();
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                phoneNumberField.setValue(formatPhoneNumber(phoneNumber));
            } else {
                phoneNumberField.setValue("");
            }

            notifyNewPostsCheckbox.setValue(Boolean.TRUE.equals(currentUser.getNotifyNewPosts()));

            if (currentUser.getProfileImage() != null && !currentUser.getProfileImage().isEmpty()) {
                String imageUrl = fileStorageService.getFileUrl(currentUser.getProfileImage());
                profileImage.setSrc(imageUrl);
            } else {
                // Set default avatar
                profileImage.setSrc("/uploads/placeholder.png");
            }

            refreshMyStatsList();
        }
    }

    void saveProfile() {
        if (currentUser != null) {
            // Validate phone number before saving
            String phoneNumber = phoneNumberField.getValue();
            if (phoneNumber != null && !phoneNumber.trim().isEmpty() && !isValidPhoneNumber(phoneNumber)) {
                AppNotification.error("Please enter a valid phone number");
                return;
            }
            
            currentUser.setFirstName(firstNameField.getValue());
            currentUser.setLastName(lastNameField.getValue());
            // Store normalized phone number (digits only)
            currentUser.setPhoneNumber(normalizePhoneNumber(phoneNumber));
            currentUser.setNotifyNewPosts(notifyNewPostsCheckbox.getValue());
            
            try {
                userService.saveUser(currentUser);
                AppNotification.success("Profile saved successfully");
            } catch (Exception e) {
                AppNotification.error("Error saving profile");
            }
        }
    }
} 
