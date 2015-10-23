package com.cleo.labs.resttest;

import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.skife.url.UrlSchemeRegistry;
import org.testng.log4testng.Logger;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfiguration;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.collect.Sets;

public class JsonComparator {
    public  static final Logger              logger         = Logger.getLogger(JsonComparator.class);
    private static final ScriptEngineManager engine_factory = new ScriptEngineManager();
    public  static final ObjectMapper        mapper         = new ObjectMapper();

    private              ScriptEngine        engine;

    /*------------------------------------------------------------------------*
     * Set up static objects needed for schema validation.                    *
     *------------------------------------------------------------------------*/
    static {
      UrlSchemeRegistry.register("resource", ResourceHandler.class);
    }
    private static final URI schema_uri = uriOrElse("http://cleo.com/schemas/");
    private static final URI schema_dir = uriOrElse("resource:/com/cleo/versalex/json/schemas/");

    private static URI uriOrElse(String u) {
        try {
            return new URI(u);
        } catch (URISyntaxException ignore) {}
        return null;
    }
    
    private static final JsonSchemaFactory schema_factory =
                 JsonSchemaFactory.byDefault().thaw()
                     .setLoadingConfiguration(
                         LoadingConfiguration.byDefault().thaw()
                         .setURITranslatorConfiguration(
                             URITranslatorConfiguration.newBuilder()
                             .setNamespace(schema_uri)
                             .addPathRedirect(schema_uri, schema_dir)
                             .freeze())
                         .freeze())
                     .freeze();

    public static org.hamcrest.Matcher<?> matchesSchema(String schema) {
        return matchesJsonSchema(uriOrElse(schema_uri.toString()+schema)).using(schema_factory);
    }

    /**
     * Represents a script evaluation result that short-circuits further
     * comparison.  Use these values instead of {@link Boolean}, since
     * {@link Boolean} will be converted to a {@link JsonNode} for
     * comparison with the actual value.
     * <p/>
     * In the JavaScript engine, the following variables are bound:
     * <ul>
     * <li>{@code success} for {@code Result.SUCCESS}</li>
     * <li>{@code failure} for {@code Result.FAILURE}</li>
     * <li>{@code expect(b)} to map {@code Boolean} values to {@code Result} values</li>
     * </ul>
     * For example, the script <pre>${success}</pre> in the {@code expect}
     * tree will match any node in the {@code actual} tree (except a missing node),
     * and <pre>${expect(actual.isArray())}</pre> will match any array node.
     */
    public enum Result {SUCCESS, FAILURE};

