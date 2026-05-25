package com.helper;

import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;

import java.io.InputStream;

public class SchemaHelper {

    /**
     * Validates a REST-Assured response body against a JSON Schema file.
     *
     * @param response   the response to validate
     * @param schemaPath classpath-relative path, e.g. "jsonSchema/LoginSchema.json"
     * @throws RuntimeException if the schema file is not found on the classpath
     */
    public static void verifySchema(Response response, String schemaPath) {
        InputStream schema = SchemaHelper.class.getClassLoader().getResourceAsStream(schemaPath);
        if (schema == null)
            throw new RuntimeException("Schema file not found: " + schemaPath);
        response.then().assertThat().body(JsonSchemaValidator.matchesJsonSchema(schema));
    }
}
