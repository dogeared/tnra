package com.afitnerd.tnra.util;

import com.afitnerd.tnra.model.command.Action;
import com.afitnerd.tnra.model.command.Command;
import com.afitnerd.tnra.model.command.Section;
import com.afitnerd.tnra.model.command.Stat;
import com.afitnerd.tnra.model.command.SubSection;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandParser {

    public static Command parse(String argString) {
        Command command = new Command();

        String[] parts = beginParse(argString);
        dealWithAction(command, parts);

        // standalone actions may have a param, but otherwise, we're done
        if (Action.standaloneActions().contains(command.getAction())) {
            dealWithParam(command, parts);
        } else {
            // now we know we need a section
            dealWithSection(command, parts);

            if (Section.STATS.equals(command.getSection())) {
                dealWithStats(command, parts);
            } else {
                // now we know need a subsection
                dealWithSubSection(command, parts);

                // no we know we need a param
                dealWithParam(command, parts);
            }
        }
        return command;
    }

    private static String[] beginParse(String argString) {
        Assert.notNull(argString, "command string cannot be null");
        String[] parts = cleanSplit(argString.split(" "));
        Assert.notEmpty(parts, "command string cannot be empty");
        return parts;
    }

    private static void dealWithAction(Command command, String[] parts) {
        Action action = Action.fromValue(parts[0]);
        command.setAction(action);
    }

    private static void dealWithSection(Command command, String[] parts) {
        Action action = command.getAction();
        Assert.isTrue(
            parts.length > 1 && parts[1] != null,
            action + " Action must have one of " + Action.validSectionsFor(action) + " as its Section argument"
        );
        Section section = Section.fromValue(parts[1]);
        if (!Action.validSectionsFor(action).contains(section)) {
            throw new IllegalArgumentException(
                    action + " Action must have one of the following Sections: " + Action.validSectionsFor(action)
            );
        }
        command.setSection(section);
    }

    private static void dealWithStats(Command command, String[] parts) {
        Section section = command.getSection();
        Assert.isTrue(
            parts.length > 2 && parts[2] != null,
            section + " Section must have one or more of " +
            Stat.allStats() + " stats in <stat>:<num> form"
        );
        // need to compress any whitespace between : and number
        String statsString = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
        statsString = statsString.replaceAll(":\\s", ":");
        String[] statParts = statsString.split(" ");

        try {
            for (String statPart : statParts) {
                String[] statsParts = statPart.split(":");
                command.addStat(Stat.fromValue(statsParts[0]), Integer.parseInt(statsParts[1]));
            }
        } catch (NumberFormatException n) {
            throw new IllegalArgumentException("Bad stats String: " + statsString);
        }
    }

    private static void dealWithSubSection(Command command, String[] parts) {
        Section section = command.getSection();
        Assert.isTrue(
                parts.length > 2 && parts[2] != null,
                section + " Section must have one of " +
                        Section.validSubSectionsFor(section) + " as its Subsection argument"
        );
        SubSection subSection = SubSection.fromValue(parts[2]);
        if (!Section.validSubSectionsFor(section).contains(subSection)) {
            throw new IllegalArgumentException(
                    section + " Section must have one of the following SubSections: " + Section.validSubSectionsFor(section)
            );
        }
        command.setSubSection(subSection);
    }

    private static void dealWithParam(Command command, String[] parts) {

        if (Action.standaloneActions().contains(command.getAction())) {
            // some commands can have a param like: show <email address>
            // but, only ever one param
            if (parts.length > 1 && parts[1] != null) {
                command.setParam(parts[1]);
            }
            return;
        }

        SubSection subSection = command.getSubSection();
        Assert.isTrue(
            parts.length > 3 && parts[3] != null,
            subSection + " SubSection must have a parameter"
        );
        command.setParam(String.join(" ", Arrays.copyOfRange(parts, 3, parts.length)));
    }

    private static String[] cleanSplit(String[] parts) {
        List<String> partsList = new ArrayList<>(Arrays.asList(parts));
        partsList.removeAll(Arrays.asList("", null));
        return partsList.toArray(new String[0]);
    }
}
