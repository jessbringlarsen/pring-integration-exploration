package dk.bringlarsen.springintegration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.test.mail.TestMailServer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;

@SpringBootTest
@IntegrationComponentScan
@EnableIntegration
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DemoApplicationTests.Config.class)
public class DemoApplicationTests {

    private static final TestMailServer.ImapServer imapServer = TestMailServer.imap(0);

    @Autowired
    private MessageChannel inChannel;

    @Before
    public void setup() throws InterruptedException {
        int n = 0;
        while (n++ < 100 && !imapServer.isListening()) {
            Thread.sleep(100);
        }
        assertTrue(n < 100);
    }

    @After
    public void teardown() {
        imapServer.stop();
    }

    @Test
    public void logMessage() {
        new MessagingTemplate().convertAndSend(inChannel, "testing");
    }

    @Configuration
    public static class Config {

        @Bean
        public DirectChannel inChannel() {
            return new DirectChannel();
        }

        @Bean
        public DirectChannel outChannel() {
            return new DirectChannel();
        }

        @Bean
        public DirectChannel discardChannel() {
            return new DirectChannel();
        }

        @Bean
        public IntegrationFlow incomming() {
            return IntegrationFlows.from(inChannel())
                    .<String, String>transform(p -> p.toUpperCase())
                    .filter(p -> filterCondition(p) , e -> e
                            .discardFlow(df -> df.channel(discardChannel())))
                    .wireTap(p -> p
                            .transform(String.class, "Incomming: " ::concat)
                            .handle(loggingHandler()))
                    .channel(outChannel())
                    .get();
        }

        private boolean filterCondition(Object payload) {
            return payload instanceof String &&
                    !((String)payload).equalsIgnoreCase("test");
        }

        @Bean
        public IntegrationFlow outgoing() {
            return IntegrationFlows.from(outChannel())
                    .transform(String.class, "outgoing channel: " ::concat)
                    .handle(loggingHandler())
                    .get();
        }

        @Bean
        public IntegrationFlow discarded() {
            return IntegrationFlows.from(discardChannel())
                    .transform(String.class, "discarded channel: " ::concat)
                    .handle(loggingHandler())
                    .get();
        }

        @Bean
        public MessageHandler loggingHandler() {
            return new LoggingHandler("WARN");
        }
    }
}
