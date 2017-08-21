package com.vlkan.log4j2.logstash.layout.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JacksonNewlineAddingPrettyPrinterTest {

    @Test
    public void test_appended_newline() throws JsonProcessingException {

        // Create a test object.
        Map<String, Object> map = new HashMap<>();
        map.put("1", 10);
        map.put("2", Arrays.asList(3, 4, 5));

        // Compare two different writers.
        ObjectMapper objectMapper = new ObjectMapper();
        String defaultOutput = objectMapper.writer().writeValueAsString(map);
        String newlineAppendedOutput = objectMapper.writer(new JacksonNewlineAddingPrettyPrinter()).writeValueAsString(map);
        assertThat(newlineAppendedOutput).isEqualTo(defaultOutput + System.lineSeparator());

    }

}
