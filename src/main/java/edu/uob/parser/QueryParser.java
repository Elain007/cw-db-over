package edu.uob.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class QueryParser {

    /**
     * 解析 SQL 命令中的列名 (用于 CREATE TABLE)
     * @param command SQL 命令字符串
     * @return 列名数组
     */
    public static List<String> extractColumns(String command) {
        int start = command.indexOf("(");
        int end = command.lastIndexOf(")");
        if (start == -1 || end == -1 || end <= start) {
            return new ArrayList<>(); // 语法错误，返回空列表
        }
        String columnsPart = command.substring(start + 1, end).trim();
        String[] columnsArray = columnsPart.split("\\s*,\\s*"); // 按逗号分割，并去掉前后空格
        return new ArrayList<>(Arrays.asList(columnsArray)); // 转换为 List<String>
    }

    /**
     * 解析 SQL 语句中的值 (用于 INSERT INTO)
     * @param command SQL 命令字符串
     * @return 解析后的值数组
     */
    public static List<String> extractValues(String command) {
        int valuesStart = command.indexOf("(");
        int valuesEnd = command.lastIndexOf(")");
        if (valuesStart == -1 || valuesEnd == -1 || valuesEnd <= valuesStart) {
            return new ArrayList<>(); // 语法错误，返回空列表
        }
        String valuesPart = command.substring(valuesStart + 1, valuesEnd).trim();
        String[] valuesArray = valuesPart.split(",");
        List<String> values = new ArrayList<>();
        for (String value : valuesArray) {
            values.add(value.trim().replaceAll("^['\"]|['\"]$", "")); // 去除首尾引号
        }
        return values;
    }

    /**
     * 解析 SELECT 语句的表名
     * @param command SQL 命令字符串
     * @return 表名
     */
    public static String extractTableName(String command) {
        String[] tokens = command.split("\\s+");
        for (int i = 0; i < tokens.length - 1; i++) {
            if (tokens[i].equalsIgnoreCase("FROM") || tokens[i].equalsIgnoreCase("INTO") || tokens[i].equalsIgnoreCase("TABLE")) {
                String tableName = tokens[i + 1].replaceAll(";", "").trim();
                //System.out.println("[DEBUG] Extracted table name: " + tableName);
                if (!tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
                    //System.out.println("[DEBUG] Invalid table name detected: " + tableName);
                    return null;
                }
                return tableName.toLowerCase();
            }
        }
        //System.out.println("[DEBUG] Table name not found in command: " + command);
        return null;
    }

    /**
     * 解析 WHERE 语句的条件
     * @param command SQL 命令字符串
     * @return 条件数组 (列名, 操作符, 值)
     */
    public static String[] extractWhereCondition(String command) {
        if (!command.contains("WHERE")) return new String[0];

        String[] tokens = command.split("\\s+");
        //System.out.println("[DEBUG] QueryParser tokens for WHERE: " + java.util.Arrays.toString(tokens));
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

        //System.out.println("[DEBUG] Extracted WHERE condition: column='" + column + "', operator='" + operator + "', value='" + value + "'");
        return new String[]{column, operator, value};
    }

    /**
     * 解析 JOIN 语句的表名
     * @param command SQL 命令字符串
     * @return [表1, 表2, 列1, 列2]
     */
    public static String[] extractJoinComponents(String command) {
        String[] tokens = command.split("\\s+");
        if (tokens.length < 6 || !tokens[2].equalsIgnoreCase("AND") || !tokens[4].equalsIgnoreCase("ON")) {
            return new String[0]; // 语法错误
        }

        String table1 = tokens[1].toLowerCase();
        String table2 = tokens[3].toLowerCase();
        String column1 = tokens[5].toLowerCase();
        String column2 = tokens[7].replace(";", "").toLowerCase();

        return new String[]{table1, table2, column1, column2};
    }

    /**
     * 从某个索引开始提取列名或值
     * @param tokens 解析后的 SQL 命令数组
     * @param startIndex 起始索引
     * @return 提取后的数组
     */
    private static String[] extractValuesFromIndex(String[] tokens, int startIndex) {
        List<String> values = new ArrayList<>();
        for (int i = startIndex; i < tokens.length; i++) {
            if (!tokens[i].equalsIgnoreCase("VALUES")) { // 跳过 "VALUES"
                values.add(tokens[i].replaceAll(";", "")); // 移除分号
            }
        }
        return values.toArray(new String[0]);
    }

    public static boolean isValidSelectQuery(String query) {
        String regex = "(?i)^SELECT\\s+.+\\s+FROM\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*(WHERE\\s+.+)?;$";
        boolean matches = query.matches(regex);

        if (!matches) {
            //System.out.println("[DEBUG] Invalid SELECT syntax: " + query);
        }

        return matches;
    }

    /**
     * 解析 SELECT 语句中的列名
     * @param command SQL 命令字符串
     * @return 解析后的列名列表
     */
    public static List<String> extractSelectColumns(String command) {
        List<String> columns = new ArrayList<>();

        // 找到 SELECT 和 FROM 之间的部分
        int selectIndex = command.toUpperCase().indexOf("SELECT");
        int fromIndex = command.toUpperCase().indexOf("FROM");

        if (selectIndex == -1 || fromIndex == -1 || fromIndex <= selectIndex) {
            return columns;  // 语法错误，返回空列表
        }

        // 提取 SELECT 和 FROM 之间的列名部分
        String columnsPart = command.substring(selectIndex + 6, fromIndex).trim();

        if (columnsPart.equals("*")) {
            columns.add("*"); // 选择所有列
        } else {
            String[] splitColumns = columnsPart.split("\\s*,\\s*");
            for (String col : splitColumns) {
                columns.add(col.trim());
            }
        }

        return columns;
    }


}

