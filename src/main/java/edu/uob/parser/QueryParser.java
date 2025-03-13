package edu.uob.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class QueryParser {

    /**
     * Parse column names from SQL command (for CREATE TABLE)
     * @param command SQL command string
     * @return Array of column names
     */
    public static List<String> extractColumns(String command) {
        int start = command.indexOf("(");
        int end = command.lastIndexOf(")");
        if (start == -1 || end == -1 || end <= start) {
            return new ArrayList<>(); 
        }
        String columnsPart = command.substring(start + 1, end).trim();
        String[] columnsArray = columnsPart.split("\\s*,\\s*");
        return new ArrayList<>(Arrays.asList(columnsArray));
    }

    /**
     * Parse values from SQL command (for INSERT INTO)
     * @param command SQL command string
     * @return Array of parsed values
     */
    public static List<String> extractValues(String command) {
        int valuesStart = command.indexOf("(");
        int valuesEnd = command.lastIndexOf(")");
        if (valuesStart == -1 || valuesEnd == -1 || valuesEnd <= valuesStart) {
            return new ArrayList<>();
        }
        String valuesPart = command.substring(valuesStart + 1, valuesEnd).trim();
        String[] valuesArray = valuesPart.split(",");
        List<String> values = new ArrayList<>();
        for (String value : valuesArray) {
            values.add(value.trim().replaceAll("^['\"]|['\"]$", ""));
        }
        return values;
    }

    /**
     * Parse table name from SELECT statement
     * @param command SQL command string
     * @return Table name
     */
    public static String extractTableName(String command) {
        String[] tokens = command.split("\\s+");
        for (int i = 0; i < tokens.length - 1; i++) {
            if (tokens[i].equalsIgnoreCase("FROM") || tokens[i].equalsIgnoreCase("INTO") || tokens[i].equalsIgnoreCase("TABLE")) {
                String tableName = tokens[i + 1].replaceAll(";", "").trim();
                if (!tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
                    return null;
                }
                return tableName.toLowerCase();
            }
        }
        return null;
    }

    /**
     * Parse conditions from WHERE clause
     * @param command SQL command string
     * @return Array of conditions (column name, operator, value)
     */
    public static String[] extractWhereCondition(String command) {
        if (!command.contains("WHERE")) return new String[0];

        String[] tokens = command.split("\\s+");
        int whereIndex = -1;
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equalsIgnoreCase("WHERE")) {
                whereIndex = i;
                break;
            }
        }
        if (whereIndex == -1 || tokens.length < whereIndex + 4) return new String[0];

        String column = tokens[whereIndex + 1]
                .replaceAll(";", "")
                .replaceAll("^[^a-zA-Z0-9_]+", "")
                .replaceAll("[^a-zA-Z0-9_]+$", "")
                .trim();
        String operator = tokens[whereIndex + 2];
        String value = tokens[whereIndex + 3].replaceAll(";", "").replaceAll("^['\"]|['\"]$", "").trim();
        return new String[]{column, operator, value};
    }

    /**
     * Parse table names from JOIN statement
     * @param command SQL command string
     * @return [table1, table2, column1, column2]
     */
    public static String[] extractJoinComponents(String command) {
        String[] tokens = command.split("\\s+");
        if (tokens.length < 6 || !tokens[2].equalsIgnoreCase("AND") || !tokens[4].equalsIgnoreCase("ON")) {
            return new String[0];
        }

        String table1 = tokens[1].toLowerCase();
        String table2 = tokens[3].toLowerCase();
        String column1 = tokens[5].toLowerCase();
        String column2 = tokens[7].replace(";", "").toLowerCase();

        return new String[]{table1, table2, column1, column2};
    }

    /**
     * Extract column names or values from a specific index
     * @param tokens Parsed SQL command array
     * @param startIndex Starting index
     * @return Extracted array
     */
    private static String[] extractValuesFromIndex(String[] tokens, int startIndex) {
        List<String> values = new ArrayList<>();
        for (int i = startIndex; i < tokens.length; i++) {
            if (!tokens[i].equalsIgnoreCase("VALUES")) {
                values.add(tokens[i].replaceAll(";", ""));
            }
        }
        return values.toArray(new String[0]);
    }

    public static boolean isValidSelectQuery(String query) {
        String regex = "(?i)^SELECT\\s+.+\\s+FROM\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*(WHERE\\s+.+)?;$";
        boolean matches = query.matches(regex);

        if (!matches) {
        }

        return matches;
    }

    /**
     * Parse column names from SELECT statement
     * @param command SQL command string
     * @return List of parsed column names
     */
    public static List<String> extractSelectColumns(String command) {
        List<String> columns = new ArrayList<>();

        int selectIndex = command.toUpperCase().indexOf("SELECT");
        int fromIndex = command.toUpperCase().indexOf("FROM");

        if (selectIndex == -1 || fromIndex == -1 || fromIndex <= selectIndex) {
            return columns;
        }
        String columnsPart = command.substring(selectIndex + 6, fromIndex).trim();
        if (columnsPart.equals("*")) {
            columns.add("*");
        } else {
            String[] splitColumns = columnsPart.split("\\s*,\\s*");
            for (String col : splitColumns) {
                columns.add(col.trim());
            }
        }
        return columns;
    }
}

