package com.afitnerd.tnra;

import com.afitnerd.tnra.model.command.Action;
import com.afitnerd.tnra.model.command.Command;
import com.afitnerd.tnra.model.command.Section;
import com.afitnerd.tnra.model.command.Stat;
import com.afitnerd.tnra.model.command.SubSection;
import com.afitnerd.tnra.util.CommandParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class CommandParserTests {

    @Test
    public void testCommandParser_fail_null() {
        try {
            CommandParser.parse(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("command string cannot be null", e.getMessage());
        }
    }

    @Test
    public void testCommandParser_fail_empty() {
        try {
            CommandParser.parse("");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("command string cannot be empty", e.getMessage());
        }
    }

    @Test
    public void testCommandParser_fail_bad_action() {
        try {
            CommandParser.parse("badcommand");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("No Action named: badcommand", e.getMessage());
        }
    }

    @Test
    public void testCommandParser_fail_too_short() {
        try {
            CommandParser.parse("no");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Bad action: no. Must be at least 3 characters", e.getMessage());
        }
    }

    @Test
    public void testCommandParser_success_start() {
        Command command = CommandParser.parse("start");
        assertEquals(Action.START, command.getAction());
        assertNull(command.getSection());
        assertNull(command.getSubSection());
        assertNull(command.getParam());
    }

    @Test
    public void testCommandParser_success_finish() {
        Command command = CommandParser.parse("finish");
        assertEquals(Action.FINISH, command.getAction());
        assertNull(command.getSection());
        assertNull(command.getSubSection());
        assertNull(command.getParam());
    }

    @Test
    public void testCommandParser_success_show() {
        Command command = CommandParser.parse("show");
        assertEquals(Action.SHOW, command.getAction());
        assertNull(command.getSection());
        assertNull(command.getSubSection());
        assertNull(command.getParam());
    }

    @Test
    public void testCommandParser_success_show_with_param() {
        Command command = CommandParser.parse("show micah@afitnerd.com");
        assertEquals(Action.SHOW, command.getAction());
        assertEquals("micah@afitnerd.com", command.getParam());
        assertNull(command.getSection());
        assertNull(command.getSubSection());
    }

    @Test
    public void testCommandParser_success_help() {
        Command command = CommandParser.parse("help");
        assertEquals(Action.HELP, command.getAction());
        assertNull(command.getSection());
        assertNull(command.getSubSection());
        assertNull(command.getParam());
    }

    @Test
    public void testCommandParser_fail_upd_empty_section() {
        try {
            CommandParser.parse("update");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("" +
                "UPDATE Action must have one of [INTRO, PERSONAL, FAMILY, WORK, STATS] as its Section argument",
                e.getMessage()
            );
        }
    }

    @Test
    public void testCommandParser_fail_upd_bad_section() {
        try {
            CommandParser.parse("update yourmama");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(
                "No Section named: yourmama",
                e.getMessage()
            );
        }
    }

    @Test
    public void testCommandParser_fail_upd_empty_subsection() {
        try {
            CommandParser.parse("update intro");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("INTRO Section must have one of [WIDWYTK, KRYPTONITE, WHATANDWHEN] as its Subsection argument", e.getMessage());
        }

    }

    @Test
    public void testCommandParser_fail_upd_bad_subsection() {
        try {
            CommandParser.parse("update intro yourmama");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("No SubSection named: yourmama", e.getMessage());
        }
    }

    @Test
    public void testCommandParser_fail_upd_int_empty_param() {
        try {
            CommandParser.parse("update intro widwytk");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("WIDWYTK SubSection must have a parameter", e.getMessage());
        }
    }

    @Test
    public void testCommandParser_success_upd_int_wid() {
        Command command = CommandParser.parse("update intro widwytk that's too much, man!");
        assertEquals(Action.UPDATE, command.getAction());
        assertEquals(Section.INTRO, command.getSection());
        assertEquals(SubSection.WIDWYTK, command.getSubSection());
        assertEquals("that's too much, man!", command.getParam());
    }

    @Test
    public void testCommandParser_fail_upd_sta_empty() {
        try {
            CommandParser.parse("update stats");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(
                "STATS Section must have one or more of " +
                "[EXERCISE, GTG, MEDITATE, MEETINGS, PRAY, READ, SPONSOR] stats in <stat>:<num> form",
                e.getMessage()
            );
        }
    }

    @Test
    public void testCommandParser_success_upd_sta() {
        Command command = CommandParser.parse("update stats exercise:7");
        assertEquals(Action.UPDATE, command.getAction());
        assertEquals(Section.STATS, command.getSection());
        assertNull(command.getSubSection());
        assertEquals(1, command.getStats().size());
        assertEquals(7, (int) command.getStats().get(Stat.EXERCISE));
    }

    @Test
    public void testCommandParser_success_upd_sta_multiple_with_whitespace() {
        Command command = CommandParser.parse("update stats exercise: 7 gtg:5 med: 4");
        assertEquals(Action.UPDATE, command.getAction());
        assertEquals(Section.STATS, command.getSection());
        assertNull(command.getSubSection());
        assertEquals(3, command.getStats().size());
        assertEquals(7, (int) command.getStats().get(Stat.EXERCISE));
        assertEquals(5, (int) command.getStats().get(Stat.GTG));
        assertEquals(4, (int) command.getStats().get(Stat.MEDITATE));
    }

    @Test
    public void testCommandParser_fail_upd_sta_bad_val() {
        try {
            CommandParser.parse("upd stat exe: g");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Bad stats String: exe:g", e.getMessage());
        }
    }

    @Test
    public void testCommandParser_fail_upd_sta_bad_sta() {
        try {
            CommandParser.parse("upd stat blarg: 1");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("No Stat named: blarg", e.getMessage());
        }
    }

    @Test
    public void testCommandParser_success_upd_sta_no_value() {
        Command command = CommandParser.parse("upd stat exe");
        assertEquals(Action.UPDATE, command.getAction());
        assertEquals(Section.STATS, command.getSection());
        assertNull(command.getSubSection());
        assertNull(command.getStats().get(Stat.EXERCISE));
    }

    @Test
    public void testCommandParser_success_upd_sta_colon_no_value() {
        Command command = CommandParser.parse("upd stat exe:");
        assertEquals(Action.UPDATE, command.getAction());
        assertEquals(Section.STATS, command.getSection());
        assertNull(command.getSubSection());
        assertNull(command.getStats().get(Stat.EXERCISE));
    }
}
