package com.cleo.labs.resttest;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;
import org.testng.log4testng.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    public static Object printClassAndValue(Object o) {
        if (o==null) {
            System.out.println("null: null");
        } else {
            System.out.println(o.getClass().getName()+": "+o);
        }
        return o;
    }
    @Test
    public void testConvert() {
        ObjectMapper om = new ObjectMapper();
        Object v;
        v="string"; printClassAndValue(om.convertValue(v, JsonNode.class));
        v=1;        printClassAndValue(om.convertValue(v, JsonNode.class));
        v=false;    printClassAndValue(om.convertValue(v, JsonNode.class));
        v=123L;     printClassAndValue(om.convertValue(v, JsonNode.class));
    }
    @Test
    public void testJsonMatch() throws Exception {
        String yaml = Resources.toString(Resources.getResource("test-compare.yaml"), Charsets.UTF_8);
        JsonNode json = new ObjectMapper().readTree(new YAMLFactory().createParser(yaml));
        JsonComparator comparator = new JsonComparator();
        json.fields().forEachRemaining((entry) -> {
            String   name = entry.getKey();
            JsonNode test = entry.getValue();
            List<JsonComparator.Comparison> mismatches = comparator.compareNodes(test.get("expected"), comparator.evalNode(test.get("actual")));
            boolean result = test.get("result").asBoolean();
            System.out.println("results for test "+name+" expected result="+result);
            mismatches.forEach((m)->System.out.println(m.toString()));
            assertEquals(result, mismatches.isEmpty());
        });
    }
    public JsonNode responseAsNode(com.jayway.restassured.response.Response response) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.set("status", JsonNodeFactory.instance.numberNode(response.statusCode()));
        node.set("type", JsonNodeFactory.instance.textNode(response.contentType()));
        ObjectNode headers = JsonNodeFactory.instance.objectNode();
        response.getHeaders().forEach((h)->headers.set(h.getName(),JsonNodeFactory.instance.textNode(h.getValue())));
        if (headers.size()>0) {
            node.set("headers", headers);
        }
        ObjectNode cookies = JsonNodeFactory.instance.objectNode();
        response.getCookies().forEach((name,value)->cookies.set(name,JsonNodeFactory.instance.textNode(value)));
        if (cookies.size()>0) {
            node.set("cookies", cookies);
        }
        JsonNode body = null;
        if (response.contentType().equals("application/json")) {
            try {
                body = new ObjectMapper().readTree(response.asByteArray());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (body==null) {
            String text = response.asString();
            if (text.matches(".*[^\\p{Print}\\p{Space}].*")) {
                body = JsonNodeFactory.instance.binaryNode(response.asByteArray());
            } else {
                body = JsonNodeFactory.instance.textNode(text);
            }
        }
        node.set("body", body);
        return node;
    }
    @Test
    public void testRest() throws Exception {
        com.jayway.restassured.response.Response response = 
        given().
          auth().preemptive().basic("administrator","Admin").
        when().
          get("http://192.168.50.105:5080/api/certs?count=2");
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        System.out.println(om.writeValueAsString(responseAsNode(response)));
    }
    @Test
    public void testRest2() throws Exception {
        Request.setBaseURI("http://192.168.50.105:5080/api");
        TestSequence.load("test-rest").get("helloworld").run().forEachError(System.out::println);
        TestSequence.forEach((name,ts) -> {
            logger.debug("running test "+name);
            System.out.println("running test "+name+
                    (ts.getDescription()!=null ? " ("+ts.getDescription()+")" : ""));
            JsonNode[] parameters = null;
            if (ts.getParameters()!=null) {
                parameters = (JsonNode[]) Arrays.stream(ts.getParameters()).map((p) ->
                    JsonComparator.mapper.convertValue(p, JsonNode.class)).toArray(JsonNode[]::new);
            }
            TestSequence.Result result = ts.run(parameters);
            if (!result.ok) {
                result.forEachError(System.out::println);
                fail();
            }
        });
    }
    @Test
    public void testProvider() throws Exception {
        Provider p = Provider.load("test-provider").get("provider1");
        printClassAndValue(p.getDescription());
        p.getData().forEach(Main::printClassAndValue);
        Object[][] objects = p.getObjects();
        for (int i=0; i<objects.length; i++) {
            for (int j=0; j<objects[i].length; j++) {
                System.out.print("objects["+i+"]["+j+"]=");
                printClassAndValue(objects[i][j]);
            }
        }
    }
    @Test
    public void testSchema() throws Exception {
        given().
          auth().preemptive().basic("administrator","Admin").
        when().
          get("http://192.168.50.105:5080/api/certs?count=2").
        then().
          body(JsonComparator.matchesSchema("collection.schema"));
    }
}
