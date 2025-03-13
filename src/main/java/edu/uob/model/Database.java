package edu.uob.model;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.io.File;

public class Database {
    private File rootPath;  // 修改为 File 类型
    private File currentDatabasePath;
    private String currentDatabase = null;

    public Database(String rootDirectory) {
        this.rootPath = new File(rootDirectory);
        if (!rootPath.exists()) {
            rootPath.mkdirs();
        }
        // System.out.println("[DEBUG] Database root set to: " + rootPath.getAbsolutePath());
    }

    public String createDatabase(String dbName) {
        File databaseDir = new File(rootPath, dbName.toLowerCase().trim());
        //System.out.println("[DEBUG] Creating database at: " + databaseDir.getAbsolutePath());

        if (databaseDir.exists()) {
            return "[ERROR] Database already exists";
        }

        if (databaseDir.mkdirs()) {
            //System.out.println("[OK] Database created at: " + databaseDir.getAbsolutePath());
            String switchResult = useDatabase(dbName);  // 创建后自动切换
            //System.out.println("[DEBUG] After creation, current database path: " +
                  //(currentDatabasePath != null ? currentDatabasePath.getAbsolutePath() : "null"));
            return switchResult;
        }
        return "[ERROR] Failed to create database";
    }

    public String useDatabase(String dbName) {
        System.out.println(dbName);
        File databaseDir = new File(rootPath, dbName.toLowerCase().trim());
        //System.out.println("[DEBUG] Trying to switch to: " + databaseDir.getAbsolutePath());

        if (!databaseDir.exists() || !databaseDir.isDirectory()) {
            //System.out.println("[DEBUG] Database does not exist: " + databaseDir.getAbsolutePath());
            return "[ERROR] Database does not exist";
        }

        this.currentDatabase = dbName.toLowerCase().trim();
        this.currentDatabasePath = databaseDir;
        //System.out.println("[OK] Successfully switched to database: " + currentDatabasePath.getAbsolutePath());
        //System.out.println("[DEBUG] Current database path after switch: " +
                //(currentDatabasePath != null ? currentDatabasePath.getAbsolutePath() : "null"));

        return "[OK] Switched to database: " + dbName;
    }

    public void setCurrentDatabasePath(String path) {
        this.currentDatabasePath = new File(path);
    }

