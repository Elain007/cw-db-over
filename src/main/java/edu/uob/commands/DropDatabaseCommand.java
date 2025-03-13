package edu.uob.commands;

import edu.uob.model.Database;

public class DropDatabaseCommand {
    private final Database database;

    public DropDatabaseCommand(Database database) {
        this.database = database;
    }

    public String execute(String[] tokens) {
        if (tokens.length < 3 || !tokens[1].equalsIgnoreCase("DATABASE")) {
            return "[ERROR] Invalid DROP DATABASE syntax";
        }
        String databaseName = tokens[2].toLowerCase();
        return database.dropDatabase(databaseName);
    }
}

