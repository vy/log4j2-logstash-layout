package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class ExceptionResolver implements TemplateResolver {

    private static final ExceptionInternalResolverFactory INTERNAL_RESOLVER_FACTORY =
            new ExceptionInternalResolverFactory() {

                @Override
                TemplateResolver createClassNameResolver() {
                    return new TemplateResolver() {
                        @Override
                        public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                            Throwable exception = logEvent.getThrown();
                            if (exception == null) {
                                jsonGenerator.writeNull();
                            } else {
                                String exceptionClassName = exception.getClass().getCanonicalName();
                                jsonGenerator.writeString(exceptionClassName);
                            }
                        }
                    };
                }

                @Override
                TemplateResolver createMessageResolver(final TemplateResolverContext context) {
                    return new TemplateResolver() {
                        @Override
                        public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                            Throwable exception = logEvent.getThrown();
                            if (exception != null) {
                                String exceptionMessage = exception.getMessage();
                                boolean exceptionMessageExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(exceptionMessage);
                                if (!exceptionMessageExcluded) {
                                    jsonGenerator.writeString(exceptionMessage);
                                    return;
                                }
                            }
                            jsonGenerator.writeNull();
                        }
                    };
                }

                @Override
                TemplateResolver createStackTraceTextResolver(final TemplateResolverContext context) {
                    return new TemplateResolver() {
                        @Override
                        public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                            Throwable exception = logEvent.getThrown();
                            ExceptionStackTraceResolvers.resolveText(context, exception, jsonGenerator);
                        }
                    };
                }

                @Override
                TemplateResolver createStackTraceObjectResolver(final TemplateResolverContext context) {
                    return new TemplateResolver() {
                        @Override
                        public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                            Throwable exception = logEvent.getThrown();
                            ExceptionStackTraceResolvers.resolveArray(context, exception, jsonGenerator);
                        }
                    };
                }

            };

    private final TemplateResolver internalResolver;

    ExceptionResolver(TemplateResolverContext context, String key) {
        this.internalResolver = INTERNAL_RESOLVER_FACTORY.createInternalResolver(context, key);
    }

    static String getName() {
        return "exception";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
