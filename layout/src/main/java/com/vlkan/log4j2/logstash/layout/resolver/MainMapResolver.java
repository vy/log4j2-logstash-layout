package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.MainMapLookup;

public class MainMapResolver implements EventResolver {

  private final EventResolverContext context;

  private final String key;

  private final MainMapLookup mainMapLookup;

  static String getName() {
    return "main";
  }

  MainMapResolver(EventResolverContext context, String key) {
      this.context = context;
      this.key = key;
      this.mainMapLookup = new MainMapLookup();
  }

  @Override
  public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
    String value = mainMapLookup.lookup(key);
    if (value != null) {
      jsonGenerator.writeObject(value);
    }
  }
}
