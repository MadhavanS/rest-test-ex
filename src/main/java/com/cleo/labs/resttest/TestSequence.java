package com.cleo.labs.resttest;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.google.common.io.Resources;

@JsonInclude(Include.NON_NULL)
public class TestSequence {
    private String     description = null;
    private String[]   parameters  = null;
    private TestStep[] steps       = null;

    /**
     * Encapsulates the result of running a {@link TestSequence}.  If
     * {@code ok} is {@code false}, then the errors will be summarized
     * in {@code failed} (indicating the failure step number, starting
     * from 0), {@code failure} (indicating any {@link Exception} that
     * may have occurred, or {@code null} if not), and {@code errors}
     * containing a list of error messages.
     */
    public static /*case*/ class Result {
        public boolean      ok      = true;
        public int          failed  = -1;
        public Exception    failure = null;
        public List<String> errors  = null;
        /**
         * @param error the error to add
         * @return this
         */
        public Result error(String error) {
            if (errors==null) {
                errors = new ArrayList<>();
            }
            errors.add(error);
            errors.forEach((s)->{});
            return this;
        }
        /**
         * Iterates {@code action} over the error messages (provided
         * the list is not {@code null}).
         * @param action
         */
        public void forEachError(Consumer<? super String> action) {
            if (errors!=null) {
                errors.forEach(action);
            }
        }
        
    }
    /*------------------------------------------------------------------------*
     * Evaluating the test case.                                              *
     *------------------------------------------------------------------------*/
    /**
     * Runs a test sequence and returns a {@link Result} indicating the result.
     * @param comparator the {@link JsonComparator}, connecting test contexts
     * @return the result
     */
    public Result run(JsonComparator comparator, JsonNode...parameters) {
        Result result = new Result();
        int expected_parms = this.parameters==null ? 0 : this.parameters.length;
        int actual_parms   = parameters==null ? 0 : parameters.length;
        if (expected_parms != actual_parms) {
            result.ok = false;
            result.error("expected "+expected_parms+
                    " parameters but found "+actual_parms);
        } else if (expected_parms>0) {
            IntStream.range(0, expected_parms).forEach((i) -> 
                comparator.putJson(this.parameters[i], parameters[i]));
        }
        for (int i=0; i<steps.length && result.ok; i++) {
            try {
                TestStep.Result step_result = steps[i].run(comparator);
                if (!step_result.ok) {
                    result.failed = i;
                    result.ok     = false;
                    result.errors = step_result.errors;
                }
            } catch (Exception e) {
                result.failed  = i;
                result.ok      = false;
                result.failure = e;
                e.printStackTrace(new PrintWriter(System.err) {
                    @Override public void println(String s) {
                        result.error(s);
                    }
                    @Override public void println(Object o) {
                        result.error(o.toString());
                    }});
            }
        }
        return result;
    }
    public Result run(JsonNode...parameters) {
        return run(new JsonComparator(), parameters);
    }
    /*------------------------------------------------------------------------*
     * Generated setter/getters (edited for fluency)                          *
     *------------------------------------------------------------------------*/
    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    /**
     * @param description the description to set
     * @return this
     */
    public TestSequence setDescription(String description) {
        this.description = description;
        return this;
    }
    /**
     * @return the parameters
     */
    public String[] getParameters() {
        return parameters;
    }
    /**
     * @param parameters the parameters to set
     * @return this
     */
    public TestSequence setParameters(String[] parameters) {
        this.parameters = parameters;
        return this;
    }
    /**
     * @return the steps
     */
    public TestStep[] getSteps() {
        return steps;
    }
    /**
     * @param steps the steps to set
     * @return this
     */
    public TestSequence setSteps(TestStep[] steps) {
        this.steps = steps;
        return this;
    }
    /*------------------------------------------------------------------------*
     * Test Sequence Factory -- loading from YAML.                            *
     *------------------------------------------------------------------------*/
    private static final Map<String,TestSequence> registry     = new HashMap<>();
    private static final Set<URL>                 loaded       = new HashSet<>();
    private static final YAMLFactory              yaml_factory = new YAMLFactory();
    private static final Loader                   loader       = new Loader();
    public static class Loader {
        public TestSequence get(String id) throws IOException {
            return TestSequence.get(id);
        }
    }
    public static synchronized Loader load(URL u) throws IOException {
        if (!loaded.contains(u)) {
            System.out.println("loading "+u.toString());
            YAMLParser parser = yaml_factory.createParser(u);
            Map<String,TestSequence> tests = JsonComparator.mapper.readValue(parser, new TypeReference<Map<String,TestSequence>>() {});
            tests.forEach((name,ts) -> registry.put(name, ts));
            loaded.add(u);
        }
        return loader;
    }
    public static Loader load(String resource) throws IOException {
        return load(Resources.getResource(resource+".yaml"));
    }
    public static TestSequence get(String id) throws IOException {
        if (!registry.containsKey(id)) {
            throw new IOException("test id \""+id+"\" not found");
        }
        return registry.get(id);
    }
    public static void forEach(BiConsumer<? super String,? super TestSequence> action) {
        registry.forEach(action);
    }
}