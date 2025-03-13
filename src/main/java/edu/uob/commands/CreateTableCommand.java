package edu.uob.commands;

import edu.uob.model.Database;

import java.util.List;
import java.util.ArrayList;


public class CreateTableCommand {
    private final Database database;

    public CreateTableCommand(Database database) {
        this.database = database;
    }

    public String execute(String[] tokens) {
        if (database.getCurrentDatabasePath() == null) {
            return "[ERROR] No database selected: CreateTableCommand.execute";
        }
        if (tokens.length < 3) {
            return "[ERROR] Invalid CREATE TABLE syntax";
        }
        String tableName = tokens[2].replaceAll(";", "").trim();
        StringBuilder columnsBuilder = new StringBuilder();
        for (int i = 3; i < tokens.length; i++) {
            columnsBuilder.append(tokens[i]).append(" ");
        }
        String columnsStr = columnsBuilder.toString().trim().replaceAll("[()]", "");
        String[] rawColumns = columnsStr.split(",");
        List<String> columns = new ArrayList<>();
        for (String col : rawColumns) {
            String trimmed = col.trim().replaceAll(";", "").toLowerCase();
            if (!trimmed.matches("^[a-zA-Z][a-zA-Z0-9]*$") &&
                    !trimmed.isEmpty()) {
                return "[ERROR] Invalid column name: " + trimmed;
            }

            if (columns.contains(trimmed)) {
                return "[ERROR] Duplicate column name: " + trimmed;
            }
            columns.add(trimmed);
        }
        return database.createTable(tableName, columns);
    }
}

