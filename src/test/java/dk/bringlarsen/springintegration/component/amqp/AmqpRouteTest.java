package dk.bringlarsen.springintegration.component.amqp;

import dk.bringlarsen.springintegration.service.PJService;
import org.awaitility.Awaitility;
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

@SpringBootTest
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@ContextConfiguration(classes = { AmqpRoute.class, PJService.class})
public class AmqpRouteTest {

    @Autowired
    private PublishSubscribeChannel errChannel;

    @Autowired
    private DirectChannel inChannel;

    @Test
    public void t() throws IOException {
        final Deque results = new LinkedList();
        errChannel.subscribe(h -> results.add(h.getPayload()));

        inChannel.send(MessageBuilder.withPayload("http://www.dr.dk/fasd".getBytes()).build());

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> !results.isEmpty());

        System.out.println("sys");
    }
}