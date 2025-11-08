package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.GoToGuyPair;
import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.vaadin.presenter.CallChainPresenter;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "gtg", layout = MainLayout.class)
@CssImport("./styles/gtg-view.css")
@PageTitle("Go To Guy Chain | TNRA")
public class GTGView extends VerticalLayout {

    private final CallChainPresenter callChainPresenter;
    
    private H2 header;
    private Grid<GoToGuyPair> grid;

    public GTGView(CallChainPresenter callChainPresenter) {
        this.callChainPresenter = callChainPresenter;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("gtg-view");
        
        initComponents();
        loadData();
    }

    private void initComponents() {
        // Header
        header = new H2("Go To Guy Call Chain");
        header.addClassName("gtg-header");
        add(header);

        // Grid
        grid = new Grid<>(GoToGuyPair.class);
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addClassName("gtg-grid");
        
        // Configure grid columns
        grid.addColumn(new ComponentRenderer<HorizontalLayout, GoToGuyPair>(pair -> createUserComponent(pair.getCaller())))
            .setHeader("Name");
            
        grid.addColumn(new ComponentRenderer<HorizontalLayout, GoToGuyPair>(pair -> createUserComponent(pair.getCallee())))
            .setHeader("Calls");
        
        add(grid);
    }

    private HorizontalLayout createUserComponent(User user) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.setAlignItems(Alignment.CENTER);
        layout.setPadding(false);
        layout.addClassName("user-component");
        
        // Profile Image
        Image profileImage = new Image();
        profileImage.addClassName("user-avatar");
        
        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            String imageUrl = callChainPresenter.getFileUrl(user.getProfileImage());
            profileImage.setSrc(imageUrl);
        } else {
            profileImage.setSrc("/uploads/placeholder.png");
        }
        
        // User Info
        VerticalLayout userInfo = new VerticalLayout();
        userInfo.setSpacing(false);
        userInfo.setPadding(false);
        userInfo.addClassName("user-info");
        
        String firstName = user.getFirstName() != null ? user.getFirstName() : 
                          (user.getEmail() != null ? user.getEmail().split("@")[0] : "Unknown");
        
        Span nameSpan = new Span(firstName);
        nameSpan.addClassName("user-name");
        
        String formattedPhone = formatPhoneNumber(user.getPhoneNumber());
        Span phoneSpan = new Span(formattedPhone);
        phoneSpan.addClassName("user-phone");
        
        userInfo.add(nameSpan, phoneSpan);
        
        layout.add(profileImage, userInfo);
        return layout;
    }

    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return "No phone";
        }
        
        // Remove all non-digit characters
        String digits = phoneNumber.replaceAll("\\D", "");
        
        // Format based on length
        if (digits.length() == 10) {
            return String.format("(%s) %s-%s", 
                digits.substring(0, 3), 
                digits.substring(3, 6), 
                digits.substring(6));
        } else if (digits.length() == 11 && digits.startsWith("1")) {
            return String.format("(%s) %s-%s", 
                digits.substring(1, 4), 
                digits.substring(4, 7), 
                digits.substring(7));
        } else {
            // Return original if we can't format it
            return phoneNumber;
        }
    }

    private void loadData() {
        try {
            GoToGuySet latestSet = callChainPresenter.getCurrentGoToGuySet();

            if (latestSet != null && latestSet.getGoToGuyPairs() != null) {
                List<GoToGuyPair> pairs = latestSet.getGoToGuyPairs();
                
                // Update header with date
                String dateStr = "Unknown Date";
                if (latestSet.getStartDate() != null) {
                    dateStr = DateTimeUtils.formatDateTime(latestSet.getStartDate());
                }
                header.setText("Go To Guy Call Chain - " + dateStr);
                
                grid.setItems(pairs);
            } else {
                header.setText("Go To Guy Call Chain - No Data Available");
                grid.setItems();
            }
        } catch (Exception e) {
            header.setText("Go To Guy Call Chain - Error Loading Data");
            grid.setItems();
        }
    }
} 