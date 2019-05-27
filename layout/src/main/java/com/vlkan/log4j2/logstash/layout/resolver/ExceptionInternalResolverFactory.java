package com.vlkan.log4j2.logstash.layout.resolver;

abstract class ExceptionInternalResolverFactory {

    private static final EventResolver NULL_RESOLVER = (ignored, jsonGenerator) -> jsonGenerator.writeNull();

    EventResolver createInternalResolver(EventResolverContext context, String key) {

        // Split the key into its major and minor components.
        String majorKey;
        String minorKey;
        int colonIndex = key.indexOf(':');
        if (colonIndex >= 0) {
            majorKey = key.substring(0, colonIndex);
            minorKey = key.substring(colonIndex + 1);
        } else {
            majorKey = key;
            minorKey = "";
        }

        // Create the resolver.
        switch (majorKey) {
            case "className": return createClassNameResolver();
            case "message": return createMessageResolver(context);
            case "stackTrace": return createStackTraceResolver(context, minorKey);
        }
        throw new IllegalArgumentException("unknown key: " + key);

    }

    abstract EventResolver createClassNameResolver();

    abstract EventResolver createMessageResolver(EventResolverContext context);

    private EventResolver createStackTraceResolver(EventResolverContext context, String minorKey) {
        if (!context.isStackTraceEnabled()) {
            return NULL_RESOLVER;
        }
        switch (minorKey) {
            case "text": return createStackTraceTextResolver(context);
            case "": return createStackTraceObjectResolver(context);
        }
        throw new IllegalArgumentException("unknown minor key: " + minorKey);
    }

    abstract EventResolver createStackTraceTextResolver(EventResolverContext context);

    abstract EventResolver createStackTraceObjectResolver(EventResolverContext context);

}
