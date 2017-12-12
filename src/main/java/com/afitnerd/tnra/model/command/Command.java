package com.afitnerd.tnra.model.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Command {

    private Action action;
    private Section section;
    private SubSection subSection;
    private String param;
    private Map<Stat, Integer> stats;

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public SubSection getSubSection() {
        return subSection;
    }

    public void setSubSection(SubSection subSection) {
        this.subSection = subSection;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public Map<Stat, Integer> getStats() {
        return stats;
    }

    public void addStat(Stat stat, Integer num) {
        if (stats == null) {
            stats = new HashMap<>();
        }
        stats.put(stat, num);
    }

    public void setStats(Map<Stat, Integer> stats) {
        this.stats = stats;
    }
}