    /**
     * Encapsulates a comparison of two {@link JsonNode} trees, one
     * containing the {@code expected} values, which might include ${scripts}
     * to capture complex or variable matching rules, and the other
     * with the {@code actual} values.  Note that ${scripts} are not evaluated
     * for the {@code actual} tree (but you can use {@link JsonComparator#evalNode(JsonNode)
     * evalNode} for that before hand).
     */
    public static class Comparison {
        private JsonPointer path;
        private JsonNode    expected;
        private JsonNode    actual;
        private JsonNode    extra;
        /**
         * Get the {@code path}.
         * @return the {@code path}.
         */
        public JsonPointer getPath() {
            return path;
        }
        /**
         * Get the {@code expected} node.
         * @return the {@code expected} node.
         */
        public JsonNode getExpected() {
            return expected;
        }
        /**
         * Get the {@code actual} node.
         * @return the {@code actual} node.
         */
        public JsonNode getActual() {
            return actual;
        }
        /**
         * Get the {@code extra} node used by default to match extra object fields.
         * @return the {@code extra} node.
         */
        public JsonNode getExtra() {
            return extra;
        }
        /**
         * Set the {@code extra} node used by default to match extra object fields.
         * @param the {@code extra} node to set
         * @return {@code this}
         */
        public Comparison setExtra(JsonNode extra) {
            this.extra = extra;
            return this;
        }
        /**
         * Creates a "root" comparison with a {@code path} of "/".  Deeper
         * comparisons should be obtained using {@link #get(int) get(int i)} and
         * {@link #get(String) get(String name)} methods.
         * @param expected the expected node
         * @param actual the actual node
         */
        public Comparison(JsonNode expected, JsonNode actual) {
            this.path     = JsonPointer.valueOf("/");
            this.expected = expected;
            this.actual   = actual;
            this.extra    = JsonNodeFactory.instance.textNode("${success}");
        }
        /**
         * Assumes that both comparison elements are Arrays and creates
         * a new comparison at index {@code i}.
         * @param i the Array index
         * @return a new {@code Comparison} at index {@code i}
         */
        public Comparison get(int i) {
            Comparison result = new Comparison(expected.get(i), actual.get(i));
            result.path = path.append(JsonPointer.valueOf("/"+String.valueOf(i)));
            result.extra = extra;
            return result;
        }
        /**
         * Assumes that both comparison elements are Objects and creates
         * a new comparison for field name {@code name}.
         * @param name the field name
         * @return a new {@code Comparison} for field name {@code name}
         */
        public Comparison get(String name) {
            JsonNode expectedChild = expected.get(name);
            if (expectedChild==null) {
                // extra actual nodes are matched against the "*" rule
                expectedChild = expected.get("*");
                if (expectedChild==null) {
                    // match against extra (${success} by default)
                    expectedChild = extra;
                }
            }
            JsonNode actualChild   = actual.get(name);
            if (actualChild==null) {
                // missing actual nodes have actual set to "missing"
                actualChild = MissingNode.getInstance();
            }
            Comparison result = new Comparison(expectedChild, actualChild);
            result.path = path.append(JsonPointer.valueOf("/"+name));
            result.extra = extra;
            return result;
        }
        /**
         * Produces a diagnostic summary of the comparison, including the path,
         * both comparison elements and their types, suitable for use in an
         * error report.
         */
        public String toString() {
            return "at "+path+
                   " expected("+(expected==null?"null":expected.getNodeType())+")="+expected+
                   " actual("+(actual==null?"null":actual.getNodeType())+")="+actual;
        }
    }
    
    /**
     * Evaluates {@code script} against the embedded script engine, setting
     * the variables {@code actual} in the engine environment to {@code actual}
     * (even if it is {@code null}). The result of the evaluation is returned.
     * @param script the script to run
     * @param actual the {@code JsonNode} to use for {@code actual}, or {@code null}
     * @return the evaluation result
     */
    public Object evalScript(String script) {
        try {
            Object result = engine.eval(script);
            return result;
        } catch (ScriptException e) {
            logger.debug(e);
            e.printStackTrace();
            return Result.FAILURE;
        }
    }

    public void putVar(String var, Object value) {
        engine.put(var, value);
    }
    public void putJson(String var, JsonNode json) {
        try {
            engine.eval("var "+var+"="+json.toString());
        } catch (ScriptException e) {
            logger.debug(e);
            e.printStackTrace();
        }
    }

    /**
     * Matches ${script}, capturing script as {@code group(1)}.
     */
    private static final Pattern SCRIPT = Pattern.compile("\\$\\{(.*)\\}");

