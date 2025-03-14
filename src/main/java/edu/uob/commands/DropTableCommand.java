package edu.uob.commands;

import edu.uob.model.Database;


public class DropTableCommand {
    private final Database database;

    public DropTableCommand(Database database) {
        this.database = database;
    }

    public String execute(String[] tokens) {
        if (tokens.length < 3)
            return "[ERROR] Invalid DROP TABLE syntax";
        String tableName = tokens[2].toLowerCase();
        //System.out.println("[DEBUG] DROP TABLE command for table: " + tableName);
        return database.dropTable(tableName);
    }
}

