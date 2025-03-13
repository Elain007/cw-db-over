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

    /**
     * 扩展后的 SELECT 命令执行函数，支持形如
     * “SELECT id FROM marks WHERE name == 'Simon';” 或
     * “SELECT * FROM marks WHERE (pass == FALSE) AND (mark > 35);”
     * 的查询语句
     * @param command 完整的 SQL 查询语句
     * @param databasePath 当前数据库的路径
     * @return 执行结果字符串
     */
    public String execute(String command, File databasePath) {
        System.out.println("[DEBUG] Executing SELECT command: " + command);

        // 解析表名
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

            // 获取并处理表头
            String headerLine = lines.get(0);
            System.out.println("[DEBUG] Retrieved table header: " + headerLine);
            String[] headerColumns = headerLine.split("\t");
            List<String> headerList = new ArrayList<>();
            for (String header : headerColumns) {
                headerList.add(header.trim());
            }
            System.out.println("[DEBUG] Processed header columns: " + headerList);

            // 解析 SELECT 语句中的列名部分
            List<String> selectedColumns = QueryParser.extractSelectColumns(command);
            System.out.println("[DEBUG] Selected columns from query: " + selectedColumns);
            if (selectedColumns.isEmpty()) {
                System.out.println("[DEBUG] No columns specified in SELECT command.");
                return "[ERROR] Invalid SELECT syntax";
            }

            // 根据所选列确定返回数据的列索引
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

            // 解析 WHERE 子句（如果存在）
            int wherePos = command.toUpperCase().indexOf("WHERE");
            boolean hasWhereClause = wherePos != -1;
            // conditions：每个条件为 String[3] {column, operator, value}
            List<String[]> conditions = new ArrayList<>();
            if (hasWhereClause) {
                // 取出 WHERE 后的内容，并去除尾部分号
                String whereClause = command.substring(wherePos + 5).trim();
                if (whereClause.endsWith(";")) {
                    whereClause = whereClause.substring(0, whereClause.length() - 1).trim();
                }
                // 判断是否为复合条件：若包含顶层 AND，则使用解析算法；否则视为单个条件
                boolean isComposite = whereClause.toUpperCase().contains("AND");
                List<String> condStrings = new ArrayList<>();
                if (isComposite) {
                    // 去除外围括号（若整段被一对括号包围且平衡则去除）
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
                    // 使用基于嵌套计数的方法按顶层 AND 拆分条件
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
                            // 当处于顶层（parenCount==0）时，检测是否遇到 "AND"
                            if (parenCount == 0 && i + 2 < whereClause.length() &&
                                    whereClause.substring(i, i + 3).equalsIgnoreCase("AND")) {
                                condStrings.add(sb.toString().trim());
                                sb.setLength(0);
                                i += 2; // 跳过 "AND"
                            } else {
                                sb.append(c);
                            }
                        }
                    }
                    if (sb.length() > 0) {
                        condStrings.add(sb.toString().trim());
                    }
                } else {
                    // 单个条件
                    condStrings.add(whereClause);
                }
                // 对每个条件进行处理
                for (String condStr : condStrings) {
                    // 再次去除条件外围括号
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
                    // 去除条件值两端引号，并只保留字母、数字、下划线
                    val = val.replaceAll("^[\"']|[\"']$", "");
                    val = val.replaceAll("[^a-zA-Z0-9_]", "");
                    System.out.println("[DEBUG] Parsed condition: '" + col + " " + op + " " + val + "'");
                    conditions.add(new String[]{col, op, val});
                }
                // 检查所有条件的列名是否存在
                for (String[] cond : conditions) {
                    if (!headerList.contains(cond[0])) {
                        System.out.println("[DEBUG] WHERE column '" + cond[0] + "' not found in header.");
                        return "[ERROR] Column " + cond[0] + " does not exist";
                    }
                }
            }

            // 构造返回结果
            List<String> resultRows = new ArrayList<>();
            // 添加表头（返回查询列的名称）
            List<String> returnHeader = new ArrayList<>();
            if (selectedColumns.contains("*")) {
                returnHeader.addAll(headerList);
            } else {
                for (int idx : selectedIndices) {
                    returnHeader.add(headerList.get(idx));
                }
            }
            resultRows.add(String.join("\t", returnHeader));

            // 遍历数据行
            for (int i = 1; i < lines.size(); i++) {
                String rowLine = lines.get(i);
                String[] rowValues = rowLine.split("\t");
                boolean rowMatches = true;
                if (hasWhereClause) {
                    // 对于每个条件，都必须满足（逻辑 AND）
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

