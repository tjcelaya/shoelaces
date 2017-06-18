package co.tjcelaya.jutop;

import static java.lang.System.out;
import static java.lang.System.err;
import static org.testng.Assert.*;

import com.sun.istack.internal.NotNull;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.UUID;

/**
 * Created by tj on 6/18/17.
 */
@Test
public class LoomTest {

    private static String randomThreadName() {
        return UUID.randomUUID().toString();
    }

    private void assertCurrentEquals(final Loom loom, final String name) {
        Assert.assertEquals(loom.current(), name);
    }

    public void testLoomsHaveNamesAndValidate() throws Exception {
        final String n = randomThreadName();
        final Loom l = new Loom(n);
        Assert.assertEquals(n, l.getName());

        Assert.assertThrows(() -> new Loom("3 !@"));
    }

    public void testLoomThreadNamesAreValidatedToo() throws Exception {
        final Loom l = new Loom(randomThreadName());
        Assert.assertThrows(() -> l.interrupt(""));
        Assert.assertThrows(() -> l.interrupt("s-!@#$%^&*()"));
        Assert.assertThrows(() -> l.interrupt("s-!@#"));
    }

    public void testSpawnFocusesWhenEmpty() throws Exception {
        final Loom l = new Loom();
        final String name = randomThreadName();

        l.spawn(name);
        final String current = l.current();
        assertCurrentEquals(l, name);
    }

    public void testInterruptUpdatesFocus() throws Exception {
        final Loom l = new Loom();
        l.interrupt(randomThreadName());

        final String second = randomThreadName();
        l.interrupt(second);
        assertCurrentEquals(l, second);
    }

    public void testInterruptRefocuses() throws Exception {
        final Loom l = new Loom();
        assertCurrentEquals(l, "");
        final String first = randomThreadName();
        l.interrupt(first);
        assertCurrentEquals(l, first);
        l.stop();
        assertTrue(l.isSuspended());
    }

    public void testInterruptBringsToFront() throws Exception {
        final Loom l = new Loom();
        final String first = randomThreadName();
        final String third = randomThreadName();

        l.interrupt(first);
        l.interrupt(randomThreadName());
        l.interrupt(third);
        assertCurrentEquals(l, third);
        l.interrupt(first);
        assertCurrentEquals(l, first);
    }

    public void testImplicitExit() throws Exception {
        final Loom l = new Loom();
        final String first = randomThreadName();
        final String second = randomThreadName();

        l.interrupt(first);
        assertCurrentEquals(l, first);
        l.exit();
        assertCurrentEquals(l, "");
    }

    public void testExplicitExit() throws Exception {
        final Loom l = new Loom();
        final String first = randomThreadName();
        final String second = randomThreadName();

        l.spawn(first);
        assertCurrentEquals(l, first);
        l.exit(first);
        assertCurrentEquals(l, "");
    }

    public void testKillNonExistentThrows() throws Exception {
        Assert.assertThrows(() -> {
            new Loom().kill(randomThreadName());
        });
    }

    public void testStop() throws Exception {
        final Loom l = new Loom();
        final String n = randomThreadName();
        l.interrupt(n);
        l.kill(n);
        assertCurrentEquals(l, "");
    }

    public void testResume() throws Exception {
    }

    public void testStatus() throws Exception {
    }

    public void testIsRunning() throws Exception {
        final Loom l = new Loom();
        assertFalse(l.isRunning());

        final String first = randomThreadName();
        final String second = randomThreadName();

        l.interrupt(first);
        assertTrue(l.isRunning());

        l.stop();
        assertFalse(l.isRunning());

        l.interrupt(second);
        assertTrue(l.isRunning());

        l.interrupt(first);
        assertTrue(l.isRunning());

        l.exit();
        assertTrue(l.isRunning());

        assertCurrentEquals(l, second);

        l.exit();

        assertFalse(l.isRunning());
    }

    public void testIsRunningThread() throws Exception {
    }

    public void testPrintIncludesNameAndThreads() throws Exception {
        final String n = randomThreadName();
        final Loom l = new Loom(n);

        Assert.assertTrue(l.print().contains(n));
        final String first = randomThreadName();
        l.interrupt(first);
        Assert.assertTrue(l.print().contains(first));
    }

    public void testLookup() throws Exception {
    }

    public void testSave() throws Exception {
    }

    public void testLoad() throws Exception {
    }

}