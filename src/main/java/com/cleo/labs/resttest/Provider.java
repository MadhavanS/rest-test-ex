package com.cleo.labs.resttest;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.google.common.io.Resources;

@JsonInclude(Include.NON_NULL)
public class Provider {
    private String           description = null;
    private String[]         parameters  = null;
    private List<ObjectNode> data        = null;

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
    public Provider setDescription(String description) {
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
    public Provider setParameters(String[] parameters) {
        this.parameters = parameters;
        return this;
    }
    /**
     * @return the data
     */
    public List<ObjectNode> getData() {
        return data;
    }
    /**
     * @param data the data to set
     * @return this
     */
    public Provider setData(List<ObjectNode> data) {
        this.data = data;
        return this;
    }
    /*------------------------------------------------------------------------*
     * Getting the Object[][].                                                *
     *------------------------------------------------------------------------*/
    public Object[][] getObjects() {
        return getObjects(parameters);
    }
    public Object[][] getObjects(String[] parameters) {
        if (parameters==null || parameters.length==0) {
            throw new IllegalArgumentException("parameters not specified");
        }
        return data.stream().map((object) ->
            Arrays.stream(parameters).map(object::get).toArray(Object[]::new)
        ).toArray(Object[][]::new);
    }
    /*------------------------------------------------------------------------*
     * Test Sequence Factory -- loading from YAML.                            *
     *------------------------------------------------------------------------*/
    private static final Map<String,Provider>     registry     = new HashMap<>();
    private static final Set<URL>                 loaded       = new HashSet<>();
    private static final YAMLFactory              yaml_factory = new YAMLFactory();
    private static final Loader                   loader       = new Loader();
    public static class Loader {
        public Provider get(String id) throws IOException {
            return Provider.get(id);
        }
    }
    public static synchronized Loader load(URL u) throws IOException {
        if (!loaded.contains(u)) {
            System.out.println("loading "+u.toString());
            YAMLParser parser = yaml_factory.createParser(u);
            Map<String,Provider> tests = JsonComparator.mapper.readValue(parser, new TypeReference<Map<String,Provider>>() {});
            tests.forEach((name,ts) -> registry.put(name, ts));
            loaded.add(u);
        }
        return loader;
    }
    public static Loader load(String resource) throws IOException {
        return load(Resources.getResource(resource+".yaml"));
    }
    public static Provider get(String id) throws IOException {
        if (!registry.containsKey(id)) {
            throw new IOException("test id \""+id+"\" not found");
        }
        return registry.get(id);
    }
    public static void forEach(BiConsumer<? super String,? super Provider> action) {
        registry.forEach(action);
    }
}
