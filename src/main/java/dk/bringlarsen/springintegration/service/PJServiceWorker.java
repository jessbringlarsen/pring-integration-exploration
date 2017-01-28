package dk.bringlarsen.springintegration.service;

import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@EnableRetry
@Component
public class PJServiceWorker {

    @Retryable(value = {HttpClientErrorException.class}, maxAttempts = 3)
    public ResponseEntity<String> doExecute(String url) {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForEntity(url, String.class);
    }
}