package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

class PaddleInfoLinkTest {

    /** Held in a field: Vaadin stores the current UI in a WeakReference, so a setUp() local can be
     *  garbage-collected before the test body runs ("No currently active UI found" on CI). */
    private UI ui;

    @BeforeEach
    void setUp() {
        ui = new UI();
        VaadinSession session = mock(VaadinSession.class, Mockito.RETURNS_DEEP_STUBS);
        lenient().when(session.hasLock()).thenReturn(true);
        VaadinService service = mock(VaadinService.class);
        lenient().when(session.getService()).thenReturn(service);
        ui.getInternals().setSession(session);
        UI.setCurrent(ui);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    private static Stream<Component> all(Component root) {
        return Stream.concat(Stream.of(root), root.getChildren().flatMap(PaddleInfoLinkTest::all));
    }

    @Test
    void rendersALinkAndNoDialogUntilClicked() {
        PaddleInfoLink info = new PaddleInfoLink();

        assertEquals(PaddleInfoLink.LINK_TEXT, info.link.getText());
        assertNull(info.dialog, "dialog is built lazily");
    }

    @Test
    void dialogExplainsPaddle_andThatEmailsComeFromPaddle() {
        PaddleInfoLink info = new PaddleInfoLink();

        info.openDialog();

        Dialog d = info.dialog;
        assertTrue(d.isOpened());
        assertEquals(PaddleInfoLink.TITLE, d.getHeaderTitle());
        List<String> paragraphs = all(d).filter(c -> c instanceof Paragraph).map(c -> ((Paragraph) c).getText()).toList();
        assertTrue(paragraphs.stream().anyMatch(t -> t.contains("Merchant of Record")), paragraphs.toString());
        assertTrue(paragraphs.stream().anyMatch(t -> t.contains("never sees or stores your card")), paragraphs.toString());
        assertTrue(paragraphs.stream().anyMatch(t -> t.contains("come from Paddle rather than TNRA")), paragraphs.toString());
        List<String> bullets = all(d).filter(c -> c instanceof ListItem).map(c -> ((ListItem) c).getText()).toList();
        assertEquals(3, bullets.size());
        assertTrue(bullets.stream().anyMatch(t -> t.contains("receipt")));
        assertTrue(bullets.stream().anyMatch(t -> t.contains("cancel")));
        assertTrue(bullets.stream().anyMatch(t -> t.contains("payment fails")));
        Anchor site = (Anchor) all(d).filter(c -> c instanceof Anchor).findFirst().orElseThrow();
        assertEquals(PaddleInfoLink.PADDLE_URL, site.getHref());
        assertEquals("_blank", site.getTarget().orElse(null));
    }

    @Test
    void reopening_reusesTheSameDialog() {
        PaddleInfoLink info = new PaddleInfoLink();
        info.openDialog();
        Dialog first = info.dialog;
        first.close();

        info.openDialog();

        assertSame(first, info.dialog);
        assertTrue(info.dialog.isOpened());
    }
}
