package com.cleo.labs.resttest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class ProviderDrivenTest {

    private static String provider = null;
    private static String sequence = null;
    private static String test     = null;

    @BeforeClass
    @Parameters({"url","sequence","test","provider"})
    public static void setup(String url, String sequence, String test, @Optional String provider) {
        Request.setBaseURI(url);
        ProviderDrivenTest.sequence = sequence;
        ProviderDrivenTest.test     = test;
        ProviderDrivenTest.provider = provider;
    }

    @DataProvider(name = "test")
    public Object[][] createData() throws IOException {
        if (provider!=null) {
            return Provider.load(provider).get(test).getObjects();
        } else {
            return new Object[][] {new Object[] {new ArrayList<JsonNode>()}};
        }
    }

    @Test(dataProvider = "test")
    public void test(List<JsonNode> parameters) throws IOException {
        TestSequence.load(sequence).get(test).run(parameters.toArray(new JsonNode[parameters.size()]));
    }
}
