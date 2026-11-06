package com.malevolentgods.fourtwenty.domain;

/** A Weedweeno program: a name and Lua code, small enough to send to the server in one message. */
public final class Programs {
    private Programs() {}

    public static final int MAX_CODE = 30_000, MAX_NAME = 32;

    public record Program(String name, String code) {
        public Program {
            name = Programs.name(name);
            code = code == null ? "" : code;
        }

        public boolean fits() { return Programs.fits(code); }
    }

    /** A tidy name: trimmed, no newlines, at most 32 characters, "Untitled" when empty. */
    public static String name(String name) {
        String n = name == null ? "" : name.replace('\n', ' ').replace('\r', ' ').strip();
        if (n.isEmpty()) n = "Untitled";
        return n.length() > MAX_NAME ? n.substring(0, MAX_NAME) : n;
    }

    public static boolean fits(String code) { return code != null && code.length() <= MAX_CODE; }

    /** A name not already taken: "Chase", then "Chase 2", "Chase 3"... */
    public static String unique(String base, java.util.Collection<String> taken) {
        String name = name(base);
        if (!taken.contains(name)) return name;
        for (int i = 2; ; i++) {
            String tail = " " + i;
            String candidate = name(name.length() + tail.length() > MAX_NAME ? name.substring(0, MAX_NAME - tail.length()) + tail : name + tail);
            if (!taken.contains(candidate)) return candidate;
        }
    }
}
