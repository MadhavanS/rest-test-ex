package com.cleo.labs.resttest;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

@JsonInclude(Include.NON_NULL)
public class Response {
    private int                status   = 0;
    private String             type     = "application/json";
    private Map<String,String> headers  = null;
    private Map<String,String> cookies  = null;
    private JsonNode           body     = null;
    private String             schema   = null;
    @JsonIgnore
    private com.jayway.restassured.response.Response
                               response = null;

    /*------------------------------------------------------------------------*
     * Constructors.                                                          *
     *------------------------------------------------------------------------*/
    public Response() {
    }

    public Response(com.jayway.restassured.response.Response response) {
        setStatus(response.statusCode());
        setType(response.contentType());
        response.getHeaders().forEach((h)->setHeader(h.getName(),h.getValue()));
        response.getCookies().forEach((name,value)->setCookie(name,value));
        JsonNode body = null;
        if (response.contentType().equals("application/json")) {
            try {
                body = JsonComparator.mapper.readTree(response.asByteArray());
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
        setBody(body);
        setResponse(response);
    }
    /*------------------------------------------------------------------------*
     * JSON converter.                                                        *
     *------------------------------------------------------------------------*/
    public JsonNode asJson() {
        return JsonComparator.mapper.convertValue(this, JsonNode.class);
    }
    /*------------------------------------------------------------------------*
     * Generated setter/getters (edited for fluency)                          *
     *------------------------------------------------------------------------*/
    /**
     * @return the status
     */
    public int getStatus() {
        return status;
    }
    /**
     * @param status the status to set
     * @return this
     */
    public Response setStatus(int status) {
        this.status = status;
        return this;
    }
    /**
     * @return the type
     */
    public String getType() {
        return type;
    }
    /**
     * @param type the type to set
     * @return this
     */
    public Response setType(String type) {
        this.type = type;
        return this;
    }
    /**
     * @return the headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }
    /**
     * @param headers the headers to set
     * @return this
     */
    public Response setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }
    /**
     * @return the cookies
     */
    public Map<String, String> getCookies() {
        return cookies;
    }
    /**
     * @param cookies the cookies to set
     * @return this
     */
    public Response setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
        return this;
    }
    /**
     * @return the body
     */
    public JsonNode getBody() {
        return body;
    }
    /**
     * @param body the body to set
     * @return this
     */
    public Response setBody(JsonNode body) {
        this.body = body;
        return this;
    }
    /**
     * @return the schema
     */
    public String getSchema() {
        return schema;
    }
    /**
     * @param body the body to set
     * @return this
     */
    public Response setSchema(String schema) {
        this.schema = schema;
        return this;
    }
    /**
     * @return the response
     */
    public com.jayway.restassured.response.Response getResponse() {
        return response;
    }
    /**
     * @param response the response to set
     * @return this
     */
    public Response setResponse(com.jayway.restassured.response.Response response) {
        this.response = response;
        return this;
    }

    /*------------------------------------------------------------------------*
     * Pseudo-setter/getters for Header and Cookie.                           *
     *------------------------------------------------------------------------*/
    /**
     * @param name the header to get
     * @return the header, or {@code null} if it is not set
     */
    public String getHeader(String name) {
        return headers==null ? null : headers.get(name);
    }
    /**
     * @param name the header name to set
     * @param value the header value to set
     * @return this
     */
    public Response setHeader(String name, String value) {
        if (headers==null) {
            headers = new HashMap<>();
        }
        headers.put(name, value);
        return this;
    }
    /**
     * @param name the cookie to get
     * @return the cookie, or {@code null} if it is not set
     */
    public String getCookie(String name) {
        return cookies==null ? null : cookies.get(name);
    }
    /**
     * @param name the cookie name to set
     * @param value the cookie value to set
     * @return this
     */
    public Response setCookie(String name, String value) {
        if (cookies==null) {
            cookies = new HashMap<>();
        }
        cookies.put(name, value);
        return this;
    }
}
