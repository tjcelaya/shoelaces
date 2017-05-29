package co.tjcelaya.jutop;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.io.IOException;
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

    private static Loom open(final File dbFile) throws IOException, ClassNotFoundException {
        out.println("using db file: " + dbFile);

        if (dbFile.exists()) {
            return Loom.load(dbFile);
        }

        if (!dbFile.createNewFile()) {
            err.println("concurrent access?");
            System.exit(1);
        }
        out.println("created");

        return new Loom(dbFile.getName());
    }

    public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException {
        final File file = new File(StringUtils.join(
                firstNonNull(
                        getenv("JUTOP_HOME"),
                        getProperty("user.dir")),
                File.separator,
                firstNonNull(
                        getenv("JUTOP_FILE"),
                        DateTimeFormatter.ISO_DATE.format(LocalDate.now())),
                ".jtdb"));

        final Loom db = open(file);
        final Options opts = new Options()
                .addOption("a", "add", true, "spawn a new thread")
                .addOption("k", "kill", false, "kill a thread")

                .addOption("i", "interrupt", true, "run a new PRIMARY thread")
                .addOption("r", "return", false, "exit the PRIMARY thread and return to <arg>, if given")

                .addOption("h", "help")

                .addOption("b", "background", false, "suspend the PRIMARY thread")
                .addOption("f", "foreground", false, "resume the PRIMARY thread");



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
            formatter.printHelp( "jutop [-akiehbf [THREAD]]", opts);
            exit(0);
            return;
        }

        // add
        else if (invocation.hasOption("a")) {
            final String t = findThreadFromOption(db, invocation, "a");

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
            final boolean self = StringUtils.isEmpty(t);
            out.println("killing: " + (self ? "RUNNING" : t));
            db.kill(t);
        }

        // interrupt
        else if (invocation.hasOption("i")) {
            final String t = findThreadFromOption(db, invocation, "i");
            out.println("interrupted by: " + t);
            db.interrupt(t);
        }

        // exit
        else if (invocation.hasOption("r")) {
            final String returning = findThreadFromOption(db, invocation, "r");
            out.println("returning to: " + returning);
            db.exit(returning);
        }

        // background
        else if (invocation.hasOption("b") && db.isRunning()) {
            out.println("background");
            db.stop();
        }

        // foreground
        else if (invocation.hasOption("f") && !db.isRunning()) {
            out.println("foreground");
            db.resume();
        }

        db.save(file);
        out.println(db.print());
    }

    private static String findThreadFromOption(Loom db, CommandLine invocation, final String opt) {
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
}
