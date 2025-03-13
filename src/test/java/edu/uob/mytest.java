package edu.uob;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;

public class mytest {

    private DBServer server;

    @BeforeEach
    public void setup() {
        server = new DBServer();
    }

    // Randomly generate a database name to ensure an isolated testing environment
    private String generateRandomName() {
        String randomName = "";
        for (int i = 0; i < 10; i++) {
            randomName += (char)(97 + (int)(Math.random() * 26));
        }
        return randomName;
    }

    // Send a command to the DBServer with a timeout to prevent infinite loops
    private String sendCommandToServer(String command) {
        return assertTimeoutPreemptively(Duration.ofMillis(1000),
            () -> server.handleCommand(command),
            "Server took too long to respond (probably stuck in an infinite loop)");
    }

    // Test basic database creation, table creation, data insertion, and query functionality
    @Test
    public void testBasicCreateAndQuery() {
        String randomName = generateRandomName();
        String response = sendCommandToServer("CREATE DATABASE " + randomName + ";");
        assertTrue(response.contains("[OK]"), "Database creation should return [OK]");
        response = sendCommandToServer("USE " + randomName + ";");
        assertTrue(response.contains("[OK]"), "Switching database should return [OK]");
        
        response = sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        assertTrue(response.contains("[OK]"), "Table creation should return [OK]");
        
        response = sendCommandToServer("INSERT INTO marks VALUES ('Alice', 30, TRUE);");
        assertTrue(response.contains("[OK]"), "Inserting Alice should return [OK]");
        response = sendCommandToServer("INSERT INTO marks VALUES ('Bob', 25, FALSE);");
        assertTrue(response.contains("[OK]"), "Inserting Bob should return [OK]");
        
        response = sendCommandToServer("SELECT * FROM marks;");
        assertTrue(response.contains("[OK]"), "Query should return [OK]");
        assertFalse(response.contains("[ERROR]"), "Query should not return [ERROR]");
        assertTrue(response.contains("Alice"), "Query result should contain Alice");
        assertTrue(response.contains("Bob"), "Query result should contain Bob");
    }

    // Test that the ID returned in the query is a valid number
    @Test
    public void testQueryID() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Alice', 30, TRUE);");
        String response = sendCommandToServer("SELECT id FROM marks WHERE name == 'Alice';");
        
        // Convert multi-line response into a single line
        String singleLine = response.replace("\n", " ").trim();
        String[] tokens = singleLine.split(" ");
        String lastToken = tokens[tokens.length - 1];
        try {
            Integer.parseInt(lastToken);
        } catch (NumberFormatException nfe) {
            fail("The last token returned by `SELECT id FROM marks WHERE name == 'Alice';` should be an integer, but was: " + lastToken);
        }
    }

    // Test that querying a non-existent table returns an [ERROR] tag and not [OK]
    @Test
    public void testForErrorTag() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        String response = sendCommandToServer("SELECT * FROM nonexisttable;");
        assertTrue(response.contains("[ERROR]"), "Accessing a non-existent table should return [ERROR]");
        assertFalse(response.contains("[OK]"), "Accessing a non-existent table should not return [OK]");
    }

    // Test that data persists after a server restart
    @Test
    public void testTablePersistsAfterRestart() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Alice', 30, TRUE);");
        
        // Simulate a server restart
        server = new DBServer();
        sendCommandToServer("USE " + randomName + ";");
        String response = sendCommandToServer("SELECT * FROM marks;");
        assertTrue(response.contains("Alice"), "After server restart, inserted data should be retrievable");
    }

    // Additional tests for SELECT-related functionality (conditions: OR, AND, and LIKE)
    @Test
    public void testSelectConditions() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE person (name, age, city);");
        sendCommandToServer("INSERT INTO person VALUES ('Alice', 30, 'NewYork');");
        sendCommandToServer("INSERT INTO person VALUES ('Bob', 25, 'LosAngeles');");
        sendCommandToServer("INSERT INTO person VALUES ('Charlie', 35, 'Chicago');");

        // Test SELECT with OR condition: ((age > 26) OR (city == 'LosAngeles'));
        String response = sendCommandToServer("SELECT * FROM person WHERE ((age > 26) OR (city == 'LosAngeles'));");
        // Expect: Alice (age 30 qualifies), Bob (city qualifies), Charlie (age 35 qualifies)
        assertTrue(response.contains("Alice"), "OR condition: Should return Alice");
        assertTrue(response.contains("Bob"), "OR condition: Should return Bob");
        assertTrue(response.contains("Charlie"), "OR condition: Should return Charlie");

        // Test SELECT with AND condition: ((age > 26) AND (city == 'Chicago'));
        response = sendCommandToServer("SELECT * FROM person WHERE ((age > 26) AND (city == 'Chicago'));");
        // Expect: Only Charlie qualifies (35 and Chicago)
        assertFalse(response.contains("Alice"), "AND condition: Should not return Alice");
        assertFalse(response.contains("Bob"), "AND condition: Should not return Bob");
        assertTrue(response.contains("Charlie"), "AND condition: Should return Charlie");

        // Test SELECT with LIKE operator: (name LIKE 'Al')
        response = sendCommandToServer("SELECT * FROM person WHERE (name LIKE 'Al');");
        // Expect: Only Alice should be returned since 'Alice' contains 'Al'
        assertTrue(response.contains("Alice"), "LIKE condition: Should return Alice");
        assertFalse(response.contains("Bob"), "LIKE condition: Should not return Bob");
        assertFalse(response.contains("Charlie"), "LIKE condition: Should not return Charlie");
    }
}

