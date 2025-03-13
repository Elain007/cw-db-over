package edu.uob;

import edu.uob.parser.QueryExecuter;

import java.io.IOException;
import java.nio.file.Paths;

public class Test {

    public static void main(String[] args) throws IOException {
        QueryExecuter qe = new QueryExecuter();
        String result;
        result = qe.execute("USE amdxeenpll;");
        System.out.println(result);
        result = qe.execute("JOIN coursework AND marks ON submission AND id;");
        System.out.println(result);
    }

}