package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class NotFoundViewTest {

    @Test
    void setErrorParameterReturns404() {
        NotFoundView view = new NotFoundView();

        @SuppressWarnings("unchecked")
        ErrorParameter<NotFoundException> parameter = mock(ErrorParameter.class);

        int status = view.setErrorParameter(mock(BeforeEnterEvent.class), parameter);

        assertEquals(404, status);
    }

    @Test
    void setErrorParameterAddsThreeComponents() {
        NotFoundView view = new NotFoundView();

        @SuppressWarnings("unchecked")
        ErrorParameter<NotFoundException> parameter = mock(ErrorParameter.class);

        view.setErrorParameter(mock(BeforeEnterEvent.class), parameter);

        // H1 + Paragraph + Button = 3 children
        assertEquals(3, view.getChildren().count());
    }

    @Test
    void layoutIsCentered() {
        NotFoundView view = new NotFoundView();

        assertEquals(
            com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER,
            view.getAlignItems()
        );
        assertEquals(
            com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.CENTER,
            view.getJustifyContentMode()
        );
    }
}
