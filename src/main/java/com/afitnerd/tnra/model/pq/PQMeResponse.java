package com.afitnerd.tnra.model.pq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PQMeResponse {

    private Pq pq;

    public Pq getPq() {
        return pq;
    }

    public void setPq(Pq pq) {
        this.pq = pq;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pq {

        private BigDecimal charge;
        private BigDecimal muscle;

        @JsonProperty("reps_day")
        private Integer repsToday;

        public BigDecimal getCharge() {
            return charge;
        }

        public void setCharge(BigDecimal charge) {
            this.charge = charge;
        }

        public BigDecimal getMuscle() {
            return muscle;
        }

        public void setMuscle(BigDecimal muscle) {
            this.muscle = muscle;
        }

        public Integer getRepsToday() {
            return repsToday;
        }

        public void setRepsToday(Integer repsToday) {
            this.repsToday = repsToday;
        }
    }
}