    /**
     * If {@code expected} is a text node that appears to contain
     * a script of the form ${...} returns the script body.
     * If not, returns {@code null}.
     * @param expected a {@code JsonNode}
     * @return a script body, or {@code null}
     */
    private static String script(JsonNode expected) {
        if (expected.isTextual()) {
            Matcher m = SCRIPT.matcher(expected.asText());
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }

    /**
     * Compares two {@code JsonNode} trees, returning a list of
     * {@link Comparison} objects for items that do not match.
     * <p/>
     * The {@code expected} tree must match the {@code actual} tree exactly
     * unless a node in the {@code expected} tree is a text node matching the
     * ${script} pattern.  If a script is found, the {@code actual} tree is
     * injected into the script bindings as {@code actual} and the script is
     * evaluated (using the JavaScript engine).
     * <p/>
     * The script may return a {@link Result} value short-circuiting further
     * matching of this tree node.  Any other value is converted to a {@link JsonNode}
     * and the matching continues with this value.
     * <p/>
     * For convenience, the JavaScript variables {@code success} and {@code failure}
     * are injected into the script, as well as a function {@code expect(boolean)}
     * that maps {@code true} to {@code success} and {@code false} to {@code failure}.
     * @param expected
     * @param actual
     * @return
     */
    public List<Comparison> compareNodes(JsonNode expected, JsonNode actual) {
        Deque<Comparison> compares = new ArrayDeque<>();
        List<Comparison>  report   = new ArrayList<>();
        compares.add(new Comparison(expected, actual));
        while (!compares.isEmpty()) {
            Comparison c = compares.pop();
            if (c.expected==null || c.actual==null) {
                if (c.expected!=null || c.actual!=null) {
                    report.add(c);
                }
                continue; // keep going...
            }
            String script = script(c.expected);
            if (script!=null) {
                putJson("actual", c.actual);
                Object result = evalScript(script);
                if (Result.SUCCESS.equals(result)) {
                    continue; // keep going...
                } else if (Result.FAILURE.equals(result)) {
                    report.add(c);
                    continue; // short circuit failure...keep going...
                } else if (result instanceof JsonNode) {
                    c.expected = (JsonNode)result;
                } else {
                    c.expected = mapper.convertValue(result, JsonNode.class);
                }
            }
            if (c.expected.getNodeType()!=c.actual.getNodeType()) {
                // fall through to report.add failure
            } else if (c.expected.isNumber()) {
                if (c.expected.canConvertToLong() && c.actual.canConvertToLong() &&
                    c.expected.longValue()==c.actual.longValue()) {
                    continue; // keep going...
                } else if (c.actual.equals(c.expected)) {
                    continue; // keep going...
                }
            } else if (c.expected.isValueNode()) {
                if (c.actual.equals(c.expected)) {
                    continue; // keep going...
                }
                // otherwise fall through to report.add failure
            } else if (c.expected.isArray()) {
                if (c.expected.size()==c.actual.size()) {
                    IntStream.range(0, c.expected.size()).forEach((i)->compares.add(c.get(i)));
                    continue; // keep going...
                }
                // otherwise fall through to report.add failure
            } else if (c.expected.isObject()) {
                // check what to do with "extra" actual nodes not corresponding to expected fields
                // ** sets the default policy from here down the tree
                JsonNode starstar = c.expected.get("**");
                if (starstar!=null) {
                    c.setExtra(starstar);
                }
                // for all expected fields, fire off a comparison.
                // note 1: any field in expected without a matching field in
                //         actual will be compared to MissingNode (see get(name))
                // note 2: the expected field "*" is reserved for matching extra
                //         fields in actual not appearing in expected (see below)
                c.expected.fieldNames().forEachRemaining((name)->{
                    if (!(name.equals("*") || name.equals("**"))) {
                        compares.add(c.get(name));
                    }
                });
                // now check the extra fields (if any) against the extra or "*" rule
                Set<String> extra = Sets.newHashSet(c.actual.fieldNames());
                extra.removeAll(Sets.newHashSet(c.expected.fieldNames()));
                extra.forEach((name)->compares.add(c.get(name)));
                continue; // all Object.field cases are delegated
            }
            report.add(c);
        }//);
        return report;
    }

    /**
     * Encapsulates an editable {@link JsonNode} with a lambda that
     * can update the parent reference to the node.  Specifically, this
     * allows elements of an {@link ArrayNode} or {@link ObjectNode} to
     * be evaluated and replaced in place if required.
     */
    public static class Editor {
        private JsonNode           node;
        private Consumer<JsonNode> replacer;
        /**
         * Replaces the encapsulated {@link JsonNode} and calls
         * the {@code replacer} to update its parent reference.
         * @param replacement the new node value
         */
        public void replace(JsonNode replacement) {
            node = replacement;
            replacer.accept(replacement);
        }
        /**
         * Encapsulate a {@link JsonNode} and its replacement lambda.
         * @param node the node to encapsulate
         * @param replacer the lambda to use if the node is updated
         */
        public Editor(JsonNode node, Consumer<JsonNode> replacer) {
            this.node     = node;
            this.replacer = replacer;
        }
        /**
         * This method assumes that {@code node} is an {@link ArrayNode}.
         * Returns an encapsulation of the {@code i}th element of the array,
         * including a {@code replacer} that updates the containing array.
         * @param i the array index
         * @return an encapsulation of array[i]
         */
        public Editor get(int i) {
            return new Editor(node.get(i), (replacement) -> ((ArrayNode)node).set(i, replacement));
        }
        /**
         * This method assumes that {@code node} is an {@link ObjectNode}.
         * Returns an encapsulation of the {@code name} field of the object,
         * including a {@code replacer} that updates the containing object.
         * @param name the object field name
         * @return an encapsulation of object.name
         */
        public Editor get(String name) {
            return new Editor(node.get(name), (replacement) -> ((ObjectNode)node).set(name, replacement));
        }
    }

    /**
     * Provides a wrapper around a value, typically to provide a final
     * (or effectively final) reference to a mutable value for use in
     * a lambda.
     * @param <T> the type of the value
     */
    public static class Wrapper<T> {
        private T value;
        /**
         * Updates the wrapped value.
         * @param value the new value.
         * @return the original {@code Wrapper}
         */
        public Wrapper<T> setValue(T value) { this.value=value; return this; }
        /**
         * Retrieves the wrapped value.
         * @return the wrapped value.
         */
        public T getValue() { return value; }
        /**
         * Create a new wrapper around a value.
         * @param value the initial wrapped value.
         */
        public Wrapper(T value) { this.value=value; }
    }

    /**
     * Walks a {@link JsonNode}, replacing any {$script} expressions
     * with their evaluation results, returning the edited node.  Where
     * possible, the tree is edited in place, with ${script} nodes replaced
     * by their evaluation results.  If the top-level node it itself a
     * ${script} node, returns the evaluation result directly as a new
     * node.
     * @param json the {@link JsonNode} to edit
     * @return the edited node
     */
    public JsonNode evalNode(JsonNode json) {
        Deque<Editor> visits = new ArrayDeque<>();
        Wrapper<JsonNode> w = new Wrapper<>(json);
        visits.add(new Editor(json, (replacement) -> w.setValue(replacement)));
        while (!visits.isEmpty()) {
            Editor e = visits.pop();
            if (e.node==null) continue;
            String script = script(e.node);
            if (script!=null) {
                Object result = evalScript(script);
                if (result instanceof JsonNode) {
                    e.replace((JsonNode)result);
                } else {
                    e.replace(mapper.convertValue(result, JsonNode.class));
                }
            }
            if (e.node.isArray()) {
                IntStream.range(0, e.node.size()).forEach((i)->visits.add(e.get(i)));
            } else if (e.node.isObject()) {
                e.node.fieldNames().forEachRemaining((name)->visits.add(e.get(name)));
            }
        }
        return w.getValue();
    }

    /**
     * Converts a JSON string (e.g. from {@code JSON.stringify}) to YAML
     * format.  Returns {@code "null"} for {@code null} or the exception
     * text in case of error.
     * @param s a JSON string
     * @return a YAML string
     */
    public static String yaml(String s) {
        String content;
        if (s==null) {
            content = "null";
        } else {
            try {
                content = new ObjectMapper(new YAMLFactory()).writeValueAsString(new ObjectMapper().readTree(s));
            } catch (IOException e) {
                content = e.toString();
            }
        }
        return content;
    }

    /**
     * Creates a new {@code JsonComparator} and initializes a {@link ScriptEngine}
     * instance for use by subsequent invocations of {@link #compareNodes(JsonNode, JsonNode)
     * compareNodes} and {@link #evalNode(JsonNode) evalNode}.  Since the script
     * engine is bound to this {@code JsonComparator}, any {@code var} assignments
     * made in the embedded ${script}s will survive, allowing chaining of compare and eval
     * invocations.
     */
    public JsonComparator () {
        engine  = engine_factory.getEngineByName("JavaScript");
        try {
            engine.eval("load('nashorn:mozilla_compat.js');"+
                        "importClass(Packages."+JsonComparator.Result.class.getName()+");"+
                        "var success=JsonComparator$Result.SUCCESS;"+
                        "var failure=JsonComparator$Result.FAILURE;"+
                        "function expect(b) { return b ? success : failure; }"+
                        "function println(s) { java.lang.System.out.println(s); }"+
                        "function yaml(o) { return com.cleo.labs.resttest.JsonComparator.yaml(JSON.stringify(o)); }"+
                        "function debug(s) { com.cleo.labs.resttest.JsonComparator.logger.debug(s); }");
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }
}
