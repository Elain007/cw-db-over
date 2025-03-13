package edu.uob;

import edu.uob.parser.QueryExecuter;

import java.io.IOException;
import java.nio.file.Paths;

public class Test {

    public static void main(String[] args) throws IOException {
        QueryExecuter qe = new QueryExecuter();
        String result;
//        String storageFolderPath = Paths.get("databases").toAbsolutePath().toString();

//        System.out.println(storageFolderPath);
//        qe.setDatabase("testDB");
        result = qe.execute("USE amdxeenpll;");
        System.out.println(result);
//
//        result = qe.execute("SELECT * FROM users;");
//        System.out.println(result);
//
//        result = qe.execute("create database testdb2;");
//        System.out.println(result);

        result = qe.execute("JOIN coursework AND marks ON submission AND id;");
        System.out.println(result);
    }

}