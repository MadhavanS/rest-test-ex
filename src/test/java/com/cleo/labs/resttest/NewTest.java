package com.cleo.labs.resttest;

import java.io.IOException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class NewTest {

    @BeforeClass
    @Parameters({"url"})
    public static void setup(String url) {
        Request.setBaseURI(url);
    }

    @DataProvider(name = "test1")
    public Object[][] createData1() throws IOException {
        return Provider.load("test-provider").get("test1").getObjects();
    }

    @Test(dataProvider = "test1")
    public void test1(JsonNode count, JsonNode name) throws IOException {
        TestSequence.load("test-rest").get("test1").run(count, name);
    }
}
