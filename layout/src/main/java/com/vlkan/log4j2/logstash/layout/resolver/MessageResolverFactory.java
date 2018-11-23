package com.vlkan.log4j2.logstash.layout.resolver;

class MessageResolverFactory implements EventResolverFactory<MessageResolver> {

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
    public MessageResolver create(EventResolverContext context, String key) {
        return new MessageResolver(context, key);
    }

}
