package edu.uob.commands;

import edu.uob.model.Database;


public class UseDatabaseCommand {
    private final Database database;

    public UseDatabaseCommand(Database database) {
        this.database = database;
    }

    public String execute(String[] tokens) {
        if (tokens.length < 2) {
            return "[ERROR] Invalid USE syntax";
        }
        //System.out.println(tokens[1]);
        String dbName = tokens[1].replaceAll(";", "").toLowerCase().trim();
        String result = database.useDatabase(dbName);

        if (!result.startsWith("[OK]")) {
        }
        return result;
    }
}



