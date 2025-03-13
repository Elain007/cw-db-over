package edu.uob.commands;

import edu.uob.model.Database;
import edu.uob.parser.QueryParser;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;

public class SelectCommand {
    private final Database database;

    public SelectCommand(Database database) {
        this.database = database;
    }

    public String execute(String command, File databasePath) {
        System.out.println("[DEBUG] Executing SELECT command: " + command);
        // Parse table name
        String tableName = QueryParser.extractTableName(command);
        if (tableName == null) {
            System.out.println("[DEBUG] Invalid table name in SELECT command.");
            return "[ERROR] Invalid table name";
        }
        System.out.println("[DEBUG] Extracted table name: '" + tableName + "'");
        File tableFile = new File(databasePath, tableName + ".tab");
        System.out.println("[DEBUG] Looking for table file at: " + tableFile.getAbsolutePath());
        if (!tableFile.exists()) {
            System.out.println("[DEBUG] Table file does not exist.");
            return "[ERROR] Table does not exist";
        }
        try {
            List<String> lines = Files.readAllLines(tableFile.toPath());
            if (lines.isEmpty()) {
                System.out.println("[DEBUG] Table file is empty.");
                return "[ERROR] Table is empty";
            }
            // Get and process header
            String headerLine = lines.get(0);
            System.out.println("[DEBUG] Retrieved table header: " + headerLine);
            String[] headerColumns = headerLine.split("\t");
            List<String> headerList = new ArrayList<>();
            for (String header : headerColumns) {
                headerList.add(header.trim());
            }
            System.out.println("[DEBUG] Processed header columns: " + headerList);

            // Parse the column name part in the SELECT statement
            List<String> selectedColumns = QueryParser.extractSelectColumns(command);
            System.out.println("[DEBUG] Selected columns from query: " + selectedColumns);
            if (selectedColumns.isEmpty()) {
                System.out.println("[DEBUG] No columns specified in SELECT command.");
                return "[ERROR] Invalid SELECT syntax";
            }

            // Determine the column index of the returned data according to the selected column.
            List<Integer> selectedIndices = new ArrayList<>();
            if (selectedColumns.contains("*")) {
                for (int i = 0; i < headerList.size(); i++) {
                    selectedIndices.add(i);
                }
            } else {
                for (String col : selectedColumns) {
                    int index = headerList.indexOf(col);
                    if (index == -1) {
                        System.out.println("[DEBUG] Column '" + col + "' not found in header.");
                        return "[ERROR] Column '" + col + "' does not exist";
                    }
                    selectedIndices.add(index);
                }
            }
            System.out.println("[DEBUG] Selected column indices: " + selectedIndices);

            // Parse the WHERE clause 
            int wherePos = command.toUpperCase().indexOf("WHERE");
            boolean hasWhereClause = wherePos != -1;
            List<String[]> conditions = new ArrayList<>();
            if (hasWhereClause) {
                String whereClause = command.substring(wherePos + 5).trim();
                if (whereClause.endsWith(";")) {
                    whereClause = whereClause.substring(0, whereClause.length() - 1).trim();
                }
                boolean isComposite = whereClause.toUpperCase().contains("AND");
                List<String> condStrings = new ArrayList<>();
                if (isComposite) {
                    while (whereClause.startsWith("(") && whereClause.endsWith(")")) {
                        int count = 0;
                        boolean balanced = true;
                        for (int i = 0; i < whereClause.length(); i++) {
                            char c = whereClause.charAt(i);
                            if (c == '(') count++;
                            else if (c == ')') count--;
                            if (count == 0 && i < whereClause.length() - 1) {
                                balanced = false;
                                break;
                            }
                        }
                        if (balanced) {
                            whereClause = whereClause.substring(1, whereClause.length() - 1).trim();
                        } else {
                            break;
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    int parenCount = 0;
                    for (int i = 0; i < whereClause.length(); i++) {
                        char c = whereClause.charAt(i);
                        if (c == '(') {
                            parenCount++;
                            sb.append(c);
                        } else if (c == ')') {
                            parenCount--;
                            sb.append(c);
                        } else {
                            if (parenCount == 0 && i + 2 < whereClause.length() &&
                                    whereClause.substring(i, i + 3).equalsIgnoreCase("AND")) {
                                condStrings.add(sb.toString().trim());
                                sb.setLength(0);
                                i += 2; 
                            } else {
                                sb.append(c);
                            }
                        }
                    }
                    if (sb.length() > 0) {
                        condStrings.add(sb.toString().trim());
                    }
                } else {
                    condStrings.add(whereClause);
                }
                for (String condStr : condStrings) {
                    while (condStr.startsWith("(") && condStr.endsWith(")")) {
                        condStr = condStr.substring(1, condStr.length() - 1).trim();
                    }
                    String[] parts = condStr.split("\\s+");
                    if (parts.length != 3) {
                        System.out.println("[DEBUG] Invalid WHERE clause detected: '" + condStr + "'");
                        return "[ERROR] Invalid WHERE clause";
                    }
                    String col = parts[0].trim();
                    String op = parts[1].trim();
                    String val = parts[2].trim();
                    val = val.replaceAll("^[\"']|[\"']$", "");
                    val = val.replaceAll("[^a-zA-Z0-9]", "");
                    System.out.println("[DEBUG] Parsed condition: '" + col + " " + op + " " + val + "'");
                    conditions.add(new String[]{col, op, val});
                }
                for (String[] cond : conditions) {
                    if (!headerList.contains(cond[0])) {
                        System.out.println("[DEBUG] WHERE column '" + cond[0] + "' not found in header.");
                        return "[ERROR] Column " + cond[0] + " does not exist";
                    }
                }
            }

            List<String> resultRows = new ArrayList<>();
            List<String> returnHeader = new ArrayList<>();
            if (selectedColumns.contains("*")) {
                returnHeader.addAll(headerList);
            } else {
                for (int idx : selectedIndices) {
                    returnHeader.add(headerList.get(idx));
                }
            }
            resultRows.add(String.join("\t", returnHeader));


            for (int i = 1; i < lines.size(); i++) {
                String rowLine = lines.get(i);
                String[] rowValues = rowLine.split("\t");
                boolean rowMatches = true;
                if (hasWhereClause) {
                    for (String[] cond : conditions) {
                        String col = cond[0];
                        String op = cond[1];
                        String val = cond[2];
                        int condIndex = headerList.indexOf(col);
                        if (condIndex >= rowValues.length) {
                            rowMatches = false;
                            break;
                        }
                        String actualValue = rowValues[condIndex].trim();
                        System.out.println("[DEBUG] Row " + i + " condition (" + col + " " + op + " " + val + "): '" + actualValue + "'");
                        boolean condResult = false;
                        switch(op) {
                            case "==":
                                condResult = actualValue.equalsIgnoreCase(val);
                                break;
                            case "!=":
                                condResult = !actualValue.equalsIgnoreCase(val);
                                break;
                            case ">":
                                try {
                                    condResult = Double.parseDouble(actualValue) > Double.parseDouble(val);
                                } catch (NumberFormatException e) {
                                    condResult = false;
                                }
                                break;
                            case "<":
                                try {
                                    condResult = Double.parseDouble(actualValue) < Double.parseDouble(val);
                                } catch (NumberFormatException e) {
                                    condResult = false;
                                }
                                break;
                            case "LIKE":
                                condResult = actualValue.toLowerCase().contains(val.toLowerCase());
                                break;
                            default:
                                System.out.println("[DEBUG] Unsupported operator in WHERE clause: " + op);
                                return "[ERROR] Unsupported operator";
                        }
                        if (!condResult) {
                            rowMatches = false;
                            break;
                        }
                    }
                }
                if (rowMatches) {
                    List<String> selectedValues = new ArrayList<>();
                    if (selectedColumns.contains("*")) {
                        for (String value : rowValues) {
                            selectedValues.add(value.trim());
                        }
                    } else {
                        for (int idx : selectedIndices) {
                            if (idx < rowValues.length)
                                selectedValues.add(rowValues[idx].trim());
                        }
                    }
                    resultRows.add(String.join("\t", selectedValues));
                }
            }

            String finalResult = String.join(System.lineSeparator(), resultRows);
            return "[OK]" + System.lineSeparator() + finalResult;
        } catch (IOException e) {
            System.out.println("[DEBUG] Exception reading table file: " + e.getMessage());
            return "[ERROR] Failed to read table";
        }
    }
}

