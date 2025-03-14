package edu.uob.commands;

import edu.uob.model.Database;
import edu.uob.parser.QueryParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class UpdateCommand {
    private final Database database;

    public UpdateCommand(Database database) {
        this.database = database;
    }

    public String execute(String[] tokens) {
        if (tokens.length < 6) {
            //System.out.println("[DEBUG] UPDATE syntax invalid: tokens length " + tokens.length);
            return "[ERROR] Invalid UPDATE syntax";
        }
        String tableName = tokens[1];
        File databasePath = database.getCurrentDatabasePath();
        File tableFile = new File(databasePath, tableName + ".tab");
        if (!tableFile.exists()) {
            //System.out.println("[DEBUG] Table file for '" + tableName + "' does not exist at " + tableFile.getAbsolutePath());
            return "[ERROR] Table does not exist";
        }

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
        String headerLine = lines.get(0);
        String[] headerColumns = headerLine.split("\t");
        List<String> headerList = new ArrayList<>();
        for (String header : headerColumns) {
            headerList.add(header.trim());
        }

        if (!tokens[2].equalsIgnoreCase("SET")) {
            //System.out.println("[DEBUG] Missing SET keyword in UPDATE command");
            return "[ERROR] Invalid UPDATE syntax: missing SET";
        }
        int whereIndex = -1;
        for (int i = 3; i < tokens.length; i++) {
            if (tokens[i].equalsIgnoreCase("WHERE")) {
                whereIndex = i;
                break;
            }
        }

        int endIndex = (whereIndex == -1 ? tokens.length : whereIndex);
        StringBuilder setClauseBuilder = new StringBuilder();
        for (int i = 3; i < endIndex; i++) {
            setClauseBuilder.append(tokens[i]).append(" ");
        }
        String setClause = setClauseBuilder.toString().trim();
        String[] assignments = setClause.split(",");
        Map<String, String> updates = new HashMap<>();
        for (String assignment : assignments) {
            String[] parts = assignment.split("=");
            if (parts.length != 2) {
                //System.out.println("[DEBUG] Invalid assignment syntax: " + assignment);
                return "[ERROR] Invalid SET syntax";
            }
            String col = parts[0].trim();
            String newValue = parts[1].trim();
            newValue = newValue.replaceAll("[^a-zA-Z0-9]", "");
            //System.out.println("[DEBUG] Update assignment: column='" + col + "', newValue='" + newValue + "'");
            if (col.equalsIgnoreCase("id")) {
                //System.out.println("[DEBUG] Attempt to update primary key 'id' detected");
                return "[ERROR] Cannot update primary key";
            }
            if (!headerList.contains(col)) {
                //System.out.println("[DEBUG] Column '" + col + "' not found in header: " + headerList);
                return "[ERROR] Column '" + col + "' does not exist";
            }
            updates.put(col, newValue);
        }
        String conditionColumn = null;
        String whereOperator = null;
        String conditionValue = null;
        boolean hasWhereClause = (whereIndex != -1);
        if (hasWhereClause) {
            if (tokens.length < whereIndex + 4) {
                //System.out.println("[DEBUG] Invalid WHERE syntax: tokens length " + tokens.length + ", expected at least " + (whereIndex + 4));
                return "[ERROR] Invalid WHERE syntax";
            }
            conditionColumn = tokens[whereIndex + 1];
            whereOperator = tokens[whereIndex + 2];
            conditionValue = tokens[whereIndex + 3];
            //System.out.println("[DEBUG] Parsed WHERE condition: column='" + conditionColumn +
                    //"', operator='" + whereOperator + "', raw value='" + conditionValue + "'");
            if ((conditionValue.startsWith("'") && conditionValue.endsWith("'")) ||
                    (conditionValue.startsWith("\"") && conditionValue.endsWith("\""))) {
                conditionValue = conditionValue.substring(1, conditionValue.length() - 1);
                //System.out.println("[DEBUG] Cleaned WHERE condition value: '" + conditionValue + "'");
            }
            conditionValue = conditionValue.replaceAll("[^a-zA-Z0-9]", "");
            //System.out.println("[DEBUG] Cleaned WHERE condition value: '" + conditionValue + "'");
            if (!headerList.contains(conditionColumn)) {
                //System.out.println("[DEBUG] WHERE column '" + conditionColumn + "' not found in header: " + headerList);
                return "[ERROR] Column '" + conditionColumn + "' does not exist";
            }
        }

        List<String> newLines = new ArrayList<>();
        newLines.add(headerLine); 
        for (int i = 1; i < lines.size(); i++) {
            String rowLine = lines.get(i);
            String[] rowValues = rowLine.split("\t", -1);
            for(String value : rowValues) {
                //System.out.println(value + "123");
            }
            boolean rowMatches = true;
            if (hasWhereClause) {
                int condIndex = headerList.indexOf(conditionColumn);
                if (condIndex < rowValues.length) {
                    String actualValue = rowValues[condIndex].trim();
                    //System.out.println("[DEBUG] Row " + i + " actual value for column '" + conditionColumn + "': '" + actualValue + "'");
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
                            //System.out.println("[DEBUG] Unsupported operator in WHERE clause: " + whereOperator);
                            return "[ERROR] Unsupported operator in WHERE clause";
                    }
                } else {
                    rowMatches = false;
                }
            }
            if (rowMatches) {
                for (Map.Entry<String, String> entry : updates.entrySet()) {
                    int colIndex = headerList.indexOf(entry.getKey());
                    if (colIndex < rowValues.length) {
                        rowValues[colIndex] = entry.getValue();
                        //System.out.println("[DEBUG] Row " + i + " updated column '" + entry.getKey() +
                                //"' to '" + entry.getValue() + "'");
                    }
                }
            }
            String updatedRow = String.join("\t", rowValues);
            newLines.add(updatedRow);
        }
        
        try {
            Files.write(tableFile.toPath(), newLines);
            //System.out.println("[DEBUG] Successfully wrote updated data to table file: " + tableFile.getAbsolutePath());
            return "[OK] Update successful";
        } catch (IOException e) {
            //System.out.println("[DEBUG] IOException writing table file: " + e.getMessage());
            return "[ERROR] Failed to write table";
        }
    }
}



