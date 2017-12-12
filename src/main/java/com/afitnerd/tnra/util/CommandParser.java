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

    public static Command parse(String args) {
        Command command = new Command();
        Assert.notNull(args, "command string cannot be null");
        String[] parts = cleanSplit(args.split(" "));
        Assert.notEmpty(parts, "command string cannot be empty");

        Action action = Action.fromValue(parts[0]);

        command.setAction(action);

        // start and finish have no section, subsection, or value
        if (Action.standaloneActions().contains(action)) {
            // some commands can have a param like: show <email address>
            // but, only ever one param
            if (parts.length > 1 && parts[1] != null) {
                command.setParam(parts[1]);
            }
            return command;
        }

        // now we know we need a section
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

        if (Section.STATS.equals(section)) {
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

            return command;
        }

        // now we know need a subsection
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

        // no we know we need a param
        Assert.isTrue(
            parts.length > 3 && parts[3] != null,
            subSection + " SubSection must have a parameter"
        );
        command.setParam(String.join(" ", Arrays.copyOfRange(parts, 3, parts.length)));

        return command;
    }

    private static void dealWithAction(Command command) {

    }

    private static String[] cleanSplit(String[] parts) {
        List<String> partsList = new ArrayList<>(Arrays.asList(parts));
        partsList.removeAll(Arrays.asList("", null));
        return partsList.toArray(new String[0]);
    }
}
