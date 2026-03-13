package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.Page.ExtendedClientDetailsReceiver;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MyServiceInitListenerTest {

    @Test
    void storesExtendedClientDetailsOnUiInit() {
        MyServiceInitListener listener = new MyServiceInitListener();

        ServiceInitEvent event = mock(ServiceInitEvent.class);
        VaadinService service = mock(VaadinService.class);
        when(event.getSource()).thenReturn(service);

        ArgumentCaptor<UIInitListener> uiInitCaptor = ArgumentCaptor.forClass(UIInitListener.class);
        listener.serviceInit(event);
        verify(service).addUIInitListener(uiInitCaptor.capture());

        UI ui = mock(UI.class);
        Page page = mock(Page.class);
        VaadinSession session = mock(VaadinSession.class);
        ExtendedClientDetails details = mock(ExtendedClientDetails.class);

        when(ui.getPage()).thenReturn(page);
        when(ui.getSession()).thenReturn(session);

        doAnswer(inv -> {
            ExtendedClientDetailsReceiver callback = inv.getArgument(0);
            callback.receiveDetails(details);
            return null;
        }).when(page).retrieveExtendedClientDetails(org.mockito.ArgumentMatchers.any());

        UIInitEvent uiInitEvent = mock(UIInitEvent.class);
        when(uiInitEvent.getUI()).thenReturn(ui);

        uiInitCaptor.getValue().uiInit(uiInitEvent);

        verify(session).setAttribute(ExtendedClientDetails.class, details);
    }
}