    // 创建表：自动在表头最前面添加 "id"
    public String createTable(String tableName, List<String> columns) {
        if (this.currentDatabasePath == null) {
            return "[ERROR] No database selected: Database.createTable";
        }
        // 对 tableName 进行清洗：去除非法字符、转换为小写、trim
        String cleanedTableName = tableName.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase().trim();
        File tableFile = new File(this.currentDatabasePath, cleanedTableName + ".tab");
        System.out.println(tableFile.toString() + "123456");

        // 如果文件已存在，则认为表已经存在（即使输入了不同大小写组合也会映射到同一个文件）
        if (tableFile.exists()) {
            return "[ERROR] Table already exists";
        }

        // 自动在表头添加 id 列
        List<String> newHeaderColumns = new ArrayList<>();
        newHeaderColumns.add("id");
        // 遍历用户提供的列（去除多余分号和空格）
        for (String col : columns) {
            newHeaderColumns.add(col.trim().replaceAll(";", ""));
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile))) {
            writer.write(String.join("\t", newHeaderColumns));
        } catch (IOException e) {
            return "[ERROR] Failed to create table";
        }

        return "[OK] Table created";
    }


    public File getCurrentDatabasePath() {
        return this.currentDatabasePath;
    }

    // **补充：删除表**
    public String dropTable(String tableName) {
        if (currentDatabasePath == null) {
            return "[ERROR] No database selected: Database.dropTable1";
        }
        File tableFile = new File(currentDatabasePath, tableName.toLowerCase().trim().replace(";", "") + ".tab");
        if (!tableFile.exists()) {
            return "[ERROR] Table does not exist";
        }
        if (tableFile.delete()) {
            return "[OK] Table dropped";
        } else {
            return "[ERROR] Failed to drop table";
        }
    }

    // **补充：删除列**

    public String alterTableDropColumn(String tableName, String columnName) throws IOException {
        if (currentDatabasePath == null) {
            return "[ERROR] No database selected: Database.alterTableDropColumn";
        }
        File tableFile = new File(currentDatabasePath, tableName.toLowerCase().trim() + ".tab");
        if (!tableFile.exists()) {
            return "[ERROR] Table does not exist";
        }

        List<String> lines = Files.readAllLines(tableFile.toPath());
        if (lines.isEmpty()) {
            return "[ERROR] Table is empty";
        }

        // 读取原始表头
        String headerLine = lines.get(0);
        System.out.println("[DEBUG] Original table header: " + headerLine);

        // 拆分表头列并对每个列名进行 trim 处理
        String[] columns = headerLine.split("\t");
        List<String> trimmedColumns = new ArrayList<>();
        for (int i = 0; i < columns.length; i++) {
            String trimmed = columns[i].trim();
            trimmedColumns.add(trimmed);
            System.out.println("[DEBUG] Processed Header Column " + i + ": '" + trimmed + "'");
        }
        System.out.println("[DEBUG] Full processed header: " + trimmedColumns);

        // 对比时也要对传入的 columnName 进行 trim
        String targetColumn = columnName.trim();
        System.out.println("[DEBUG] Column to drop (target): '" + targetColumn + "'");

        int columnIndex = -1;
        for (int i = 0; i < trimmedColumns.size(); i++) {
            if (trimmedColumns.get(i).equalsIgnoreCase(targetColumn)) {
                columnIndex = i;
                System.out.println("[DEBUG] Found column '" + targetColumn + "' at index: " + i);
                break;
            }
        }
        if (columnIndex == -1) {
            return "[ERROR] Column does not exist";
        }

        // 检查是否尝试删除主键 "id"
        if (targetColumn.equalsIgnoreCase("id")) {
            return "[ERROR] Cannot drop primary key";
        }
        // 构造新的表头，不包含被删除的列
        StringBuilder newHeader = new StringBuilder();
        for (int i = 0; i < trimmedColumns.size(); i++) {
            if (i == columnIndex) continue;  // 跳过要删除的列
            if (newHeader.length() > 0) {
                newHeader.append("\t");
            }
            newHeader.append(trimmedColumns.get(i));
        }
        System.out.println("[DEBUG] New table header after dropping column: " + newHeader.toString());

        // 构造新内容：第一行为新的表头，其余每行删除对应列的数据
        List<String> newLines = new ArrayList<>();
        newLines.add(newHeader.toString());
        for (int j = 1; j < lines.size(); j++) {
            String line = lines.get(j);
            String[] values = line.split("\t");
            StringBuilder newLine = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (i == columnIndex) continue;
                if (newLine.length() > 0) {
                    newLine.append("\t");
                }
                newLine.append(values[i].trim());
            }
            System.out.println("[DEBUG] New row " + j + ": " + newLine.toString());
            newLines.add(newLine.toString());
        }

        // 将更新后的内容写回文件
        Files.write(tableFile.toPath(), newLines);

        // 重新读取文件以确认新表头是否写入成功
        List<String> checkLines = Files.readAllLines(tableFile.toPath());
        if (!checkLines.isEmpty()) {
            System.out.println("[DEBUG] Verified new table header from file: " + checkLines.get(0));
        } else {
            System.out.println("[DEBUG] Error: Table file is empty after writing new header.");
        }

        return "[OK] Column dropped";
    }



    // **补充：合并表**
    public String joinTables(String table1, String table2, String column1, String column2) throws IOException {
        if (currentDatabasePath == null) {
            //System.out.println("[DEBUG] No database selected in selectFromTable()");
            return "[ERROR] No database selected: Database.selectFromTable";
        }
        File table1File = new File(currentDatabasePath, table1.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase().trim() + ".tab");
        File table2File = new File(currentDatabasePath, table2.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase().trim() + ".tab");
        List<String> table1Lines = Files.readAllLines(table1File.toPath());
        List<String> table2Lines = Files.readAllLines(table2File.toPath());
        List<String> table1Columns = List.of(table1Lines.get(0).split("\t"));
        List<String> table2Columns = List.of(table2Lines.get(0).split("\t"));
        table1Lines.remove(0);
        table2Lines.remove(0);
        int column1Index = table1Columns.indexOf(column1);
        int column2Index = table2Columns.indexOf(column2);
        int newId = 0;
        List<Record> newRecords = new ArrayList<>();
        for (String table1Line : table1Lines) {
            List<String> line1Values = List.of(table1Line.split("\t"));
            for (String table2Line : table2Lines) {
                List<String> line2Values = List.of(table2Line.split("\t"));
                if (line1Values.get(column1Index).equals(line2Values.get(column2Index))) {
                    List<String> newRow = new ArrayList<>();
                    newRow.add(String.valueOf(++newId));
                    newRow.addAll(line1Values);
                    newRow.addAll(line2Values);
                    newRecords.add(new Record(newRow));
                }
            }
        }
        List<String> newColumns = new ArrayList<>();
        newColumns.add("id");
        for (String table1Column : table1Columns) {
            newColumns.add(table1 + "." + table1Column);
        }
        for (String table2Column : table2Columns) {
            newColumns.add(table2 + "." + table2Column);
        }
        Table joinedTable = new Table(newColumns, newRecords);
        List<String> deletedColumns = new ArrayList<>();
        deletedColumns.add(table1 + "." + column1);
        deletedColumns.add(table2 + "." + column2);
        deletedColumns.add(table1 + ".id");
        deletedColumns.add(table2 + ".id");
        for (String column : deletedColumns) {
            joinedTable.deleteColumn(column);
        }

        return "[OK]" + System.lineSeparator() + joinedTable.getTableString();
    }

    // **补充：查询表**
    public String selectFromTable(String tableName) {
        if (currentDatabasePath == null) {
            //System.out.println("[DEBUG] No database selected in selectFromTable()");
            return "[ERROR] No database selected: Database.selectFromTable";
        }
        // 处理表名，去掉非法字符
        File tableFile = new File(currentDatabasePath, tableName.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase().trim() + ".tab");

        //System.out.println("[DEBUG] Attempting to retrieve data from table: " + tableFile.getAbsolutePath());

        if (!tableFile.exists()) {
            //System.out.println("[DEBUG] Table does not exist: " + tableFile.getAbsolutePath());
            return "[ERROR] Table does not exist";
        }

        try {
            List<String> lines = Files.readAllLines(tableFile.toPath());
            if (lines.isEmpty()) {
                return "[ERROR] Table is empty";
            }
            String data = String.join(System.lineSeparator(), lines);
            return "[OK] " + data;
        } catch (IOException e) {
            return "[ERROR] Failed to read table";
        }
    }

    // **补充：插入数据**
    // 插入数据：自动生成 id 值（新行 id 为当前数据行数，即 header 行算作第1行）
    public String insertIntoTable(String tableName, List<String> values) {
        if (currentDatabasePath == null) {
            //System.out.println("[DEBUG] No database selected in insertIntoTable()");
            return "[ERROR] No database selected: Database: insertIntoTable";
        }
        String cleanedTableName = tableName.toLowerCase().trim();
        File tableFile = new File(currentDatabasePath, cleanedTableName + ".tab");
        //System.out.println("[DEBUG] Inserting into table file: " + tableFile.getAbsolutePath());
        //System.out.println("[DEBUG] Original insert values: " + values);

        if (!tableFile.exists()) {
            //System.out.println("[DEBUG] Table file does not exist: " + tableFile.getAbsolutePath());
            return "[ERROR] Table does not exist";
        }

        try {
            // 读取当前行数，以计算下一个 id
            List<String> lines = Files.readAllLines(tableFile.toPath());
            int nextId = lines.size(); // header占一行，所以第一条数据 id 为 1
            // 构造新行：id + 原始值（每个值不作额外修改）
            List<String> newRow = new ArrayList<>();
            newRow.add(String.valueOf(nextId));
            newRow.addAll(values);
            //System.out.println("[DEBUG] New row to insert (with id): " + newRow);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, true))) {
                writer.newLine();
                writer.write(String.join("\t", newRow));
            }
            //System.out.println("[DEBUG] Insert successful into table file: " + tableFile.getAbsolutePath());
            return "[OK] Insert successful";
        } catch (IOException e) {
            //System.out.println("[DEBUG] Insert failed: " + e.getMessage());
            return "[ERROR] Insert failed";
        }
    }

    // **检查数据库是否存在**
    public boolean databaseExists(String dbName) {
        File databaseDir = new File(rootPath, dbName.toLowerCase());
        return databaseDir.exists();
    }

    // **创建数据库**

    // **删除数据库**
    public String dropDatabase(String dbName) {
        File databaseDir = new File(rootPath, dbName.toLowerCase().trim().replace(";", ""));
        if (!databaseDir.exists()) {
            return "[ERROR] Database does not exist";
        }
        if (deleteDirectory(databaseDir)) {
            return "[OK] Database dropped";
        }
        return "[ERROR] Failed to drop database";
    }

    // **递归删除文件夹**
    private boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                deleteDirectory(file);
            }
        }
        return dir.delete();
    }


    public String getCurrentDatabase() {
        return currentDatabase;
    }

    // 在 Database.java 中，其他方法之后新增：
