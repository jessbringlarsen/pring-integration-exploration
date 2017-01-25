package dk.bringlarsen.springintegration.component;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;

public class WireMockTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options()
        .port(8089));

    @Test
    public void testServer() throws IOException {
        stubFor(get(urlEqualTo("/api/person"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<response>Some content</response>")));

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> result = restTemplate.getForEntity("http://localhost:8089/api/person", String.class);

        assertEquals("<response>Some content</response>", result.getBody());
    }
}
