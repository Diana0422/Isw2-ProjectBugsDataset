package org.example.logic.model.exceptions;

public class CommandException extends Exception {

    public CommandException(String command_malformed) {
        super(command_malformed);
    }
}
