package co.tjcelaya.shoelaces;

import static org.testng.Assert.*;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Created by tj on 6/18/17.
 */
@Test
public class ShoeLacesTest {

    private static String randomThreadName() {
        return UUID.randomUUID().toString();
    }

    private void assertCurrentEquals(final ShoeLaces loom, final String name) {
        Assert.assertEquals(loom.current(), name);
    }

    public void testLoomsHaveNamesAndValidate() throws Exception {
        final String n = randomThreadName();
        final ShoeLaces l = new ShoeLaces(n);
        Assert.assertEquals(n, l.getName());

        Assert.assertThrows(() -> new ShoeLaces("3 !@"));
    }

    public void testLoomThreadNamesAreValidatedToo() throws Exception {
        final ShoeLaces l = new ShoeLaces(randomThreadName());
        Assert.assertThrows(() -> l.interrupt(""));
        Assert.assertThrows(() -> l.interrupt("s-!@#$%^&*()"));
        Assert.assertThrows(() -> l.interrupt("s-!@#"));
    }

    public void testSpawnFocusesWhenEmpty() throws Exception {
        final ShoeLaces l = new ShoeLaces();
        final String name = randomThreadName();

        l.spawn(name);
        final String current = l.current();
        assertCurrentEquals(l, name);

        l.spawn(randomThreadName());
        assertCurrentEquals(l, name);
    }

    public void testSpawnInvalidName() throws Exception {
        final ShoeLaces l = new ShoeLaces();

        Assert.assertThrows(() -> l.spawn("!@#$%^&*()"));
    }

    public void testInterruptUpdatesFocus() throws Exception {
        final ShoeLaces l = new ShoeLaces();
        l.interrupt(randomThreadName());

        final String second = randomThreadName();
        l.interrupt(second);
        assertCurrentEquals(l, second);
    }

    public void testInterruptRefocuses() throws Exception {
        final ShoeLaces l = new ShoeLaces();
        assertNull(l.current());
        final String first = randomThreadName();
        l.interrupt(first);
        assertCurrentEquals(l, first);
        l.stop();
        assertTrue(l.isSuspended());
    }

    public void testInterruptBringsToFront() throws Exception {
        final ShoeLaces l = new ShoeLaces();
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
        final ShoeLaces l = new ShoeLaces();
        final String first = randomThreadName();
        final String second = randomThreadName();

        l.interrupt(first);
        assertCurrentEquals(l, first);
        l.exit();
        assertNull(l.current());
    }

    public void testExplicitExit() throws Exception {
        final ShoeLaces l = new ShoeLaces();
        final String first = randomThreadName();
        final String second = randomThreadName();

        l.spawn(first);
        assertCurrentEquals(l, first);
        l.exit(first);
        assertNull(l.current());
    }

    public void testNonExistentThrows() throws Exception {
        final ShoeLaces l = new ShoeLaces();
        Assert.assertThrows(() -> l.kill(randomThreadName()));

        Assert.assertThrows(() -> l.exit(randomThreadName()));
    }

    public void testStop() throws Exception {
        final ShoeLaces l = new ShoeLaces();
        final String n = randomThreadName();
        l.interrupt(n);
        l.stop();
        assertNull(l.current());
    }

    public void testKill() throws Exception {
        final ShoeLaces l = new ShoeLaces();
        final String n = randomThreadName();

        assertThrows(() -> l.kill());

        l.interrupt(n);
        l.kill(n);
        assertFalse(l.isRunning());

        assertThrows(() -> l.kill(""));
        assertThrows(() -> l.kill("nonexist"));
    }

    public void testKillWillUndoSuspend() throws Exception {
        final ShoeLaces l = new ShoeLaces();
        final String n = randomThreadName();
        l.interrupt(n);
        assertTrue(l.isRunning());

        l.stop();
        assertFalse(l.isRunning());

        l.kill();
    }

    public void testExitWillUndoSuspend() throws Exception {
        final ShoeLaces l = new ShoeLaces();
        final String first = randomThreadName();
        final String second = randomThreadName();
        l.interrupt(first);
        l.interrupt(second);
        l.stop();
        assertEquals(l.current(), null);

        l.exit();
        assertEquals(l.current(), first);
    }

    public void testResume() throws Exception {
        final ShoeLaces l = new ShoeLaces();

        Assert.assertThrows(l::resume);

        l.interrupt(randomThreadName());
        l.resume(); // noop

        l.stop();
        assertNull(l.current());
        l.resume(); // noop
    }

    public void testIsRunning() throws Exception {
        final ShoeLaces l = new ShoeLaces();
        assertFalse(l.isRunning());

        final String first = randomThreadName();
        final String second = randomThreadName();

        Assert.assertThrows(l::exit);

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

    public void testPrintIncludesNameAndThreads() throws Exception {
        final String n = randomThreadName();
        final ShoeLaces l = new ShoeLaces(n);

        Assert.assertTrue(l.print().contains(n));
        final String first = randomThreadName();
        l.interrupt(first);
        Assert.assertTrue(l.print().contains(first));

        l.kill(first);
        l.kill(first);

        Assert.assertTrue(l.print().contains("KK"));
    }

    public void testLookup() throws Exception {
        final ShoeLaces l = new ShoeLaces();

        final String first = randomThreadName();
        final String second = randomThreadName();
        l.interrupt(first);

        assertEquals(first, l.lookup(0));

        Assert.assertThrows(() -> l.lookup(1));

        l.interrupt(second);

        assertEquals(second, l.lookup(1));
        assertEquals(first, l.lookup(0));

        Assert.assertThrows(() -> l.lookup(-1));
        Assert.assertThrows(() -> l.lookup(5));
    }

    public void testLoadEmptyFile() throws Exception {
        final String filename = randomThreadName();
        final File f = new File(System.getProperty("java.io.tmpdir") + "/" + filename + ".sldb");
        f.deleteOnExit();

        final ShoeLaces emptyLoom = ShoeLaces.load(f);
        assertFalse(emptyLoom.isRunning());

        assertEquals(filename, emptyLoom.getName());
    }

    public void testSaveAndLoadWithData() throws Exception {
        final String name = randomThreadName();
        final ShoeLaces l = new ShoeLaces(name);
        final File f = File.createTempFile(randomThreadName(), ".sldb");
        f.deleteOnExit();

        l.save(f);

        final String serialized = FileUtils.readFileToString(f, StandardCharsets.UTF_8);

        assertTrue(0 < serialized.length(), "serialized version empty?");
        assertTrue(serialized.contains(name));
        assertTrue(serialized.contains("name"));
        assertTrue(serialized.contains("attention"));
        assertTrue(serialized.contains("threads"));

        final ShoeLaces loaded = ShoeLaces.load(f);

        assertEquals(l.getName(), loaded.getName());
        assertEquals(l.current(), loaded.current());
        assertEquals(l.isRunning(), loaded.isRunning());
    }
}