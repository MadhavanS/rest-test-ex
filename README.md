# REST API Testing Using TestNG, RestAssured, Jackson, YAML and Java 8 #

This project shows a YAML-centric approach to using TestNG to drive REST
API testing using RestAssured and Jackson.  The idea is this:

1. Create a YAML file describing test sequences, where each *sequence* is
   a list of *steps*, each of which may include a `before` script (in
   JavaScript), a `request` and `response` (in JSON), and an `after`
   script.  A single `ScriptEngine` instance is used throughout the test
   sequence, so you can store data and references between steps.
   
2. Create a YAML file describing your test data.  This is a simple format
   providing lists of named parameters (in JSON) that match the parameters
   on your test sequences.
   
3. Write TestNG test classes that map to test sequences (through a very
   simple API), and TestNG data providers that map to your test data
   collections (also through a very simple API).
   
4. Use TestNG's YAML format to create a test suite file (I had to create some
   patching classes to bridge TestNG's reliance on an out-of-date version
   of Snakeyaml to the current release to avoid CNF errors when using the
   TestNG/Eclipse integration).  Run your tests.

Test sequence `response` documents can contain special matching expressions of the form `${script}`, which indicate that `script` should be evaluated (using Java's embedded JavaScript engine) instead of performing literal matching of `response` to the actual response to the `request`.  The script can return a calculated value to return (such as maybe a reference to a previously stored variable), or can return the special `success` or `failure` values to indicate calculated match results (the `boolean` values `true` and `false` are used to match `boolean` values in the JSON response).  The `expect(b)` JavaScript function converts a `boolean` `true` or `false` into `success` or `failure`.

For example, here is a simple test sequence that checks the accuracy of the `?count` query parameter on a API against the size of the result collection:

```
testsequence1:
  description: list the certs
  parameters:
  - count
  steps:
  - description: get the first count certs
    request:
      method: GET
      path:   ${"/certs?count="+count}
    response:
      status: 200
      schema: collection.schema
      body:
        totalResults: ${success}
        startIndex: 0
        count: ${count}
        resources: ${expect(Array.isArray(actual) && actual.length==count)}
    after: |
      println("step 1 after report: "+response.body.totalResults)
```

Here you can see the `count` parameter, expected to be injected from the data provider, used to calculate the path string for the `GET` request.  The response is validated against a json-schema, but then is also matched against the JSON response template for status code and body syntax.

Any value is accepted for `totalResults` (although a more rigorous expression might check that it is actually a number), but the `resources` field is checked for type (an array) and size (the `count` parameter) using the injected `actual` variable, which contains the `JsonNode` corresponding to the value to be matched converted to a JavaScript object or value.

The `after` script illustrates

* use of the built-in `println` function to produce output on `System.out`
* (there is also a `debug` function that produces debug ouput on a `Logger`)
* reference to the injected `response` from JavaScript (`request` is also injected)

There are more examples in the unit tests.