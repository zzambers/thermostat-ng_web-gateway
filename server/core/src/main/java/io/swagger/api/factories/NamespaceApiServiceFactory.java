package io.swagger.api.factories;

import io.swagger.api.NamespaceApiService;
import io.swagger.api.impl.NamespaceApiServiceImpl;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-02-21T16:01:56.111Z")
public class NamespaceApiServiceFactory {
    private final static NamespaceApiService service = new NamespaceApiServiceImpl();

    public static NamespaceApiService getNamespaceApi() {
        return service;
    }
}
