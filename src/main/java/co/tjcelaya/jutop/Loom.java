package co.tjcelaya.jutop;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.out;

/**
 * Created by tomascelaya on 5/28/17.
 */
public class Loom implements Serializable {

    public static final String REGEX_THREAD_NAME = "^(\\.?[A-z0-9])+$";
    private final LinkedHashMap<String, String> threads;
    private final Deque<String> attention;
    private final String name;

    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    Loom(final String name) {
        this.name = name;
        this.threads = new LinkedHashMap<>();
        this.attention = new ArrayDeque<>();
    }

    @JsonCreator
    Loom(@JsonProperty("name") final String name,
         @JsonProperty("threads") final LinkedHashMap<String, String> threads,
         @JsonProperty("attention") final Deque<String> attention)
    {
        this.name = name;
        this.threads = threads;
        this.attention = attention;
    }

    void spawn(final String thread) {
        if (!thread.matches(REGEX_THREAD_NAME)) {
            throw new IllegalArgumentException("invalid thread name: " + thread);
        }
        threads.put(thread, "");
    }

    void interrupt(final String thread) {
        if (!attention.isEmpty() && attention.peek() == null) {
            attention.pop(); // refocus
        }

        if (threads.containsKey(thread)) {
            // bring to front
            if (attention.contains(thread)) {
                attention.removeLastOccurrence(thread);
            }
            attention.push(thread);
            return;
        }

        if (!thread.matches(REGEX_THREAD_NAME)) {
            throw new IllegalArgumentException("invalid thread name: " + thread);
        }

        threads.put(thread, "");
        attention.push(thread);
    }

    void exit(final String thread) {
        final String t = tumble(thread);

        if (!threads.containsKey(t)) {
            throw new NoSuchElementException("thread not found: " + t);
        }

        threads.put(t, threads.get(t) + "E");

        if (attention.contains(t)) {
            attention.removeLastOccurrence(t);
        }
    }

    void kill(final String thread) {
        final String t = tumble(thread);

        if (!threads.containsKey(t)) {
            throw new NoSuchElementException("thread not found: " + t);
        }

        threads.put(t, threads.get(t) + "K");

        if (isRunningThread(t)) {
            attention.pop();
        }
    }

    void stop() {
        final String focus = attention.peek();
        if (focus != null) {
            attention.push(null);
        }
    }

    void resume() {
        final String focus = attention.peek();
        if (focus != null) {
            throw new IllegalStateException("not stopped");
        }

        attention.pop();
    }

    // Thread reads

    /**
     * Fall into a target thread.
     * @param target intended target
     * @return the target if given, last thread otherwise
     */
    private String tumble(final String target) {
        if (!StringUtils.isEmpty(target)) {
            return target;
        }

        if (attention.peek() == null) {
            attention.pop();
        }

        if (!attention.isEmpty() && attention.peek() != null) {
            return attention.pop();
        }

        throw new NoSuchElementException("fell flat");
    }

    String status() {
        if (attention.isEmpty()) {
            return "";
        } else {
            return attention.stream()
                    .map(at -> ObjectUtils.firstNonNull(at, "PAUSED"))
                    .collect(Collectors.joining(" < "));
        }
    }

    boolean isRunning() {
        return !attention.isEmpty() && attention.peek() != null;
    }

    boolean isRunningThread(final String thread) {
        return isRunning() && attention.peek().equals(thread);
    }

    // Utility

    String print() {
        final StringBuilder sb = new StringBuilder("status: ").append(status()).append('\n');

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

    String lookup(final int tid) {
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

    void save(final File file) throws IOException {
        MAPPER.writeValue(file, this);
        out.println("wrote self to " + file);
    }

    static Loom load(final File file) throws IOException, ClassNotFoundException {
        if (file.length() == 0L) {
            return new Loom(file.getName());
        }
        return MAPPER.readValue(file, Loom.class);
    }
}
