package dk.bringlarsen.springintegration.component.amqp;

import dk.bringlarsen.springintegration.service.PJService;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.integration.dsl.http.Http;
import org.springframework.integration.dsl.support.Transformers;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.router.ErrorMessageExceptionTypeRouter;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@EnableIntegration
@Configuration
public class AmqpRoute {

    private final String amqpInQueue = "inQueue";

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
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("somehost");
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        connectionFactory.setHost("localhost");

        return connectionFactory;
    }

    @Bean
    public IntegrationFlow in() {
        return IntegrationFlows.from(inChannel())
                .handle(Amqp.outboundAdapter(new RabbitTemplate(connectionFactory()))
                    .routingKey("inQueue"))
                .get();
    }

    @Bean
    public IntegrationFlow amqpIn(ConnectionFactory connectionFactory) {
        ErrorMessageExceptionTypeRouter errorMessageExceptionTypeRouter = new ErrorMessageExceptionTypeRouter();
        errorMessageExceptionTypeRouter.setLoggingEnabled(true);
        errorMessageExceptionTypeRouter.setDefaultOutputChannel(errChannel());

        return IntegrationFlows.from(Amqp.inboundAdapter(connectionFactory, amqpInQueue))
                .transform(Transformers.objectToString())
                .handle(Http.outboundChannelAdapter(p -> p.getPayload())
                                .httpMethod(HttpMethod.GET)
                                .expectedResponseType(String.class)
                                .charset("UTF-8"),
                        e -> e.advice(retryAdvice()))
                            .get();
    }

    @Bean
    public IntegrationFlow errorChannelIn() {
        return IntegrationFlows.from(errChannel())
                .handle(Amqp.outboundAdapter(new RabbitTemplate(connectionFactory())) // If gateway the message are not pulled off the in queue...
                        .defaultDeliveryMode(MessageDeliveryMode.PERSISTENT)
                        .mappedRequestHeaders("cause")  // Map header to string so it is readable from the Rabbit gui
                        .routingKey("errorChannel"))
                .get();
    }

    @Bean
    public AbstractRequestHandlerAdvice retryAdvice() {
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500);
        backOffPolicy.setMultiplier(2.0);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        RequestHandlerRetryAdvice requestHandlerRetryAdvice = new RequestHandlerRetryAdvice();
        requestHandlerRetryAdvice.setRetryTemplate(retryTemplate);
        requestHandlerRetryAdvice.setRecoveryCallback(new MyErrorMessageSendingRecover(errChannel()));
        return requestHandlerRetryAdvice;
    }
}
