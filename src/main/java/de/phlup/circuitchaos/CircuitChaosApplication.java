package de.phlup.circuitchaos;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class CircuitChaosApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(CircuitChaosApplication.class).headless(false).run(args);
    }

}
