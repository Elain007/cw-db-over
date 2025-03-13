package edu.uob.commands;

import edu.uob.model.Database;

import java.io.IOException;

public class JoinCommand {
    private final Database database;

    public JoinCommand(Database database) {
        this.database = database;
    }

    public String execute(String[] tokens) throws IOException {
        if (tokens.length < 6 || !tokens[2].equalsIgnoreCase("AND") || !tokens[4].equalsIgnoreCase("ON")) {
            return "[ERROR] Invalid JOIN syntax. Use: JOIN table1 AND table2 ON column1 AND column2;";
        }

        String table1 = tokens[1].toLowerCase();
        String table2 = tokens[3].toLowerCase();
        String column1 = tokens[5].toLowerCase();
        String column2 = tokens[7].replace(";", "").toLowerCase();

        return database.joinTables(table1, table2, column1, column2);
    }
}

