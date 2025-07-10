package com.afitnerd.tnra.vaadin;

import java.io.IOException;
import java.io.InputStream;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.FileStorageService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.button.Button;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "profile", layout = MainLayout.class)
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
        
        phoneNumberField = new TextField("Phone Number");
        phoneNumberField.setWidth("100%");
        phoneNumberField.setMaxLength(20);
        phoneNumberField.setPlaceholder("(555) 123-4567");
        
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

    private void loadUserData() {
        currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            firstNameField.setValue(currentUser.getFirstName() != null ? currentUser.getFirstName() : "");
            lastNameField.setValue(currentUser.getLastName() != null ? currentUser.getLastName() : "");
            phoneNumberField.setValue(currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "");
            
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
            currentUser.setFirstName(firstNameField.getValue());
            currentUser.setLastName(lastNameField.getValue());
            currentUser.setPhoneNumber(phoneNumberField.getValue());
            
            try {
                userService.saveUser(currentUser);
                Notification.show("Profile saved successfully", 3000, Notification.Position.TOP_CENTER);
            } catch (Exception e) {
                Notification.show("Error saving profile", 3000, Notification.Position.TOP_CENTER);
            }
        }
    }
} 