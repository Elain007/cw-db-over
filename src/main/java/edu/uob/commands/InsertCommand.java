package edu.uob.commands;

import edu.uob.model.Database;
//import edu.uob.parser.QueryParser;

import java.util.List;
import java.util.ArrayList;


public class InsertCommand {
    private final Database database;

    public InsertCommand(Database database) {
        this.database = database;
    }

    public String execute(String[] tokens) {
        //System.out.println("[DEBUG] InsertCommand tokens: " + Arrays.toString(tokens));
        //if (tokens.length < 4 || !tokens[3].equalsIgnoreCase("VALUES")) {
            //System.out.println("[DEBUG] Invalid INSERT syntax: " + Arrays.toString(tokens));
            //return "[ERROR] Invalid INSERT syntax";
        //}


        // 将 tokens 拼接成完整命令字符串，去掉结尾的分号
        String fullCommand = String.join(" ", tokens).replace(";", "").trim();

        // 检查完整命令中是否包含 "VALUES"（不区分大小写）
        if (fullCommand.toUpperCase().indexOf("VALUES") == -1) {
            return "[ERROR] Invalid INSERT syntax";
        }


        // 清理表名，去掉非字母数字字符
        String tableName = tokens[2].replaceAll("[^a-zA-Z0-9_]", "").toLowerCase().trim();

        // 解析插入值，去掉 `;`
        //String rawValues = String.join(" ", tokens).replace(";", "").trim();
        //List<String> values = QueryParser.extractValues(rawValues);


        // 使用自定义方法提取括号内的值，确保字符串中的特殊字符能被正确解析
        List<String> values = extractValues(fullCommand);

        if (values.isEmpty()) {
            return "[ERROR] Invalid INSERT syntax";
        }

        // 调试信息
        //System.out.println("[DEBUG] Insert into table: " + tableName);
        //System.out.println("[DEBUG] Raw insert string: " + rawValues);
        //System.out.println("[DEBUG] Parsed values: " + values);

        return database.insertIntoTable(tableName, values);
    }


    /**
     * 从完整的 INSERT 命令字符串中提取括号内的各个值，支持单引号内包含任意特殊字符。
     * 例如：INSERT INTO Marks VALUES('%.{$abc}abc', 65.1, TRUE)
     * 将提取出 ["'%.{$abc}abc'", "65.1", "TRUE"]
     */
    private List<String> extractValues(String raw) {
        List<String> result = new ArrayList<>();
        int start = raw.indexOf("(");
        int end = raw.lastIndexOf(")");
        if (start == -1 || end == -1 || end <= start) {
            return result;  // 返回空列表表示解析失败
        }
        String valuesSubstring = raw.substring(start + 1, end);
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        // 遍历整个值字符串，逗号作为分隔符，但如果在引号中则忽略逗号
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

    /**
     * 如果值以单引号包围，则去除它们。
     */
    private String processValue(String value) {
        return value.replace("'", "");
    }

}

