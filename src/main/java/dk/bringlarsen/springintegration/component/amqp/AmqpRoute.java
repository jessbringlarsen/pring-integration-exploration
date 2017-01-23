package dk.bringlarsen.springintegration.component.amqp;

import dk.bringlarsen.springintegration.service.PJService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.integration.dsl.support.Transformers;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.handler.advice.RequestHandlerCircuitBreakerAdvice;
import org.springframework.messaging.Message;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@EnableIntegration
@Configuration
public class AmqpRoute {

    @Autowired
    private PJService pjService;

    @Bean
    public DirectChannel inChannel() {
        return new DirectChannel();
    }

    @Bean
    public DirectChannel httpOutChannel() {
        return new DirectChannel();
    }

    @Bean
    public PublishSubscribeChannel errChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public IntegrationFlow in(AmqpTemplate amqpTemplate) {
        return IntegrationFlows.from(inChannel())
                .handle(Amqp.outboundAdapter(amqpTemplate)
                    .routingKey("inQueue"))
                .get();
    }

    @Bean
    public IntegrationFlow amqpIn(ConnectionFactory connectionFactory) {
        return IntegrationFlows.from(Amqp.inboundAdapter(connectionFactory, "inQueue").errorChannel(errChannel()))
                .transform(Transformers.objectToString())
                .handle(Message.class, (p, h)-> pjService.execute(p))
                .route("headers['channel']")
               /* .handle(Http.outboundGateway(p -> p.getPayload())
                                .httpMethod(HttpMethod.GET),
                        e -> e.advice(retryAdvice())
                ) .enrichHeaders(Collections.singletonMap("header!", "value!"))*/
                .get();
    }

    @Bean
    public IntegrationFlow errorChannelIn(AmqpTemplate amqpTemplate) {
        return IntegrationFlows.from(errChannel())
                .wireTap(w -> w.handle(h -> System.out.println("***" + h.getHeaders().get("cause"))))
                .handle(Amqp.outboundGateway(amqpTemplate)
                        .routingKey("errorChannel"))
                .get();
    }

    @Bean
    public AbstractRequestHandlerAdvice retryAdvice() {
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(1);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000);
        backOffPolicy.setMultiplier(5.0);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        RequestHandlerCircuitBreakerAdvice requestHandlerCircuitBreakerAdvice = new RequestHandlerCircuitBreakerAdvice();
        requestHandlerCircuitBreakerAdvice.setThreshold(1);
        requestHandlerCircuitBreakerAdvice.setHalfOpenAfter(2000);
        return requestHandlerCircuitBreakerAdvice;
      /*  RequestHandlerRetryAdvice requestHandlerRetryAdvice = new RequestHandlerRetryAdvice();
        requestHandlerRetryAdvice.setRetryTemplate(retryTemplate);
        return requestHandlerRetryAdvice;
        */
    }
}
