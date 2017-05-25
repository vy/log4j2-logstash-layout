package com.vlkan.log4j2.logstash.layout.demo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum LogstashLayoutDemo {;

    public static void main(String[] args) {
        Logger logger = LogManager.getLogger(LogstashLayoutDemo.class);
        logger.info("Hello, world!");
        RuntimeException error = new RuntimeException("test");
        logger.error("Hello, error!", error);
    }

}
