package org.example.watchparty.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate for calling the backend's public movie API (MovieClient). Bounded connect/
 * read timeouts so a slow or unreachable backend fails a party-create fast instead of
 * parking the request thread — this is the service's only synchronous dependency on the
 * backend, and it's read-only (never a dual-write), so a failure just fails the create.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }
}
