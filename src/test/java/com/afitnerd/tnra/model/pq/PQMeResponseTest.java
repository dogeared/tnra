package com.afitnerd.tnra.model.pq;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PQMeResponseTest {

    @Test
    void pqResponseExposesNestedMetrics() {
        PQMeResponse response = new PQMeResponse();
        PQMeResponse.Pq pq = new PQMeResponse.Pq();
        pq.setCharge(BigDecimal.valueOf(12.5));
        pq.setMuscle(BigDecimal.valueOf(7.2));
        pq.setRepsToday(42);
        pq.setUpdatedAt(123456789L);

        response.setPq(pq);

        assertSame(pq, response.getPq());
        assertEquals(BigDecimal.valueOf(12.5), pq.getCharge());
        assertEquals(BigDecimal.valueOf(7.2), pq.getMuscle());
        assertEquals(42, pq.getRepsToday());
        assertEquals(123456789L, pq.getUpdatedAt());
    }
}
