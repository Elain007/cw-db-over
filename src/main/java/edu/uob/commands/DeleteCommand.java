package edu.uob.commands;

import edu.uob.model.Database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DeleteCommand {
    private final Database database;

    public DeleteCommand(Database database) {
        this.database = database;
    }

    public String execute(String[] tokens) {
        if (tokens.length < 5) {
            //System.out.println("[DEBUG] DELETE syntax invalid: tokens length " + tokens.length);
            return "[ERROR] Invalid DELETE syntax";
        }

        // Two DELETE syntax are supported:
        // 1. DELETE FROM tableName WHERE conditionColumn operator conditionValue
        // 2. DELETE tableName WHERE conditionColumn operator conditionValue
        String tableName;
        int whereStartIndex;
        if (tokens[0].equalsIgnoreCase("DELETE") && tokens[1].equalsIgnoreCase("FROM")) {
            tableName = tokens[2];
            whereStartIndex = 3;
        } else {
            tableName = tokens[1];
            whereStartIndex = 2;
        }

        // Check whether the WHERE keyword exists.
        if (tokens.length <= whereStartIndex || !tokens[whereStartIndex].equalsIgnoreCase("WHERE")) {
            //System.out.println("[DEBUG] DELETE command missing WHERE clause");
            return "[ERROR] DELETE command must contain a WHERE clause";
        }

        // Get the database directory
        File databasePath = database.getCurrentDatabasePath();
        File tableFile = new File(databasePath, tableName + ".tab");
        if (!tableFile.exists()) {
            //System.out.println("[DEBUG] Table file for '" + tableName + "' does not exist at " + tableFile.getAbsolutePath());
            return "[ERROR] Table does not exist";
        }

        // Read table file content
        List<String> lines;
        try {
            lines = Files.readAllLines(tableFile.toPath());
        } catch (IOException e) {
            //System.out.println("[DEBUG] IOException reading table file: " + e.getMessage());
            return "[ERROR] Failed to read table";
        }
        if (lines.isEmpty()) {
            //System.out.println("[DEBUG] Table file is empty");
            return "[ERROR] Table is empty";
        }

        // Analytic header
        String headerLine = lines.get(0);
        String[] headerColumns = headerLine.split("\t");
        List<String> headerList = new ArrayList<>();
        for (String header : headerColumns) {
            headerList.add(header.trim());
        }

        // Analytic WHERE condition
        if (tokens.length < whereStartIndex + 4) {
            //System.out.println("[DEBUG] Invalid WHERE syntax in DELETE: tokens length " + tokens.length + ", expected at least " + (whereStartIndex + 4));
            return "[ERROR] Invalid WHERE syntax";
        }
        String conditionColumn = tokens[whereStartIndex + 1];
        String whereOperator = tokens[whereStartIndex + 2];
        String conditionValue = tokens[whereStartIndex + 3];
        //System.out.println("[DEBUG] Parsed DELETE WHERE condition: column='" + conditionColumn +
                //"', operator='" + whereOperator + "', raw value='" + conditionValue + "'");

        if ((conditionValue.startsWith("'") && conditionValue.endsWith("'")) ||
                (conditionValue.startsWith("\"") && conditionValue.endsWith("\""))) {
            conditionValue = conditionValue.substring(1, conditionValue.length() - 1);
            //System.out.println("[DEBUG] Cleaned DELETE WHERE condition value: '" + conditionValue + "'");
        }

        // Clear all characters except numbers and letters in conditionValue.
        conditionValue = conditionValue.replaceAll("[^a-zA-Z0-9]", "");
        //System.out.println("[DEBUG] Cleaned DELETE WHERE condition value: '" + conditionValue + "'");

        if (!headerList.contains(conditionColumn)) {
            //System.out.println("[DEBUG] DELETE WHERE column '" + conditionColumn + "' not found in header: " + headerList);
            return "[ERROR] Column '" + conditionColumn + "' does not exist";
        }

        // Traverse the data rows and delete the rows that meet the WHERE condition.
        List<String> newLines = new ArrayList<>();
        newLines.add(headerLine); 
        for (int i = 1; i < lines.size(); i++) {
            String rowLine = lines.get(i);
            String[] rowValues = rowLine.split("\t", -1);
            boolean rowMatches = true;
            int condIndex = headerList.indexOf(conditionColumn);
            if (condIndex < rowValues.length) {
                String actualValue = rowValues[condIndex].trim();
                //System.out.println("[DEBUG] Row " + i + " actual value for DELETE, column '" + conditionColumn + "': '" + actualValue + "'");
                switch (whereOperator) {
                    case "==":
                        if (!actualValue.equalsIgnoreCase(conditionValue))
                            rowMatches = false;
                        break;
                    case "!=":
                        if (actualValue.equalsIgnoreCase(conditionValue))
                            rowMatches = false;
                        break;
                    case ">":
                        try {
                            if (Double.parseDouble(actualValue) <= Double.parseDouble(conditionValue))
                                rowMatches = false;
                        } catch (NumberFormatException e) {
                            rowMatches = false;
                        }
                        break;
                    case "<":
                        try {
                            if (Double.parseDouble(actualValue) >= Double.parseDouble(conditionValue))
                                rowMatches = false;
                        } catch (NumberFormatException e) {
                            rowMatches = false;
                        }
                        break;
                    default:
                        //System.out.println("[DEBUG] DELETE unsupported operator: " + whereOperator);
                        return "[ERROR] Unsupported operator in WHERE clause";
                }
            } else {
                rowMatches = false;
            }

            if (!rowMatches) {
                newLines.add(rowLine);
            }else {
                //System.out.println("[DEBUG] Row " + i + " matched DELETE condition and will be removed");
            }
        }

        try {
            Files.write(tableFile.toPath(), newLines);
            //System.out.println("[DEBUG] Successfully wrote updated data to table file: " + tableFile.getAbsolutePath());
            return "[OK] Delete successful";
        } catch (IOException e) {
            //System.out.println("[DEBUG] IOException writing table file: " + e.getMessage());
            return "[ERROR] Failed to write table";
        }
    }
}

