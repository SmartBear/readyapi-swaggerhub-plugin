package com.smartbear.plugins.swaggerhub

import groovy.json.JsonSlurper

class ApisJsonImporter {
    public List<ApiDescriptor> importApis(String json) {
        def apisJson = new JsonSlurper().parseText(json)
        def result = new ArrayList<ApiDescriptor>()

        def basePath = Utils.swaggerHubApiBasePath;

        apisJson.apis.each { it ->
            def descriptor = new ApiDescriptor()
            descriptor.name = it.name
            descriptor.description = it.description

            it.properties.each { prop ->
                if (prop.type == "Swagger") {
                    descriptor.swaggerUrl = prop.url

                    def ix = descriptor.swaggerUrl.indexOf( basePath )
                    def ix2 = descriptor.swaggerUrl.indexOf( '/', ix + basePath.length()+1 )

                    if( ix2 > 0 ){
                        descriptor.owner = descriptor.swaggerUrl.substring( basePath.length()+1, ix2 )
                    }
                }
                else if( prop.type == "X-Versions" ){
                    descriptor.versions = prop.value.split(',')
                }
            }

            result.add(descriptor)
        }

        return result;
    }
}

class ApiDescriptor {
    public String owner
    public String name
    public String description
    public String swaggerUrl
    public String [] versions

    @Override
    String toString() {
        return owner + "/" + name + ", " +
                "[" + versions.length + " version" + ((versions.length==1)?"]":"s] ") +
                description
    }
}
