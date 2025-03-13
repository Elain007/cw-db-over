package edu.uob.parser;

import edu.uob.commands.*;
import edu.uob.model.Database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class QueryExecuter {
//    private String command;
    private String storageFolderPath;
    private Database database;

    public QueryExecuter() {
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        database = new Database(storageFolderPath);
    }

    public String execute(String command) throws IOException {
        command = command.trim();
        if (command.isEmpty()) {
            return "[ERROR] command is empty.";
        }
        String[] commandTokens = command.split("\\s+");
        String commandType = commandTokens[0].toUpperCase();
        String result;
        switch (commandType) {
            case "SELECT" -> result = executeSelectCommand(command);
            case "USE" -> result = executeUseCommand(command);
            case "CREATE" -> result = executeCreateCommand(commandTokens);
            case "DROP" -> result = executeDropCommand(commandTokens);
            case "ALTER" -> result = new AlterTableCommand(database).execute(commandTokens);
            case "INSERT" -> result = new InsertCommand(database).execute(commandTokens);
            case "UPDATE" -> result = new UpdateCommand(database).execute(commandTokens);
            case "DELETE" -> result = new DeleteCommand(database).execute(commandTokens);
            case "JOIN" -> result = new JoinCommand(database).execute(commandTokens);
            default -> result = "[ERROR] Unsupported command.";
        }
        return result;
    }

    private String executeSelectCommand(String command) {
        if (database.getCurrentDatabase().isEmpty()) {
            return "[ERROR] No database selected: QueryExecuter.executeSelectCommand";
        } else {
            File currentDatabaseFolder = database.getCurrentDatabasePath();
            return new SelectCommand(database).execute(command, currentDatabaseFolder);
        }
    }

    private String executeUseCommand(String command) throws IOException {
        String[] tokens = command.split("\\s+");
        database.useDatabase(tokens[1].replace(";", ""));
        return "[OK]";
    }

    private String executeCreateCommand(String[] commandTokens) {
        String createType = commandTokens[1].toUpperCase();
        if (createType.equals("DATABASE")) {
            return new CreateDatabaseCommand(storageFolderPath).execute(commandTokens);
        } else if (createType.equals("TABLE")) {
            return new CreateTableCommand(database).execute(commandTokens);
        } else {
            return "[ERROR] Unsupported command.";
        }
    }

    private String executeDropCommand(String[] commandTokens) {
        String dropType = commandTokens[1].toUpperCase();
        if (dropType.equals("DATABASE")) {
            return new DropDatabaseCommand(database).execute(commandTokens);
        } else if (dropType.equals("TABLE")) {
            return new DropTableCommand(database).execute(commandTokens);
        } else {
            return "[ERROR] Unsupported command.";
        }
    }
}
