package edu.uob.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Record {
    private final List<String> values;

    public Record(List<String> values) {
        this.values = values;
    }

    public String getValue(int index) {
        return values.get(index);
    }

    public void setValue(int index, String newValue) {
        values.set(index, newValue);
    }

    public List<String> getValues() {
        return values;
    }

    public String toTSV() {
        return values.stream().collect(Collectors.joining("\t"));
    }

    @Override
    public String toString() {
        return toTSV();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Record)) return false;
        Record record = (Record) obj;
        return Objects.equals(values, record.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    public void deleteValue(int index) {
        values.remove(index);
    }
}

