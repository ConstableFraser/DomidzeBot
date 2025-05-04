package org.shvedchikov.domidzebot.util;

import java.util.Arrays;
import java.util.Optional;

public enum Command {
    START,
    REGISTER,
    MONTHMINUS,
    MONTH,
    HALFYEAR,
    MONTHPREV,
    HALFYEARPREV,
    PERIOD,
    HELP,
    ACTIVATE,
    GETHASH,
    SETHASH,
    ENCODESTRING,
    DECODESTRING,
    ENCODEPWD,
    DECODEPWD,
    USERS,
    NOTEXIST;

    public static Optional<Command> getCommand(String name) {
        return Arrays.stream(Command.values())
                .filter((c) -> c.name().equals(name))
                .findFirst();
    }
}
