package com.cleo.labs.resttest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(Include.NON_NULL)
public class TestStep {
    private String   description = null;
    private String   before      = null;
    private JsonNode request     = null;
    private JsonNode response    = null;
    private String   after       = null;

    /**
     * Encapsulates the result of running a {@link TestStep}.  If
     * {@code ok} is {@code false}, then the errors will be summarized
     * in {@code errors}, which will contain a list of error messages.
     * If the {@code before} expression evaluated to
     * {@link JsonComparator.Result#SUCCESS SUCCESS}, this is interpreted
     * as a skip signal and {@code skip} will be {@code true}.
     */
    public static /*case*/ class Result {
        public boolean      ok     = true;
        public boolean      skip   = false;
        public List<String> errors = null;
        /**
         * @param error the error to add
         * @return this
         */
        public Result error(String error) {
            if (errors==null) {
                errors = new ArrayList<>();
            }
            errors.add(error);
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
    Result run(JsonComparator comparator) {
        Result result = new Result();
        Response actual = null;
        if (result.ok && before!=null) {
            Object script_result = comparator.evalScript(before);
            if (JsonComparator.Result.FAILURE.equals(script_result)) {
                result.ok = false;
                result.error("before script failed: "+before);
            } else if (JsonComparator.Result.SUCCESS.equals(script_result)) {
                // this means skip the request/response/after scripts
                result.skip = true;
            }
        }
        if (result.ok && !result.skip && request!=null) {
            Request req = JsonComparator.mapper.convertValue(comparator.evalNode(request), Request.class);
            actual = req.getResponse();
            result.ok = actual!=null;
            comparator.putJson("request", req.asJson());
            comparator.putJson("response", actual.asJson());
        }
        if (result.ok && !result.skip && actual!=null && response!=null) {
            if (response.get("schema")!=null) {
                String schema = response.get("schema").asText();
                actual.getResponse().then().body(JsonComparator.matchesSchema(schema));
                actual.setSchema(schema);
            }
            List<JsonComparator.Comparison> mismatches = comparator.compareNodes(response, actual.asJson());
            result.ok = mismatches.isEmpty();
            mismatches.forEach((c)->result.error(c.toString()));
        }
        if (result.ok && !result.skip && after!=null) {
            if (JsonComparator.Result.FAILURE.equals(comparator.evalScript(after))) {
                result.ok = false;
                result.error("after script failed: "+after);
            }
        }
        return result;
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
    public TestStep setDescription(String description) {
        this.description = description;
        return this;
    }
    /**
     * @return the before
     */
    public String getBefore() {
        return before;
    }
    /**
     * @param before the before to set
     * @return this
     */
    public TestStep setBefore(String before) {
        this.before = before;
        return this;
    }
    /**
     * @return the request
     */
    public JsonNode getRequest() {
        return request;
    }
    /**
     * @param request the request to set
     * @return this
     */
    public TestStep setRequest(JsonNode request) {
        this.request = request;
        return this;
    }
    /**
     * @return the response
     */
    public JsonNode getResponse() {
        return response;
    }
    /**
     * @param response the response to set
     * @return this
     */
    public TestStep setResponse(JsonNode response) {
        this.response = response;
        return this;
    }
    /**
     * @return the after
     */
    public String getAfter() {
        return after;
    }
    /**
     * @param after the after to set
     * @return this
     */
    public TestStep setAfter(String after) {
        this.after = after;
        return this;
    }
}