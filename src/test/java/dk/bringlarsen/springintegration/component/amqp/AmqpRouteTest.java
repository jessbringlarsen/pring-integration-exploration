package dk.bringlarsen.springintegration.component.amqp;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import dk.bringlarsen.springintegration.service.PJService;
import dk.bringlarsen.springintegration.service.PJServiceWorker;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@ContextConfiguration(classes = { AmqpRoute.class, PJService.class, PJServiceWorker.class})
public class AmqpRouteTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options()
            .port(8089));

    @Autowired
    private PublishSubscribeChannel errChannel;

    @Autowired
    private DirectChannel inChannel;

    /**
     * We do now expect to see an entry on the error channel as a client exception should
     * cause the failing message to remain on the in queue
     * @throws IOException
     */
    @Test
    public void testClientException() throws IOException {
        setupClientSideErrorResponse();

        final Deque results = new LinkedList();
        errChannel.subscribe(results::add);

        inChannel.send(MessageBuilder.withPayload("http://localhost:8089/api/person".getBytes()).build());

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> results.isEmpty());
    }

    private void setupClientSideErrorResponse() {
        stubFor(get(urlEqualTo("/api/person"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("<response>Some content</response>")));

    }

    /**
     * If we see server side exceptions we should send the message to the error queue
     * @throws IOException
     */
    @Test
    public void testServerSideException() throws IOException {
        setupServerSideErrorResponse();

        final Deque results = new LinkedList();
        errChannel.subscribe(results::add);

        inChannel.send(MessageBuilder.withPayload("http://localhost:8089/api/person".getBytes()).build());

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !results.isEmpty());
    }

    private void setupServerSideErrorResponse() {
        stubFor(get(urlEqualTo("/api/person"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("<response>Some content</response>")));

    }
}