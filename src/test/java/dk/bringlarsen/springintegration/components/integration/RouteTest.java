package dk.bringlarsen.springintegration.components.integration;

import dk.bringlarsen.springintegration.service.FailingService;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.test.mail.TestMailServer;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { Route.class, FailingService.class, Config.class, HttpGate.class})
public class RouteTest {

    private static final TestMailServer.ImapServer imapServer = TestMailServer.imap(0);

    @Autowired
    private MessageChannel inChannel;

    @Before
    public void setup() throws InterruptedException {
        Awaitility.waitAtMost(5, TimeUnit.SECONDS).until(imapServer::isListening);
        assertTrue(imapServer.isListening());
    }

    @After
    public void teardown() {
        imapServer.stop();
    }

    @Test
    public void logMessage() {
        for(int i=0; i<5; i++) {
            new MessagingTemplate().convertAndSend(inChannel, "testing-" + i);
            Awaitility.await().atLeast(2000, TimeUnit.SECONDS);
        }
    }
}
