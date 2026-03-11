package com.afitnerd.tnra.service.pq;

import com.afitnerd.tnra.model.pq.PQMeResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PQRendererTest {

    @Test
    void calcChargeNeverDropsBelowZero() {
        long longAgo = System.currentTimeMillis() - (60L * 60L * 1000L);

        assertEquals(0L, PQRenderer.calcCharge(1.0, longAgo));
    }

    @Test
    void padHelpersRespectRequestedDirectionAndMinimumLength() {
        assertEquals("abc", PQRenderer.pad("abc", 2, ""));
        assertEquals("  abc", PQRenderer.padLeft("abc", 5));
        assertEquals("abc  ", PQRenderer.padRight("abc", 5));
    }

    @Test
    void renderRoundsValuesSortsNamesAndMarksWinners() {
        long now = System.currentTimeMillis();
        Map<String, PQMeResponse> metrics = new LinkedHashMap<>();
        metrics.put("Zed", response(9.6, 7.2, 8, now));
        metrics.put("Alice", response(10.2, 6.4, 9, now));
        metrics.put("Nobody", null);

        String rendered = PQRenderer.render(metrics);

        assertTrue(rendered.startsWith("```\n"));
        assertTrue(rendered.endsWith("```"));
        assertTrue(rendered.contains("Name"));
        assertTrue(rendered.indexOf("Alice") < rendered.indexOf("Zed"));
        assertTrue(rendered.contains("*10"));
        assertTrue(rendered.contains("*7"));
        assertTrue(rendered.contains("*9"));
    }

    private static PQMeResponse response(double charge, double muscle, int reps, long updatedAt) {
        PQMeResponse response = new PQMeResponse();
        PQMeResponse.Pq pq = new PQMeResponse.Pq();
        pq.setCharge(BigDecimal.valueOf(charge));
        pq.setMuscle(BigDecimal.valueOf(muscle));
        pq.setRepsToday(reps);
        pq.setUpdatedAt(updatedAt);
        response.setPq(pq);
        return response;
    }
}
