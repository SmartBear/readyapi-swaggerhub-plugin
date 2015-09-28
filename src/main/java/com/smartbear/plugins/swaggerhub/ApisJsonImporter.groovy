package com.smartbear.plugins.swaggerhub

import groovy.json.JsonSlurper

class ApisJsonImporter {
    public List<ApiDescriptor> importApis(String json) {
        def apisJson = new JsonSlurper().parseText(json)
        def result = new ArrayList<ApiDescriptor>()

        apisJson.apis.each { it ->
            def descriptor = new ApiDescriptor()
            descriptor.name = it.name
            descriptor.description = it.description

            it.properties.each { prop ->
                if (prop.type == "Swagger") {
                    descriptor.swaggerUrl = prop.url
                }
            }

            result.add(descriptor)
        }

        return result;
    }
}

class ApiDescriptor {
    public String name
    public String description
    public String swaggerUrl

    @Override
    String toString() {
        return name
    }
}
