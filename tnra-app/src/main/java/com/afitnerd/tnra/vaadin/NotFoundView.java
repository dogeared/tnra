package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@AnonymousAllowed
@CssImport("./styles/error-view.css")
public class NotFoundView extends VerticalLayout implements HasErrorParameter<NotFoundException> {

    public NotFoundView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        H1 title = new H1("Page not found");
        title.addClassName("error-title");

        Paragraph message = new Paragraph("The page you're looking for doesn't exist or you don't have access to it.");
        message.addClassName("error-message");

        Button homeButton = new Button("Go to Home", e -> {
            getUI().ifPresent(ui -> ui.navigate(""));
        });
        homeButton.addClassName("error-home-button");

        add(title, message, homeButton);

        return 404;
    }
}
