package edu.uob.model;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.io.File;

public class Database {
    private File rootPath;
    private File currentDatabasePath;
    private String currentDatabase = null;

    public Database(String rootDirectory) {
        this.rootPath = new File(rootDirectory);
        if (!rootPath.exists()) {
            rootPath.mkdirs();
        }
    }

    public String createDatabase(String dbName) {
        File databaseDir = new File(rootPath, dbName.toLowerCase().trim());
        if (databaseDir.exists()) {
            return "[ERROR] Database already exists";
        }
        if (databaseDir.mkdirs()) {
            String switchResult = useDatabase(dbName);
            return switchResult;
        }
        return "[ERROR] Failed to create database";
    }

    public String useDatabase(String dbName) {
        System.out.println(dbName);
        File databaseDir = new File(rootPath, dbName.toLowerCase().trim());
        if (!databaseDir.exists() || !databaseDir.isDirectory()) {
            return "[ERROR] Database does not exist";
        }

        this.currentDatabase = dbName.toLowerCase().trim();
        this.currentDatabasePath = databaseDir;

        return "[OK] Switched to database: " + dbName;
    }

    public void setCurrentDatabasePath(String path) {
        this.currentDatabasePath = new File(path);
    }

    // Create table: automatically add "id" at the front of the header.
    public String createTable(String tableName, List<String> columns) {
        if (this.currentDatabasePath == null) {
            return "[ERROR] No database selected: Database.createTable";
        }
        // Clean tableName
        String cleanedTableName = tableName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase().trim();
        File tableFile = new File(this.currentDatabasePath, cleanedTableName + ".tab");
        System.out.println(tableFile.toString() + "123456");
        if (tableFile.exists()) {
            return "[ERROR] Table already exists";
        }

        // Automatically add id column in header
        List<String> newHeaderColumns = new ArrayList<>();
        newHeaderColumns.add("id");
        // Traverse user-provided columns
        for (String col : columns) {
            newHeaderColumns.add(col.trim().replaceAll(";", ""));
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile))) {
            writer.write(String.join("\t", newHeaderColumns));
        } catch (IOException e) {
            return "[ERROR] Failed to create table";
        }

        return "[OK] Table created";
    }

    public File getCurrentDatabasePath() {
        return this.currentDatabasePath;
    }

    // Delete table
    public String dropTable(String tableName) {
        if (currentDatabasePath == null) {
            return "[ERROR] No database selected: Database.dropTable1";
        }
        File tableFile = new File(currentDatabasePath, tableName.toLowerCase().trim().replace(";", "") + ".tab");
        if (!tableFile.exists()) {
            return "[ERROR] Table does not exist";
        }
        if (tableFile.delete()) {
            return "[OK] Table dropped";
        } else {
            return "[ERROR] Failed to drop table";
        }
    }

    // Delete column
    public String alterTableDropColumn(String tableName, String columnName) throws IOException {
        if (currentDatabasePath == null) {
            return "[ERROR] No database selected: Database.alterTableDropColumn";
        }
        File tableFile = new File(currentDatabasePath, tableName.toLowerCase().trim() + ".tab");
        if (!tableFile.exists()) {
            return "[ERROR] Table does not exist";
        }
        List<String> lines = Files.readAllLines(tableFile.toPath());
        if (lines.isEmpty()) {
            return "[ERROR] Table is empty";
        }
        // Read the original header
        String headerLine = lines.get(0);
        System.out.println("[DEBUG] Original table header: " + headerLine);

        // Split header columns and trim each column name.
        String[] columns = headerLine.split("\t");
        List<String> trimmedColumns = new ArrayList<>();
        for (int i = 0; i < columns.length; i++) {
            String trimmed = columns[i].trim();
            trimmedColumns.add(trimmed);
            System.out.println("[DEBUG] Processed Header Column " + i + ": '" + trimmed + "'");
        }
        System.out.println("[DEBUG] Full processed header: " + trimmedColumns);

        // Trim the incoming columnName when comparing.
        String targetColumn = columnName.trim();
        System.out.println("[DEBUG] Column to drop (target): '" + targetColumn + "'");

        int columnIndex = -1;
        for (int i = 0; i < trimmedColumns.size(); i++) {
            if (trimmedColumns.get(i).equalsIgnoreCase(targetColumn)) {
                columnIndex = i;
                System.out.println("[DEBUG] Found column '" + targetColumn + "' at index: " + i);
                break;
            }
        }
        if (columnIndex == -1) {
            return "[ERROR] Column does not exist";
        }

        // Check if you are trying to delete the primary key "id"
        if (targetColumn.equalsIgnoreCase("id")) {
            return "[ERROR] Cannot drop primary key";
        }
        // Construct a new header that does not contain the deleted columns.
        StringBuilder newHeader = new StringBuilder();
        for (int i = 0; i < trimmedColumns.size(); i++) {
            if (i == columnIndex) continue;
            if (newHeader.length() > 0) {
                newHeader.append("\t");
            }
            newHeader.append(trimmedColumns.get(i));
        }
        System.out.println("[DEBUG] New table header after dropping column: " + newHeader.toString());

        /**
         * Construct new content: the first line is a new header, 
         * and the data of the corresponding column is deleted in each other line.
        */ 
        List<String> newLines = new ArrayList<>();
        newLines.add(newHeader.toString());
        for (int j = 1; j < lines.size(); j++) {
            String line = lines.get(j);
            String[] values = line.split("\t");
            StringBuilder newLine = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (i == columnIndex) continue;
                if (newLine.length() > 0) {
                    newLine.append("\t");
                }
                newLine.append(values[i].trim());
            }
            System.out.println("[DEBUG] New row " + j + ": " + newLine.toString());
            newLines.add(newLine.toString());
        }

        // Write the updated content back to the file.
        Files.write(tableFile.toPath(), newLines);

        // Re-read the file to confirm whether the new header was written successfully.
        List<String> checkLines = Files.readAllLines(tableFile.toPath());
        if (!checkLines.isEmpty()) {
            System.out.println("[DEBUG] Verified new table header from file: " + checkLines.get(0));
        } else {
            System.out.println("[DEBUG] Error: Table file is empty after writing new header.");
        }

        return "[OK] Column dropped";
    }



    // Consolidated table
    public String joinTables(String table1, String table2, String column1, String column2) throws IOException {
        if (currentDatabasePath == null) {
            return "[ERROR] No database selected: Database.selectFromTable";
        }
        File table1File = new File(currentDatabasePath, table1.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase().trim() + ".tab");
        File table2File = new File(currentDatabasePath, table2.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase().trim() + ".tab");
        List<String> table1Lines = Files.readAllLines(table1File.toPath());
        List<String> table2Lines = Files.readAllLines(table2File.toPath());
        List<String> table1Columns = List.of(table1Lines.get(0).split("\t"));
        List<String> table2Columns = List.of(table2Lines.get(0).split("\t"));
        table1Lines.remove(0);
        table2Lines.remove(0);
        int column1Index = table1Columns.indexOf(column1);
        int column2Index = table2Columns.indexOf(column2);
        int newId = 0;
        List<Record> newRecords = new ArrayList<>();
        for (String table1Line : table1Lines) {
            List<String> line1Values = List.of(table1Line.split("\t"));
            for (String table2Line : table2Lines) {
                List<String> line2Values = List.of(table2Line.split("\t"));
                if (line1Values.get(column1Index).equals(line2Values.get(column2Index))) {
                    List<String> newRow = new ArrayList<>();
                    newRow.add(String.valueOf(++newId));
                    newRow.addAll(line1Values);
                    newRow.addAll(line2Values);
                    newRecords.add(new Record(newRow));
                }
            }
        }
        List<String> newColumns = new ArrayList<>();
        newColumns.add("id");
        for (String table1Column : table1Columns) {
            newColumns.add(table1 + "." + table1Column);
        }
        for (String table2Column : table2Columns) {
            newColumns.add(table2 + "." + table2Column);
        }
        Table joinedTable = new Table(newColumns, newRecords);
        List<String> deletedColumns = new ArrayList<>();
        deletedColumns.add(table1 + "." + column1);
        deletedColumns.add(table2 + "." + column2);
        deletedColumns.add(table1 + ".id");
        deletedColumns.add(table2 + ".id");
        for (String column : deletedColumns) {
            joinedTable.deleteColumn(column);
        }

        return "[OK]" + System.lineSeparator() + joinedTable.getTableString();
    }

    // polling list
    public String selectFromTable(String tableName) {
        if (currentDatabasePath == null) {
            return "[ERROR] No database selected: Database.selectFromTable";
        }
        // Handle table names and remove illegal characters.
        File tableFile = new File(currentDatabasePath, tableName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase().trim() + ".tab");
        if (!tableFile.exists()) {
            return "[ERROR] Table does not exist";
        }

        try {
            List<String> lines = Files.readAllLines(tableFile.toPath());
            if (lines.isEmpty()) {
                return "[ERROR] Table is empty";
            }
            String data = String.join(System.lineSeparator(), lines);
            return "[OK] " + data;
        } catch (IOException e) {
            return "[ERROR] Failed to read table";
        }
    }

    // Insert data
    public String insertIntoTable(String tableName, List<String> values) {
        if (currentDatabasePath == null) {
            return "[ERROR] No database selected: Database: insertIntoTable";
        }
        String cleanedTableName = tableName.toLowerCase().trim();
        File tableFile = new File(currentDatabasePath, cleanedTableName + ".tab");
        if (!tableFile.exists()) {
            return "[ERROR] Table does not exist";
        }

        try {
            List<String> lines = Files.readAllLines(tableFile.toPath());
            int nextId = lines.size();
            List<String> newRow = new ArrayList<>();
            newRow.add(String.valueOf(nextId));
            newRow.addAll(values);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, true))) {
                writer.newLine();
                writer.write(String.join("\t", newRow));
            }
            return "[OK] Insert successful";
        } catch (IOException e) {
            return "[ERROR] Insert failed";
        }
    }

    // Check whether the database exists.
    public boolean databaseExists(String dbName) {
        File databaseDir = new File(rootPath, dbName.toLowerCase());
        return databaseDir.exists();
    }

    // Delete database
    public String dropDatabase(String dbName) {
        File databaseDir = new File(rootPath, dbName.toLowerCase().trim().replace(";", ""));
        if (!databaseDir.exists()) {
            return "[ERROR] Database does not exist";
        }
        if (deleteDirectory(databaseDir)) {
            return "[OK] Database dropped";
        }
        return "[ERROR] Failed to drop database";
    }

    // Recursively delete folders
    private boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                deleteDirectory(file);
            }
        }
        return dir.delete();
    }

    public String getCurrentDatabase() {
        return currentDatabase;
    }

    // Add column
    public String alterTableAddColumn(String tableName, String columnName) {
        if (currentDatabasePath == null) {
            return "[ERROR] No database selected: Database.alterTableAddColumn";
        }
        File tableFile = new File(currentDatabasePath, tableName.toLowerCase().trim() + ".tab");
        if (!tableFile.exists()) {
            return "[ERROR] Table does not exist";
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(tableFile.toPath());
        } catch (IOException e) {
            return "[ERROR] Failed to read table";
        }
        if (lines.isEmpty()) {
            return "[ERROR] Table is empty";
        }
        String headerLine = lines.get(0);
        String[] columns = headerLine.split("\t");
        for (String col : columns) {
            if (col.equalsIgnoreCase(columnName.trim())) {
                return "[ERROR] Duplicate column name: " + columnName;
            }
        }
        String newHeader = headerLine + "\t" + columnName.trim().toLowerCase();
        List<String> newLines = new ArrayList<>();
        newLines.add(newHeader);
        for (int i = 1; i < lines.size(); i++) {
            newLines.add(lines.get(i) + "\t ");
        }
        try {
            Files.write(tableFile.toPath(), newLines);
        } catch (IOException e) {
            return "[ERROR] Failed to alter table";
        }
        return "[OK] Column added";
    }
}




