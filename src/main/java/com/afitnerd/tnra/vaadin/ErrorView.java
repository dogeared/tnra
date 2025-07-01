package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.component.button.Button;
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
public class ErrorView extends VerticalLayout implements HasErrorParameter<Exception> {

    public ErrorView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
        H1 title = new H1("Oops! Something went wrong");
        title.getStyle().set("color", "var(--lumo-error-color)");
        
        Paragraph errorMessage = new Paragraph(
            "We encountered an error while processing your request. " +
            "Please try again or contact support if the problem persists."
        );
        errorMessage.getStyle().set("text-align", "center");
        errorMessage.getStyle().set("max-width", "600px");
        
        Button homeButton = new Button("Go to Home", e -> {
            getUI().ifPresent(ui -> ui.navigate(""));
        });
        homeButton.getStyle().set("margin-top", "2rem");
        
        add(title, errorMessage, homeButton);
        
        return 500; // Return a default error status code
    }
} 