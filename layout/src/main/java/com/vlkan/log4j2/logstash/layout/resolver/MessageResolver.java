package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.vlkan.log4j2.logstash.layout.util.JsonGenerators;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.*;
import org.apache.logging.log4j.util.TriConsumer;

import java.io.IOException;

class MessageResolver implements EventResolver {

    private static final String NAME = "message";

    private static final String[] FORMATS = { "JSON" };

    private final EventResolverContext context;

    private final String key;

    MessageResolver(EventResolverContext context, String key) {
        this.context = context;
        this.key = key;
    }

    static String getName() {
        return NAME;
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        Message message = logEvent.getMessage();
        if (FORMATS[0].equalsIgnoreCase(key)) {
            resolveJson(message, jsonGenerator);
        } else {
            resolveText(message, jsonGenerator);
        }
    }

    private void resolveText(Message message, JsonGenerator jsonGenerator) throws IOException {
        String formattedMessage = resolveText(message);
        if (formattedMessage == null) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeString(formattedMessage);
        }
    }

    private String resolveText(Message message) {
        String formattedMessage = message.getFormattedMessage();
        boolean messageExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(formattedMessage);
        return messageExcluded ? null : formattedMessage;
    }

    private void resolveJson(Message message, JsonGenerator jsonGenerator) throws IOException {

        // Try SimpleMessage serializer.
        if (writeSimpleMessage(jsonGenerator, message)) {
            return;
        }

        // Try MultiformatMessage serializer.
        if (writeMultiformatMessage(jsonGenerator, message)) {
            return;
        }

        // Try ObjectMessage serializer.
        if (writeObjectMessage(jsonGenerator, message)) {
            return;
        }

        // Fallback to plain Object write.
        writeObject(message, jsonGenerator);

    }

    private boolean writeSimpleMessage(JsonGenerator jsonGenerator, Message message) throws IOException {

        // Check type.
        if (!(message instanceof SimpleMessage)) {
            return false;
        }
        SimpleMessage simpleMessage = (SimpleMessage) message;

        // Write message.
        String formattedMessage = simpleMessage.getFormattedMessage();
        boolean messageExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(formattedMessage);
        if (messageExcluded) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeString(formattedMessage);
        }
        return true;

    }

    private boolean writeMultiformatMessage(JsonGenerator jsonGenerator, Message message) throws IOException {

        // Check type.
        if (!(message instanceof MultiformatMessage)) {
            return false;
        }
        MultiformatMessage multiformatMessage = (MultiformatMessage) message;

        // As described in LOG4J2-2703, MapMessage#getFormattedMessage() is
        // incorrectly formatting Object's. Hence, we will temporarily work
        // around the problem by serializing it ourselves rather than using the
        // default provided formatter.

        // Override the provided MapMessage formatter.
        if (context.isMapMessageFormatterIgnored() && message instanceof MapMessage) {
            MapMessage mapMessage = (MapMessage) message;
            writeMapMessage(jsonGenerator, mapMessage);
            return true;
        }

        // Check formatter's JSON support.
        boolean jsonSupported = false;
        String[] formats = multiformatMessage.getFormats();
        for (String format : formats) {
            if (FORMATS[0].equalsIgnoreCase(format)) {
                jsonSupported = true;
                break;
            }
        }

        // Get the formatted message, if there is any.
        if (!jsonSupported) {
            writeObject(message, jsonGenerator);
            return true;
        }

        // Write the formatted JSON.
        String messageJson = multiformatMessage.getFormattedMessage(FORMATS);
        JsonNode jsonNode = readMessageJson(context, messageJson);
        boolean nodeExcluded = isNodeExcluded(jsonNode);
        if (nodeExcluded) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeTree(jsonNode);
        }
        return true;

    }

    private static void writeMapMessage(JsonGenerator jsonGenerator, MapMessage mapMessage) throws IOException {
        jsonGenerator.writeStartObject();
        mapMessage.forEach(MAP_MESSAGE_ENTRY_WRITER, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    private static TriConsumer<String, Object, JsonGenerator> MAP_MESSAGE_ENTRY_WRITER =
            (key, value, jsonGenerator) -> {
                try {
                    jsonGenerator.writeFieldName(key);
                    JsonGenerators.writeObject(jsonGenerator, value);
                } catch (IOException error) {
                    throw new RuntimeException("MapMessage entry serialization failure", error);
                }
            };

    private static JsonNode readMessageJson(EventResolverContext context, String messageJson) {
        try {
            return context.getObjectMapper().readTree(messageJson);
        } catch (IOException error) {
            throw new RuntimeException("JSON message read failure", error);
        }
    }

    private void writeObject(Message message, JsonGenerator jsonGenerator) throws IOException {

        // Resolve text node.
        String formattedMessage = resolveText(message);
        if (formattedMessage == null) {
            jsonGenerator.writeNull();
            return;
        }

        // Put textual representation of the message in an object.
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField(NAME, formattedMessage);
        jsonGenerator.writeEndObject();

    }

    private boolean isNodeExcluded(JsonNode jsonNode) {

        if (!context.isEmptyPropertyExclusionEnabled()) {
            return false;
        }

        if (jsonNode.isNull()) {
            return true;
        }

        if (jsonNode.isTextual() && StringUtils.isEmpty(jsonNode.asText())) {
            return true;
        }

        // noinspection RedundantIfStatement
        if (jsonNode.isContainerNode() && jsonNode.size() == 0) {
            return true;
        }

        return false;

    }

    private boolean writeObjectMessage(JsonGenerator jsonGenerator, Message message) throws IOException {

        // Check type.
        if (!(message instanceof ObjectMessage)) {
            return false;
        }

        // Serialize object.
        ObjectMessage objectMessage = (ObjectMessage) message;
        Object object = objectMessage.getParameter();
        JsonGenerators.writeObject(jsonGenerator, object);
        return true;

    }

}
