package dk.bringlarsen.springintegration.components.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.http.Http;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

@Component
public class Route {

    @Autowired
    private DirectChannel inChannel;

    @Autowired
    private DirectChannel outChannel;

    @Autowired
    public DirectChannel errChannel;

    @Autowired
    private DirectChannel filterDiscardChannel;

    @Autowired
    private MessageHandler loggingHandler;

    @Autowired
    private AbstractRequestHandlerAdvice retryAdvice;

    @Autowired
    private ClientHttpRequestFactory clientHttpRequestFactory;

    @Bean
    public IntegrationFlow incomming() {
        return IntegrationFlows.from(inChannel)
                .<String, String>transform(String::toUpperCase)
                .filter(this::filterCondition, e -> e
                        .discardFlow(df -> df.channel(filterDiscardChannel)))
                .wireTap(p -> p
                        .transform(String.class, "Incomming: "::concat)
                        .handle(loggingHandler))
                .handle(Http.outboundChannelAdapter("https://api.github.com/users/jessbringlarsen/test")
                        .requestFactory(clientHttpRequestFactory)
                        .httpMethod(HttpMethod.GET)
                        .extractPayload(true)
                        .expectedResponseType(String.class)
                        .charset("UTF-8"),
                        e -> e
                         .advice(retryAdvice)
                )
                .channel(outChannel)
                .get();
    }

    private boolean filterCondition(Object payload) {
        return payload instanceof String &&
                !((String) payload).equalsIgnoreCase("test");
    }

    @Bean
    public IntegrationFlow outgoing() {
        return IntegrationFlows.from(outChannel)
                .transform(String.class, "outgoing channel: "::concat)
                .handle(loggingHandler)
                .get();
    }

    @Bean
    public IntegrationFlow discarded() {
        return IntegrationFlows.from(filterDiscardChannel)
                .transform(String.class, "discarded channel: "::concat)
                .handle(loggingHandler)
                .get();
    }
}
