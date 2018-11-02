package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class ExceptionRootCauseResolver implements TemplateResolver {

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
                                Throwable rootCause = Throwables.getRootCause(exception);
                                String rootCauseClassName = rootCause.getClass().getCanonicalName();
                                jsonGenerator.writeString(rootCauseClassName);
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
                                Throwable rootCause = Throwables.getRootCause(exception);
                                String rootCauseMessage = rootCause.getMessage();
                                boolean rootCauseMessageExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(rootCauseMessage);
                                if (!rootCauseMessageExcluded) {
                                    jsonGenerator.writeString(rootCauseMessage);
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
                            if (!context.isStackTraceEnabled() || exception == null) {
                                jsonGenerator.writeNull();
                            } else {
                                Throwable rootCause = Throwables.getRootCause(exception);
                                ExceptionStackTraceResolvers.resolveText(context, rootCause, jsonGenerator);
                            }
                        }
                    };
                }

                @Override
                TemplateResolver createStackTraceObjectResolver(final TemplateResolverContext context) {
                    return new TemplateResolver() {
                        @Override
                        public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                            Throwable exception = logEvent.getThrown();
                            if (!context.isStackTraceEnabled() || exception == null) {
                                jsonGenerator.writeNull();
                            } else {
                                Throwable rootCause = Throwables.getRootCause(exception);
                                ExceptionStackTraceResolvers.resolveArray(context, rootCause, jsonGenerator);
                            }
                        }
                    };
                }

            };

    private final TemplateResolver internalResolver;

    ExceptionRootCauseResolver(TemplateResolverContext context, String key) {
        this.internalResolver = INTERNAL_RESOLVER_FACTORY.createInternalResolver(context, key);
    }

    static String getName() {
        return "exceptionRootCause";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
