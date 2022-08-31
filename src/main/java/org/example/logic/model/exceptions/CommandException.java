package org.example.logic.model.exceptions;

public class CommandException extends Exception {

    public CommandException(String commandMalformed) {
        super(commandMalformed);
    }
}
