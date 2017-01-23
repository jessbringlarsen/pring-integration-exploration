package dk.bringlarsen.springintegration.service;

import org.springframework.http.ResponseEntity;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class PJService {

    /**
     * For auth config: http://www.baeldung.com/how-to-use-resttemplate-with-basic-authentication-in-spring
     * @param incomming
     * @return
     */
    @ServiceActivator
    public Message<String> execute(Message<String> incomming) {
        MessageBuilder<String> replyMessageBuilder = MessageBuilder.fromMessage(incomming);

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(incomming.getPayload(), String.class);
            if(response.getStatusCodeValue() != 200) {
                replyMessageBuilder.setHeader("channel", "errChannel");
            }
        } catch (RestClientException e) {
            replyMessageBuilder
                    .setHeader("channel", "errChannel")
                    .setHeader("cause", e);
        }
        return replyMessageBuilder.build();
    }

    //@Retryable(value = HttpClientErrorException.class, maxAttempts = 2)
    private ResponseEntity<String> doExecute(String url) {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForEntity(url, String.class);
    }
}
