package co.tjcelaya.shoelaces;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by tomascelaya on 5/28/17.
 */
public class ShoeLaces implements Serializable {

    public static final Pattern REGEX_THREAD_NAME =
            Pattern.compile("^([0-9a-z-]+\\.)*[0-9a-z-]+$");

    private final LinkedHashMap<String, String> threads;
    private final Deque<String> attention;
    private final String name;

    private static final ObjectMapper MAPPER;
    private static final String NULLFOCUS = "";

    public static final String INTERRUPT = "I";
    public static final String EXIT = "E";
    public static final String KILL = "K";

    static {
        MAPPER = new ObjectMapper();
        MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    /**
     * The laces are not concerned with "faster" UUID generation, you can worry about that.
     */
    ShoeLaces() {
        this(UUID.randomUUID().toString());
    }

    ShoeLaces(final String name) {
        this(name, new LinkedHashMap<>(), new ArrayDeque<>());
    }

    @JsonCreator
    ShoeLaces(@JsonProperty("name") final String name,
              @JsonProperty("threads") final LinkedHashMap<String, String> threads,
              @JsonProperty("attention") final Deque<String> attention) {

        if (!REGEX_THREAD_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(name);
        }

        this.name = name;
        this.threads = threads;
        this.attention = attention;
    }

    public void spawn(final String thread) {
        if (!REGEX_THREAD_NAME.matcher(thread).matches()) {
            throw new IllegalArgumentException("invalid thread name: " + thread);
        }

        threads.put(thread, "");

        if (attention.isEmpty()) {
            attention.push(thread);
        }
    }

    public void interrupt(final String thread) {
        final String focus = attention.peek();
        if (focus != null && focus.equals(NULLFOCUS)) {
            attention.pop(); // refocus
        }

        if (attention.contains(thread)) {
            updateThread(thread, INTERRUPT);
            attention.removeFirstOccurrence(thread);
            attention.push(thread);
            return;
        }

        if (!REGEX_THREAD_NAME.matcher(thread).matches()) {
            throw new IllegalArgumentException("invalid thread name: " + thread);
        }

        threads.put(thread, "");
        attention.push(thread);
    }

    public void exit() {
        if (!isSuspended() && !isRunning()) {
            throw new IllegalStateException("Nothing from which to exit");
        }

        exit(attention.pop());
    }

    public void exit(final String thread) {
        final String t = tumble(thread);

        if (!threads.containsKey(t)) {
            throw new NoSuchElementException("thread not found: " + t);
        }

        updateThread(t, EXIT);

        if (attention.contains(t)) {
            attention.removeLastOccurrence(t);
        }
    }

    private void updateThread(final String thread, final String status) {
        threads.put(thread, threads.get(thread) + status);
    }

    public void kill() {
        if (!isSuspended() && !isRunning()) {
            throw new IllegalStateException("Nothing running to kill");
        }

        exit(attention.pop());
    }

    public void kill(final String thread) {
        final String t = tumble(thread);

        if (!threads.containsKey(t)) {
            throw new NoSuchElementException("thread not found: " + t);
        }

        updateThread(t, KILL);
        attention.removeFirstOccurrence(t);
    }

    public void stop() {
        if (!attention.peek().equals(NULLFOCUS)) {
            attention.push(NULLFOCUS);
        }
    }

    public void resume() {
        final String focus = attention.peek();
        if (focus == null) {
            throw new IllegalStateException("Nothing to resume");
        }

        if (focus.equals(NULLFOCUS)) {
            attention.pop();
        }

        // noop if not suspended
    }

    /**
     * Fall into a target thread.
     *
     * @param target intended target
     * @return the target if given, last thread otherwise
     */
    private String tumble(final String target) {
        if (!StringUtils.isEmpty(target)) {
            return target;
        }

        final String focus = attention.peek();

        if (focus != null && !focus.equals(NULLFOCUS)) {
            return attention.pop();
        }

        throw new NoSuchElementException("fell flat");
    }

    // Reads

    public String getName() {
        return name;
    }

    // Thread reads

    public String current() {
        final String focus = attention.peek();

        return focus == null || focus.equals(NULLFOCUS)
                ? null
                : focus;
    }

    private String status() {
        if (attention.isEmpty()) {
            return " - STOP -";
        } else {
            return " RUN: " +
                    attention.stream()
                            .map(at -> ObjectUtils.firstNonNull(at, "PAUSED"))
                            .collect(Collectors.joining(" < "));
        }
    }

    boolean isSuspended() {
        final String focus = attention.peek();
        return focus != null && focus.equals(NULLFOCUS);
    }

    @JsonIgnore
    public boolean isRunning() {
        final String focus = attention.peek();
        return focus != null && !focus.equals(NULLFOCUS);
    }

    // Utility

    public String print() {
        final StringBuilder sb = new StringBuilder("name: ")
                .append(getName())
                .append("\n\nstatus: ")
                .append(status()).append("\n\n");

        if (threads.isEmpty()) {
            return sb.toString();
        }

        final Set<String> threadNames = threads.keySet();
        final int longestNameLength = threadNames.stream().map(String::length).max(Integer::compare).get();
        int tid = 0;

        final int COL_WIDTH_TID = 5;
        final int COL_WIDTH_THREAD = Math.max(longestNameLength, "thread".length()) + 1;

        sb
                .append(StringUtils.rightPad("tid", COL_WIDTH_TID))
                .append(StringUtils.rightPad("thread", COL_WIDTH_THREAD))
                .append("status")
                .append("\n")

                .append(StringUtils.repeat("=", COL_WIDTH_TID - 1)).append(" ")
                .append(StringUtils.repeat("=", COL_WIDTH_THREAD - 1)).append(" ")
                .append(StringUtils.repeat("=", "status".length())).append(" ").append('\n');

        for (String threadName : threadNames) {
            sb.append(StringUtils.rightPad(String.valueOf(tid), COL_WIDTH_TID)); // tid
            sb.append(StringUtils.rightPad(threadName, COL_WIDTH_THREAD)); // thread
            sb.append(
                    Objects.equals(attention.peek(), threadName)
                            ? "RUNNING"
                            : threads.get(threadName)); // status
            sb.append('\n');
            tid++;
        }

        return sb.append('\n').toString();
    }

    public String lookup(final int tid) {
        final Iterator<String> s = threads.keySet().iterator();
        int i = 0;
        String threadName;

        while (s.hasNext()) {
            threadName = s.next();
            if (i == tid) {
                return threadName;
            }
            i++;
        }

        throw new NoSuchElementException("tid not found: " + tid);
    }

    // Persistence

    public void save(final File file) throws IOException {
        MAPPER.writeValue(file, this);
    }

    public static ShoeLaces load(final File file) throws IOException, ClassNotFoundException {
        if (file.length() == 0L) {
            ;
            return new ShoeLaces(FilenameUtils.getBaseName(file.getName()));
        }
        return MAPPER.readValue(file, ShoeLaces.class);
    }
}
