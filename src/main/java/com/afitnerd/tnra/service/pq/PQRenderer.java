package com.afitnerd.tnra.service.pq;

import com.afitnerd.tnra.model.pq.PQMeResponse;

import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;

public interface PQRenderer {

    static Long calcCharge(Double charge, Long updatedAt) {
        Long now = new Date().getTime();
        Long adj = (now - updatedAt)/1000/60/4;
        Double finalCharge = (charge - adj) < 0 ? 0 : (charge - adj);
        return Math.round(finalCharge);
    }

    static String pad(String str, Integer length, String padDir) {
        if (length < str.length()) {
            return str;
        }
        return String.format("%1$" + padDir + length + "s", str);

    }

    static String padLeft(String str, Integer length) {
        return pad(str, length, "");
    }

    static String padRight(String str, Integer length) {
        return pad(str, length, "-");
    }

    static String render(Map<String, PQMeResponse> metricsAll) {
        final StringBuffer ret = new StringBuffer();
        ret.append("```\n")
            .append(padRight("Name", 20)).append(padLeft("Charge", 10))
            .append(padLeft("Muscle", 9)).append(padLeft("Reps Today", 12))
            .append("\n");
        ret
            .append(padRight("----", 20)).append(padLeft("------", 10))
            .append(padLeft("------", 9)).append(padLeft("----------", 12))
            .append("\n");
        String chargeWinName = metricsAll.entrySet().stream()
            .max(Comparator.comparing(e -> e.getValue().getPq().getCharge()))
            .orElseThrow(NoSuchElementException::new).getKey();
        String muscleWinName = metricsAll.entrySet().stream()
            .max(Comparator.comparing(e -> e.getValue().getPq().getMuscle()))
            .orElseThrow(NoSuchElementException::new).getKey();
        String repsWinName = metricsAll.entrySet().stream()
            .max(Comparator.comparing(e -> e.getValue().getPq().getRepsToday()))
            .orElseThrow(NoSuchElementException::new).getKey();
        metricsAll.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String name = entry.getKey();
            PQMeResponse.Pq pq = entry.getValue().getPq();
            ret.append(padRight(name, 20))
                .append(padLeft((name.equals(chargeWinName))?"*":"" +
                    calcCharge(pq.getCharge().doubleValue(), pq.getUpdatedAt()), 10))
                .append(padLeft((name.equals(muscleWinName))?"*":"" +
                    Math.round(pq.getMuscle().doubleValue()), 9))
                .append(padLeft((name.equals(repsWinName))?"*":"" +
                    pq.getRepsToday(), 12))
                .append("\n");
        });
        return ret.append("```").toString();
    }
}
