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
        //System.out.println("[DEBUG] ALTER TABLE command for table: " + tableName);

        if (operation.equals("DROP")) {
            String columnName;
            // 如果命令语法是 "ALTER TABLE marks DROP COLUMN pass;"
            if (tokens.length >= 6 && tokens[4].equalsIgnoreCase("COLUMN")) {
                columnName = tokens[5].replaceAll(";", "").toLowerCase().trim();
            } else {
                // 如果命令语法是 "ALTER TABLE marks DROP pass;"
                columnName = tokens[4].replaceAll(";", "").toLowerCase().trim();
            }
            //System.out.println("[DEBUG] ALTER TABLE DROP COLUMN for column: " + columnName);
            try {
                return database.alterTableDropColumn(tableName, columnName);
                //System.out.println("[DEBUG] Result of DROP COLUMN: " + result);
            } catch (IOException e) {
                //System.out.println("[DEBUG] Exception in DROP COLUMN: " + e.getMessage());
                return "[ERROR] " + e.getMessage();
            }
        } else if (operation.equals("ADD")) {
            String columnName;
            // 支持 "ALTER TABLE marks ADD COLUMN age;" 或 "ALTER TABLE marks ADD age;"
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
