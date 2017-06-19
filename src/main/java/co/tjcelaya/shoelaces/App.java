package co.tjcelaya.shoelaces;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;

import static java.lang.Integer.parseInt;
import static java.lang.System.*;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

/**
 * Hello world!
 */
public class App {

    private static ShoeLaces open(final File dbFile) throws IOException, ClassNotFoundException {
        out.println("using db file: " + dbFile);

        if (dbFile.exists()) {
            return ShoeLaces.load(dbFile);
        }

        File p = dbFile.getParentFile();

        err.println(p.getAbsolutePath());

        if (!dbFile.createNewFile()) {
            err.println("concurrent access?");
            System.exit(1);
        }
        out.println("created");

        return new ShoeLaces(dbFile.getName());
    }

    private static String findThreadFromOption(ShoeLaces db, CommandLine invocation, final String opt) {
        final String raw = StringUtils.trimToEmpty(invocation.getOptionValue(opt));
        if (!NumberUtils.isDigits(raw)) {
            return raw;
        }

        try {
            return db.lookup(parseInt(raw));
        } catch (NoSuchElementException e) {
            err.println(e.getMessage());
            out.println(db.print());
            exit(1);
            return null;
        }
    }

    public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException {
        final Path filePath = Paths.get(
                firstNonNull(
                        getenv("SHOELACES_HOME")
                                .replaceFirst("^~", System.getProperty("user.home")),
                        getProperty("user.dir")));
        final File file = filePath.resolve(
                firstNonNull(
                        getenv("SHOELACES_FILE"),
                        DateTimeFormatter.ISO_DATE.format(LocalDate.now()))
                + ".sldb").toFile();

        final ShoeLaces db = open(file);
        final Options opts = new Options()
                .addOption("h", "help")

                .addOption("s", "spawn", true, "spawn a new thread")
                .addOption("k", "kill", false, "kill a thread")

                .addOption("i", "interrupt", true, "run a new PRIMARY thread")
                .addOption("ret", "return", true, "exit the PRIMARY thread and return to <arg>, if given")

                .addOption("p", "pause", false, "pause (background) the PRIMARY thread")
                .addOption("r", "resume", false, "resume (foreground) the PRIMARY thread");

        final CommandLine invocation;
        try {
            invocation = new DefaultParser().parse(opts, args);
        } catch (MissingArgumentException e) {
            err.println(e.getMessage());
            exit(1);
            return;
        }

        if (invocation.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "sl [-h] [-s|-k|-i|-ret [THREAD]] [-p|-r]", opts);
            exit(0);
            return;
        }

        // add
        else if (invocation.hasOption("s")) {
            final String t = findThreadFromOption(db, invocation, "s");

            if (db.isRunning()) {
                out.println("spawning background: " + t);
                db.spawn(t);
            } else {
                out.println("spawning and switching to: " + t);
                db.interrupt(t);
            }
        }

        // kill
        else if (invocation.hasOption("k")) {
            final String t = findThreadFromOption(db, invocation, "k");

            if (!StringUtils.isEmpty(t)) {
                out.println("killing: " + t);
                db.kill(t);
            } else {
                out.println("killing running: " + db.current());
                db.kill();
            }
        }

        // interrupt
        else if (invocation.hasOption("i")) {
            final String t = findThreadFromOption(db, invocation, "i");
            out.println("interrupted by: " + t);
            db.interrupt(t);
        }

        // exit
        else if (invocation.hasOption("ret")) {
            final String returning = findThreadFromOption(db, invocation, "ret");
            if (returning != null && !returning.equals("")) {
                out.println("returning to: " + returning);
                db.exit(returning);
            } else {
                db.resume();
                out.println("returning to current: " + db.current());
            }
        }

        // background
        else if (invocation.hasOption("p")) {
            if (db.isRunning()) {
                out.println("pause");
                db.pause();
            } else {
                err.println("not running");
            }
        }

        // foreground
        else if (invocation.hasOption("r")) {
            if (db.isPaused()) {
                out.println("resume");
                db.resume();
            } else {
                err.println("not paused");
            }
        } else {
            err.println("no args");
        }

        db.save(file);
        out.println(db.print());
    }
}
