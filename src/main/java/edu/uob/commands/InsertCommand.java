package edu.uob.commands;

import edu.uob.model.Database;
import java.util.List;
import java.util.ArrayList;


public class InsertCommand {
    private final Database database;

    public InsertCommand(Database database) {
        this.database = database;
    }

    public String execute(String[] tokens) {
        String fullCommand = String.join(" ", tokens).replace(";", "").trim();
        if (fullCommand.toUpperCase().indexOf("VALUES") == -1) {
            return "[ERROR] Invalid INSERT syntax";
        }
        String tableName = tokens[2].replaceAll("[^a-zA-Z0-9_]", "").toLowerCase().trim();
        List<String> values = extractValues(fullCommand);
        if (values.isEmpty()) {
            return "[ERROR] Invalid INSERT syntax";
        }
        return database.insertIntoTable(tableName, values);
    }


    /**
     * Extract the values in parentheses from the complete INSERT command string
     * support the inclusion of any special characters in single quotes.
     * for example：INSERT INTO Marks VALUES('%.{$abc}abc', 65.1, TRUE)
     * will take out ["'%.{$abc}abc'", "65.1", "TRUE"]
     */
    private List<String> extractValues(String raw) {
        List<String> result = new ArrayList<>();
        int start = raw.indexOf("(");
        int end = raw.lastIndexOf(")");
        if (start == -1 || end == -1 || end <= start) {
            return result;
        }
        String valuesSubstring = raw.substring(start + 1, end);
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < valuesSubstring.length(); i++) {
            char c = valuesSubstring.charAt(i);
            if (c == '\'') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            result.add(processValue(current.toString().trim()));
        }
        return result;
    }

    private String processValue(String value) {
        return value.replace("'", "");
    }

}