// **补充：添加列**
    public String alterTableAddColumn(String tableName, String columnName) {
        if (currentDatabasePath == null) {
            return "[ERROR] No database selected: Database.alterTableAddColumn";
        }
        File tableFile = new File(currentDatabasePath, tableName.toLowerCase().trim() + ".tab");
        if (!tableFile.exists()) {
            return "[ERROR] Table does not exist";
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(tableFile.toPath());
        } catch (IOException e) {
            return "[ERROR] Failed to read table";
        }
        if (lines.isEmpty()) {
            return "[ERROR] Table is empty";
        }
        // 获取现有表头
        String headerLine = lines.get(0);
        String[] columns = headerLine.split("\t");
        for (String col : columns) {
            if (col.equalsIgnoreCase(columnName.trim())) {
                return "[ERROR] Duplicate column name: " + columnName;
            }
        }
        // 构造新的表头：在原有表头后追加新列（统一转为小写）
        String newHeader = headerLine + "\t" + columnName.trim().toLowerCase();
        List<String> newLines = new ArrayList<>();
        newLines.add(newHeader);
        // 对于每一行数据，追加一个空字段
        for (int i = 1; i < lines.size(); i++) {
            newLines.add(lines.get(i) + "\t ");
        }
        try {
            Files.write(tableFile.toPath(), newLines);
        } catch (IOException e) {
            return "[ERROR] Failed to alter table";
        }
        return "[OK] Column added";
    }

}




