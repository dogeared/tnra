package com.afitnerd.tnra.vaadin;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.FileStorageService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "profile", layout = MainLayout.class)
@CssImport("./styles/profile-view.css")
@PageTitle("Profile | TNRA")
public class ProfileView extends VerticalLayout {

    private final UserService userService;
    private final FileStorageService fileStorageService;
    private User currentUser;
    
    private TextField firstNameField;
    private TextField lastNameField;
    private TextField phoneNumberField;
    private Image profileImage;
    private Upload imageUpload;
    private Button saveButton;
    
    // Phone number validation pattern
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\(?([0-9]{3})\\)?[-.\\s]?([0-9]{3})[-.\\s]?([0-9]{4})$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("[^0-9]");

    public ProfileView(UserService userService, FileStorageService fileStorageService) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        
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
        header.getStyle().set("margin", "0");
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
        
        MemoryBuffer buffer = new MemoryBuffer();
        imageUpload = new Upload(buffer);
        imageUpload.setAcceptedFileTypes("image/*");
        imageUpload.setMaxFileSize(5 * 1024 * 1024); // 5MB limit
        imageUpload.setDropLabel(new Div(new Paragraph("Drop image here or click to upload")));
        imageUpload.setUploadButton(new Button("Upload Image"));
        
        imageUpload.addSucceededListener(event -> {
            try {
                InputStream inputStream = buffer.getInputStream();
                String fileName = event.getFileName();
                String contentType = event.getMIMEType();
                
                // Delete old profile image if it exists
                if (currentUser.getProfileImage() != null && !currentUser.getProfileImage().isEmpty()) {
                    fileStorageService.deleteFile(currentUser.getProfileImage());
                }
                
                // Store the file and get the filename
                String storedFileName = fileStorageService.storeFile(inputStream, fileName, contentType);
                
                // Update the user's profile image reference
                currentUser.setProfileImage(storedFileName);
                
                // Update the image display
                String imageUrl = fileStorageService.getFileUrl(storedFileName);
                profileImage.setSrc(imageUrl);
                
                Notification.show("Image uploaded successfully", 3000, Notification.Position.TOP_CENTER);
            } catch (IOException e) {
                Notification.show("Error uploading image", 3000, Notification.Position.TOP_CENTER);
            }
        });
        
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
        
        // Save Button
        saveButton = new Button("Save Changes");
        saveButton.addClickListener(e -> saveProfile());
        
        // Layout
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSpacing(true);
        formLayout.setPadding(false);
        formLayout.addClassName("form-section");
        formLayout.add(firstNameField, lastNameField, phoneNumberField, saveButton);
        
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSpacing(true);
        mainLayout.setPadding(false);
        mainLayout.add(imageSection, formLayout);
        mainLayout.setFlexGrow(1, formLayout);
        
        add(mainLayout);
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
    
    private String formatPhoneNumber(String input) {
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
    
    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return true; // Allow empty phone numbers
        }
        return PHONE_PATTERN.matcher(phoneNumber).matches();
    }
    
    private String normalizePhoneNumber(String phoneNumber) {
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
            
            if (currentUser.getProfileImage() != null && !currentUser.getProfileImage().isEmpty()) {
                String imageUrl = fileStorageService.getFileUrl(currentUser.getProfileImage());
                profileImage.setSrc(imageUrl);
            } else {
                // Set default avatar
                profileImage.setSrc("/uploads/placeholder.png");
            }
        }
    }

    private void saveProfile() {
        if (currentUser != null) {
            // Validate phone number before saving
            String phoneNumber = phoneNumberField.getValue();
            if (phoneNumber != null && !phoneNumber.trim().isEmpty() && !isValidPhoneNumber(phoneNumber)) {
                Notification.show("Please enter a valid phone number", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            
            currentUser.setFirstName(firstNameField.getValue());
            currentUser.setLastName(lastNameField.getValue());
            // Store normalized phone number (digits only)
            currentUser.setPhoneNumber(normalizePhoneNumber(phoneNumber));
            
            try {
                userService.saveUser(currentUser);
                Notification.show("Profile saved successfully", 3000, Notification.Position.TOP_CENTER);
            } catch (Exception e) {
                Notification.show("Error saving profile", 3000, Notification.Position.TOP_CENTER);
            }
        }
    }
} 