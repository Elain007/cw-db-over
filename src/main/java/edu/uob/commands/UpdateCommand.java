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

    /**
     * 扩展后的 UPDATE 命令执行函数，支持形如
     * “UPDATE tableName SET col1 = newValue1, col2 = newValue2 WHERE conditionColumn == 'someValue';”
     * 的更新语句
     * @param tokens 命令拆分后的字符串数组
     * @return 执行结果字符串
     */
    public String execute(String[] tokens) {
        if (tokens.length < 6) {
            System.out.println("[DEBUG] UPDATE syntax invalid: tokens length " + tokens.length);
            return "[ERROR] Invalid UPDATE syntax";
        }

        // tokens 格式：UPDATE tableName SET ... [WHERE conditionColumn operator conditionValue]
        String tableName = tokens[1];
        // 获取数据库目录（假设 Database 类提供 getCurrentDatabasePath() 方法）
        File databasePath = database.getCurrentDatabasePath();
        File tableFile = new File(databasePath, tableName + ".tab");
        if (!tableFile.exists()) {
            System.out.println("[DEBUG] Table file for '" + tableName + "' does not exist at " + tableFile.getAbsolutePath());
            return "[ERROR] Table does not exist";
        }

        // 读取表文件内容
        List<String> lines;
        try {
            lines = Files.readAllLines(tableFile.toPath());
        } catch (IOException e) {
            System.out.println("[DEBUG] IOException reading table file: " + e.getMessage());
            return "[ERROR] Failed to read table";
        }
        if (lines.isEmpty()) {
            System.out.println("[DEBUG] Table file is empty");
            return "[ERROR] Table is empty";
        }

        // 解析表头
        String headerLine = lines.get(0);
        String[] headerColumns = headerLine.split("\t");
        List<String> headerList = new ArrayList<>();
        for (String header : headerColumns) {
            headerList.add(header.trim());
        }

        // 检查 SET 关键字是否正确
        if (!tokens[2].equalsIgnoreCase("SET")) {
            System.out.println("[DEBUG] Missing SET keyword in UPDATE command");
            return "[ERROR] Invalid UPDATE syntax: missing SET";
        }

        // 查找 WHERE 关键字的位置（如果存在）
        int whereIndex = -1;
        for (int i = 3; i < tokens.length; i++) {
            if (tokens[i].equalsIgnoreCase("WHERE")) {
                whereIndex = i;
                break;
            }
        }

        // 解析 SET 子句：从 tokens[3] 到 whereIndex（或 tokens.length）
        int endIndex = (whereIndex == -1 ? tokens.length : whereIndex);
        StringBuilder setClauseBuilder = new StringBuilder();
        for (int i = 3; i < endIndex; i++) {
            setClauseBuilder.append(tokens[i]).append(" ");
        }
        String setClause = setClauseBuilder.toString().trim();
        // 拆分多个赋值（假设使用逗号分隔）
        String[] assignments = setClause.split(",");
        Map<String, String> updates = new HashMap<>();
        for (String assignment : assignments) {
            String[] parts = assignment.split("=");
            if (parts.length != 2) {
                System.out.println("[DEBUG] Invalid assignment syntax: " + assignment);
                return "[ERROR] Invalid SET syntax";
            }
            String col = parts[0].trim();
            String newValue = parts[1].trim();
            // 清除 newValue 中除数字、字母、下划线之外的所有字符
            newValue = newValue.replaceAll("[^a-zA-Z0-9_]", "");
            System.out.println("[DEBUG] Update assignment: column='" + col + "', newValue='" + newValue + "'");
            if (col.equalsIgnoreCase("id")) {
                System.out.println("[DEBUG] Attempt to update primary key 'id' detected");
                return "[ERROR] Cannot update primary key";
            }
            if (!headerList.contains(col)) {
                System.out.println("[DEBUG] Column '" + col + "' not found in header: " + headerList);
                return "[ERROR] Column '" + col + "' does not exist";
            }
            updates.put(col, newValue);
        }

        // 解析 WHERE 条件（如果存在）
        String conditionColumn = null;
        String whereOperator = null;
        String conditionValue = null;
        boolean hasWhereClause = (whereIndex != -1);
        if (hasWhereClause) {
            // 期望 WHERE 后面有三个部分：条件列、操作符和条件值
            if (tokens.length < whereIndex + 4) {
                System.out.println("[DEBUG] Invalid WHERE syntax: tokens length " + tokens.length + ", expected at least " + (whereIndex + 4));
                return "[ERROR] Invalid WHERE syntax";
            }
            conditionColumn = tokens[whereIndex + 1];
            whereOperator = tokens[whereIndex + 2];
            conditionValue = tokens[whereIndex + 3];
            System.out.println("[DEBUG] Parsed WHERE condition: column='" + conditionColumn +
                    "', operator='" + whereOperator + "', raw value='" + conditionValue + "'");
            if ((conditionValue.startsWith("'") && conditionValue.endsWith("'")) ||
                    (conditionValue.startsWith("\"") && conditionValue.endsWith("\""))) {
                conditionValue = conditionValue.substring(1, conditionValue.length() - 1);
                System.out.println("[DEBUG] Cleaned WHERE condition value: '" + conditionValue + "'");
            }

            // 清除 conditionValue 中除数字、字母、下划线之外的所有字符
            conditionValue = conditionValue.replaceAll("[^a-zA-Z0-9_]", "");
            System.out.println("[DEBUG] Cleaned WHERE condition value: '" + conditionValue + "'");

            if (!headerList.contains(conditionColumn)) {
                System.out.println("[DEBUG] WHERE column '" + conditionColumn + "' not found in header: " + headerList);
                return "[ERROR] Column '" + conditionColumn + "' does not exist";
            }
        }

        // 遍历每一行数据，根据 WHERE 条件判断后执行更新
        List<String> newLines = new ArrayList<>();
        newLines.add(headerLine); // 表头保持不变
        for (int i = 1; i < lines.size(); i++) {
            String rowLine = lines.get(i);
            String[] rowValues = rowLine.split("\t", -1);  // 保留空值
            for(String value : rowValues) {
                System.out.println(value + "123");
            }
            boolean rowMatches = true;
            if (hasWhereClause) {
                int condIndex = headerList.indexOf(conditionColumn);
                if (condIndex < rowValues.length) {
                    String actualValue = rowValues[condIndex].trim();
                    System.out.println("[DEBUG] Row " + i + " actual value for column '" + conditionColumn + "': '" + actualValue + "'");
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
                            System.out.println("[DEBUG] Unsupported operator in WHERE clause: " + whereOperator);
                            return "[ERROR] Unsupported operator in WHERE clause";
                    }
                } else {
                    rowMatches = false;
                }
            }
            // 如果满足条件，则更新指定的列
            if (rowMatches) {
                for (Map.Entry<String, String> entry : updates.entrySet()) {
                    int colIndex = headerList.indexOf(entry.getKey());
                    if (colIndex < rowValues.length) {
                        rowValues[colIndex] = entry.getValue();
                        System.out.println("[DEBUG] Row " + i + " updated column '" + entry.getKey() +
                                "' to '" + entry.getValue() + "'");
                    }
                }
            }
            String updatedRow = String.join("\t", rowValues);
            newLines.add(updatedRow);
        }

        // 写回更新后的数据到表文件
        try {
            Files.write(tableFile.toPath(), newLines);
            System.out.println("[DEBUG] Successfully wrote updated data to table file: " + tableFile.getAbsolutePath());
            return "[OK] Update successful";
        } catch (IOException e) {
            System.out.println("[DEBUG] IOException writing table file: " + e.getMessage());
            return "[ERROR] Failed to write table";
        }
    }
}


