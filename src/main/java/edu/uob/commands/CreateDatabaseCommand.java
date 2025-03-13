package edu.uob.commands;

import edu.uob.model.Database;

public class CreateDatabaseCommand {
    private final Database database;

    public CreateDatabaseCommand(String storageFolderPath) {
        this.database = new Database(storageFolderPath);
    }

    public String execute(String[] tokens) {
        if (tokens.length < 3) {
            return "[ERROR] Invalid CREATE DATABASE syntax";
        }

        String dbName = tokens[2].replaceAll(";", "").trim();
        return database.createDatabase(dbName);
    }



}

