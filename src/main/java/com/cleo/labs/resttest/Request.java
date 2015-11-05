package com.cleo.labs.resttest;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.specification.RequestSender;
import com.jayway.restassured.specification.RequestSpecification;

@JsonInclude(Include.NON_NULL)
public class Request {
    private static String baseURI  = null;
    private static int    port     = -1;
    private static String basePath = null;

    public interface MethodInvoker {
        com.jayway.restassured.response.Response apply(RequestSender req, String path);
    }
    public enum Method {
        HEAD    ((req,path)->req.head(path)),
        GET     ((req,path)->req.get(path)),
        POST    ((req,path)->req.post(path)),
        PUT     ((req,path)->req.put(path)),
        DELETE  ((req,path)->req.delete(path)),
        PATCH   ((req,path)->req.patch(path)),
        OPTIONS ((req,path)->req.options(path));
        private MethodInvoker method;
        private Method(MethodInvoker method) {
            this.method = method;
        }
        public MethodInvoker getMethod() {
            return method;
        }
    }

    private String             username = "administrator";
    private String             password = "Admin";
    private Method             method   = Method.GET;
    private String             path     = null;
    private String             type     = "application/json";
    private Map<String,String> headers  = null;
    private Map<String,String> cookies  = null;
    private JsonNode           body     = null;

    /*------------------------------------------------------------------------*
     * Doing the method, convert Request to Response.                         *
     *------------------------------------------------------------------------*/
    @JsonIgnore
    public Response getResponse() {
        RequestSpecification reqspec = RestAssured.given();
        if (baseURI!=null) {
            reqspec = reqspec.baseUri(baseURI);
        }
        if (port>=0) {
            reqspec = reqspec.port(port);
        }
        if (basePath!=null) {
            reqspec = reqspec.basePath(basePath);
        }
        if (username!=null && password!=null) {
            reqspec = reqspec.auth().preemptive().basic(username, password);
        }
        if (headers!=null) {
            reqspec = reqspec.headers(headers);
        }
        if (cookies!=null) {
            reqspec = reqspec.cookies(cookies);
        }
        if (body!=null) {
            if (type!=null) {
                reqspec = reqspec.contentType(type);
            }
            reqspec = reqspec.body(body.toString());
        }
        return new Response(method.getMethod().apply(reqspec, path));
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
     * @return the baseURI
     */
    public static String getBaseURI() {
        return baseURI;
    }
    /**
     * @param baseURI the baseURI to set
     */
    public static void setBaseURI(String baseURI) {
        Request.baseURI = baseURI;
    }
    /**
     * @return the port
     */
    public static int getPort() {
        return port;
    }
    /**
     * @param port the port to set
     */
    public static void setPort(int port) {
        Request.port = port;
    }
    /**
     * @return the basePath
     */
    public static String getBasePath() {
        return basePath;
    }
    /**
     * @param basePath the basePath to set
     */
    public static void setBasePath(String basePath) {
        Request.basePath = basePath;
    }
    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }
    /**
     * @param username the username to set
     * @return this
     */
    public Request setUsername(String username) {
        this.username = username;
        return this;
    }
    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }
    /**
     * @param password the password to set
     * @return this
     */
    public Request setPassword(String password) {
        this.password = password;
        return this;
    }
    /**
     * @return the method
     */
    public Method getMethod() {
        return method;
    }
    /**
     * @param method the method to set
     * @return this
     */
    public Request setMethod(Method method) {
        this.method = method;
        return this;
    }
    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }
    /**
     * @param path the path to set
     * @return this
     */
    public Request setPath(String path) {
        this.path = path;
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
    public Request setType(String type) {
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
    public Request setHeaders(Map<String, String> headers) {
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
    public Request setCookies(Map<String, String> cookies) {
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
    public Request setBody(JsonNode body) {
        this.body = body;
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
    public Request setHeader(String name, String value) {
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
    public Request setCookie(String name, String value) {
        if (cookies==null) {
            cookies = new HashMap<>();
        }
        cookies.put(name, value);
        return this;
    }
}