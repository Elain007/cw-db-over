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

    /**
     * 扩展后的 DELETE 命令执行函数，支持形如
     * “DELETE FROM tableName WHERE conditionColumn == 'someValue';”
     * 的删除语句
     * @param tokens 命令拆分后的字符串数组
     * @return 执行结果字符串
     */
    public String execute(String[] tokens) {
        if (tokens.length < 5) {
            System.out.println("[DEBUG] DELETE syntax invalid: tokens length " + tokens.length);
            return "[ERROR] Invalid DELETE syntax";
        }

        // 支持两种 DELETE 语法：
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

        // 检查 WHERE 关键字是否存在
        if (tokens.length <= whereStartIndex || !tokens[whereStartIndex].equalsIgnoreCase("WHERE")) {
            System.out.println("[DEBUG] DELETE command missing WHERE clause");
            return "[ERROR] DELETE command must contain a WHERE clause";
        }

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

        // 解析 WHERE 条件：期望 WHERE 后面有三个部分：条件列、操作符和条件值
        if (tokens.length < whereStartIndex + 4) {
            System.out.println("[DEBUG] Invalid WHERE syntax in DELETE: tokens length " + tokens.length + ", expected at least " + (whereStartIndex + 4));
            return "[ERROR] Invalid WHERE syntax";
        }
        String conditionColumn = tokens[whereStartIndex + 1];
        String whereOperator = tokens[whereStartIndex + 2];
        String conditionValue = tokens[whereStartIndex + 3];
        System.out.println("[DEBUG] Parsed DELETE WHERE condition: column='" + conditionColumn +
                "', operator='" + whereOperator + "', raw value='" + conditionValue + "'");

        if ((conditionValue.startsWith("'") && conditionValue.endsWith("'")) ||
                (conditionValue.startsWith("\"") && conditionValue.endsWith("\""))) {
            conditionValue = conditionValue.substring(1, conditionValue.length() - 1);
            System.out.println("[DEBUG] Cleaned DELETE WHERE condition value: '" + conditionValue + "'");
        }

        // 清除 conditionValue 中除数字、字母、下划线之外的所有字符
        conditionValue = conditionValue.replaceAll("[^a-zA-Z0-9_]", "");
        System.out.println("[DEBUG] Cleaned DELETE WHERE condition value: '" + conditionValue + "'");

        if (!headerList.contains(conditionColumn)) {
            System.out.println("[DEBUG] DELETE WHERE column '" + conditionColumn + "' not found in header: " + headerList);
            return "[ERROR] Column '" + conditionColumn + "' does not exist";
        }

        // 遍历数据行，删除满足 WHERE 条件的行
        List<String> newLines = new ArrayList<>();
        newLines.add(headerLine); // 保留表头
        for (int i = 1; i < lines.size(); i++) {
            String rowLine = lines.get(i);
            String[] rowValues = rowLine.split("\t", -1);
            boolean rowMatches = true;
            int condIndex = headerList.indexOf(conditionColumn);
            if (condIndex < rowValues.length) {
                String actualValue = rowValues[condIndex].trim();
                System.out.println("[DEBUG] Row " + i + " actual value for DELETE, column '" + conditionColumn + "': '" + actualValue + "'");
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
                        System.out.println("[DEBUG] DELETE unsupported operator: " + whereOperator);
                        return "[ERROR] Unsupported operator in WHERE clause";
                }
            } else {
                rowMatches = false;
            }
            // 如果行不满足删除条件，则保留该行；否则不添加（即删除）
            if (!rowMatches) {
                newLines.add(rowLine);
            }else {
                System.out.println("[DEBUG] Row " + i + " matched DELETE condition and will be removed");
            }
        }

        // 写回更新后的数据到表文件
        try {
            Files.write(tableFile.toPath(), newLines);
            System.out.println("[DEBUG] Successfully wrote updated data to table file: " + tableFile.getAbsolutePath());
            return "[OK] Delete successful";
        } catch (IOException e) {
            System.out.println("[DEBUG] IOException writing table file: " + e.getMessage());
            return "[ERROR] Failed to write table";
        }
    }
}

