package de.phlup.circuitchaos.common.config;

import de.phlup.circuitchaos.common.settings.ClientSettings;
import org.springframework.boot.autoconfigure.web.client.RestClientSsl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@SuppressWarnings("unused")
public class RestClientConfiguration {

    @Bean(name = "restClient")
    @Profile("ssl")
    public RestClient restClientSsl(RestClient.Builder restClientBuilder, RestClientSsl ssl, ClientSettings settings) {
        return RestClient.builder()
                         .apply(ssl.fromBundle(settings.getSslBundle()))
                         .build();
    }

    @Bean(name = "restClient")
    @Profile("!ssl")
    public RestClient restClientNoSsl(RestClient.Builder restClientBuilder) {
        return RestClient.builder().build();
    }

}
