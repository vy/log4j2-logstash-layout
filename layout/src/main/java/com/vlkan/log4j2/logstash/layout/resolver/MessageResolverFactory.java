package com.vlkan.log4j2.logstash.layout.resolver;

class MessageResolverFactory implements TemplateResolverFactory<MessageResolver> {

    private static final MessageResolverFactory INSTANCE = new MessageResolverFactory();

    private MessageResolverFactory() {}

    static MessageResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return MessageResolver.getName();
    }

    @Override
    public MessageResolver create(TemplateResolverContext context, String key) {
        return new MessageResolver(context, key);
    }

}
