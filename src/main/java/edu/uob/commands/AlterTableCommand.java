package edu.uob.commands;

import edu.uob.model.Database;
import java.io.IOException;

public class AlterTableCommand {
    private final Database database;

    public AlterTableCommand(Database database) {
        this.database = database;
    }

    public String execute(String[] tokens) {
        if (tokens.length < 5 || !tokens[1].equalsIgnoreCase("TABLE")) {
            return "[ERROR] Invalid ALTER TABLE syntax";
        }
        String tableName = tokens[2].toLowerCase().trim();
        String operation = tokens[3].toUpperCase().trim();
        if (operation.equals("DROP")) {
            String columnName;
            if (tokens.length >= 6 && tokens[4].equalsIgnoreCase("COLUMN")) {
                columnName = tokens[5].replaceAll(";", "").toLowerCase().trim();
            } else {
                columnName = tokens[4].replaceAll(";", "").toLowerCase().trim();
            }
            try {
                return database.alterTableDropColumn(tableName, columnName);
            } catch (IOException e) {
                return "[ERROR] " + e.getMessage();
            }
        } else if (operation.equals("ADD")) {
            String columnName;
            if (tokens.length >= 6 && tokens[4].equalsIgnoreCase("COLUMN")) {
                columnName = tokens[5].replaceAll(";", "").toLowerCase().trim();
            } else {
                columnName = tokens[4].replaceAll(";", "").toLowerCase().trim();
            }
            return database.alterTableAddColumn(tableName, columnName);
        }
        return "[ERROR] Unsupported ALTER TABLE command";
    }
}
