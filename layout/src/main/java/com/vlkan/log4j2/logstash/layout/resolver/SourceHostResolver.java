package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.core.LogEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @deprecated Instead, use standard property {@code ${hostName}} that is part of {@code log4j-core} module.
 *
 * @see org.apache.logging.log4j.core.util.NetUtils#getLocalHostname()
 * @see org.apache.logging.log4j.core.LoggerContext#setConfiguration(org.apache.logging.log4j.core.config.Configuration)
 */
@Deprecated
public class SourceHostResolver implements TemplateResolver {

    private static final SourceHostResolver INSTANCE = new SourceHostResolver();

    private static final String SOURCE_HOST = getLocalHost();

    private static final TextNode SOURCE_HOST_NODE = new TextNode(SOURCE_HOST);

    private static String getLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException error) {
            return "localhost";
        }
    }

    private SourceHostResolver() {
        // Do nothing.
    }

    public static SourceHostResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "sourceHost";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent) {
        return SOURCE_HOST_NODE;
    }

}
