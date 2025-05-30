package org.example.common.request;

import java.io.Serial;
import java.io.Serializable;

public class CommandRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 104L;
    private final String commandName;
    private final Object argument;
    private final String username;
    private final String password;

    public CommandRequest(String commandName, Object argument) {
        this(commandName, argument, null, null);
    }

    public CommandRequest(String commandName, Object argument, String username, String password) {
        this.commandName = commandName;
        this.argument = argument;
        this.username = username;
        this.password = password;
    }

    public String getCommandName() {
        return commandName;
    }

    public Object getArgument() {
        return argument;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}