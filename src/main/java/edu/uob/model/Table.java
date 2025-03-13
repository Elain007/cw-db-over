package edu.uob.model;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Table {
    private final String name;
    private final List<String> columns;
    private final List<Record> records;
    private final File file;

    public Table(List<String> columns, List<Record> records) {
        this.columns = columns;
        this.records = records;
        this.name = null;
        this.file = null;
    }

    public Table(String tableName, File file) throws IOException {
        this.name = tableName;
        this.file = file;
        this.columns = new ArrayList<>();
        this.records = new ArrayList<>();
        loadFromFile();
    }

    private void loadFromFile() throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        if (!lines.isEmpty()) {
            columns.addAll(List.of(lines.get(0).split("\t")));
            for (String line : lines.subList(1, lines.size())) {
                records.add(new Record(List.of(line.split("\t"))));
            }
        }
    }

    public void addRecord(Record record) throws IOException {
        records.add(record);
        saveToFile();
    }

    public void removeRecord(Record record) throws IOException {
        records.remove(record);
        saveToFile();
    }

    private void saveToFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(String.join("\t", columns));
            writer.newLine();
            for (Record record : records) {
                writer.write(record.toTSV());
                writer.newLine();
            }
        }
    }

    public List<Record> getRecords() {
        return records;
    }

    public List<String> getColumns() {
        return columns;
    }

    public String getName() {
        return name;
    }

    public void deleteColumn(String column) {
        int deletedIndex = -1;
        for (int idx=0; idx<columns.size(); idx++) {
            if (columns.get(idx).equalsIgnoreCase(column)) deletedIndex = idx;
        }
        if (deletedIndex != -1) {
            columns.remove(deletedIndex);
            for (Record record : records) {
                record.deleteValue(deletedIndex);
            }
        }
    }

    public String getTableString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.join("\t", columns));
        stringBuilder.append(System.lineSeparator());

        for (Record record : records) {
            stringBuilder.append(record.toTSV());
            stringBuilder.append(System.lineSeparator());
        }
        return stringBuilder.toString();
    }
}

