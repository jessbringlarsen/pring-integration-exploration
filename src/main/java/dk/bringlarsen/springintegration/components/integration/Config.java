package dk.bringlarsen.springintegration.components.integration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.handler.advice.RequestHandlerCircuitBreakerAdvice;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.integration.http.inbound.RequestMapping;
import org.springframework.integration.router.ErrorMessageExceptionTypeRouter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.backoff.SleepingBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.backoff.BackOff;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.xml.ws.Response;

@Configuration
@EnableIntegration
public class Config {

    @Bean
    public DirectChannel inChannel() {
        return new DirectChannel();
    }

    @Bean
    public DirectChannel outChannel() {
        return new DirectChannel();
    }

    @Bean
    public DirectChannel errChannel() {
        return new DirectChannel();
    }

    @Bean
    public DirectChannel filterDiscardChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageHandler loggingHandler() {
        return new LoggingHandler("WARN");
    }

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setReadTimeout(2000);
        clientHttpRequestFactory.setConnectTimeout(2000);
        return clientHttpRequestFactory;
    }

    @Bean
    public AbstractRequestHandlerAdvice retryAdvice() {
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(4);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000);
        backOffPolicy.setMultiplier(5.0);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

//        RequestHandlerCircuitBreakerAdvice requestHandlerCircuitBreakerAdvice = new RequestHandlerCircuitBreakerAdvice();
//        requestHandlerCircuitBreakerAdvice.setThreshold(1);
//        requestHandlerCircuitBreakerAdvice.setHalfOpenAfter(2000);
//        return requestHandlerCircuitBreakerAdvice;
        RequestHandlerRetryAdvice requestHandlerRetryAdvice = new RequestHandlerRetryAdvice();
        requestHandlerRetryAdvice.setRetryTemplate(retryTemplate);
        return requestHandlerRetryAdvice;
    }

    @Bean
    public MessageChannel requestChannel() {
        return new DirectChannel();
    }
}