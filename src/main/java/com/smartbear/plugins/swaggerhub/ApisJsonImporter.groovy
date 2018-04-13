package com.smartbear.plugins.swaggerhub

import groovy.json.JsonSlurper

import java.util.regex.Matcher
import java.util.regex.Pattern

class ApisJsonImporter {
    static final Pattern OWNER_PATTERN = Pattern.compile("api\\.swaggerhub\\.com\\/apis\\/(.*?)\\/");

    public List<ApiDescriptor> importApis(String json) {
        def apisJson = new JsonSlurper().parseText(json)
        def result = new ArrayList<ApiDescriptor>()

        apisJson.apis.each { it ->
            def descriptor = new ApiDescriptor()
            descriptor.name = it.name
            descriptor.description = it.description

            it.properties.each { prop ->
                if (prop.type == "Swagger") {
                    String url = prop.url
                    descriptor.swaggerUrl = url
                    Matcher matcher = OWNER_PATTERN.matcher(url);
                    if (matcher.find()) {
                        descriptor.owner = matcher.group(1)
                    }
                } else if (prop.type == "X-Versions") {
                    descriptor.versions = prop.value.split(',')
                } else if (prop.type == "X-Private") {
                    descriptor.isPrivate = Boolean.parseBoolean(prop.value);
                } else if (prop.type == "X-OASVersion") {
                    descriptor.oasVersion = prop.value
                } else if (prop.type == "X-Published") {
                    descriptor.isPublished = Boolean.parseBoolean(prop.value)
                } else if (prop.type == "X-Version") {
                    descriptor.defaultVersion = prop.value
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
    public String oasVersion
    public String owner
    public String defaultVersion
    public String[] versions
    public boolean isPrivate;
    public boolean isPublished;


    @Override
    String toString() {
        return name + " - " + description + ((description.length() > 0) ? " " : "") +
                "[" + versions.length + " version" + ((versions.length == 1) ? "]" : "s]")
    }
}
