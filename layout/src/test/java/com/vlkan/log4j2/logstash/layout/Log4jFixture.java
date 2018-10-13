package com.vlkan.log4j2.logstash.layout;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

public enum Log4jFixture {;

    public static final LoggerContext LOGGER_CONTEXT = (LoggerContext) LogManager.getContext(false);

    public static final Configuration CONFIGURATION = LOGGER_CONTEXT.getConfiguration();

}
