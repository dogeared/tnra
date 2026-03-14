package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErrorViewTest {

    @Test
    void setErrorParameterBuildsErrorUiAndReturns500() {
        ErrorView view = new ErrorView();

        RuntimeException exception = new RuntimeException("boom");
        @SuppressWarnings("unchecked")
        ErrorParameter<Exception> parameter = mock(ErrorParameter.class);
        when(parameter.getException()).thenReturn(exception);

        int status = view.setErrorParameter(mock(BeforeEnterEvent.class), parameter);

        assertEquals(500, status);
        assertTrue(view.getChildren().count() >= 4);
    }
}
