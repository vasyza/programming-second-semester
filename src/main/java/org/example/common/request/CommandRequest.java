package org.example.common.request;

import java.io.Serial;
import java.io.Serializable;

public class CommandRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 4L;
    private final String commandName;
    private final Object argument;

    public CommandRequest(String commandName, Object argument) {
        this.commandName = commandName;
        this.argument = argument;
    }

    public String getCommandName() { return commandName; }
    public Object getArgument() { return argument; }
}