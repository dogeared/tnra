package com.afitnerd.tnra.vaadin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@PageTitle("Error - TNRA")
@Route(value = "error")
@AnonymousAllowed
@CssImport("./styles/error-view.css")
public class ErrorView extends VerticalLayout implements HasErrorParameter<Exception> {

    private static final Logger logger = LoggerFactory.getLogger(ErrorView.class);

    public ErrorView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
        H1 title = new H1("Oops! Something went wrong");
        title.addClassName("error-title");
        
        Paragraph errorMessage = new Paragraph("""
             We encountered an error while processing your request.
             Please try again or contact support if the problem persists.
        """);
        errorMessage.addClassName("error-message");

        Paragraph errorDetails = new Paragraph(parameter.getException().getMessage());
        errorDetails.addClassName("error-details");
        
        logger.error(parameter.getException().getMessage(), parameter.getException());

        Button homeButton = new Button("Go to Home", e -> {
            getUI().ifPresent(ui -> ui.navigate(""));
        });
        homeButton.addClassName("error-home-button");
        
        add(title, errorMessage, errorDetails, homeButton);
        
        return 500; // Return a default error status code
    }
} 