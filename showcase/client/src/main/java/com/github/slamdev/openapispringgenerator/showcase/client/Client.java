package com.github.slamdev.openapispringgenerator.showcase.client;

import com.github.slamdev.openapispringgenerator.showcase.server.api.ServerApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Client implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

    private final ServerApi api;

    public Client(ServerApi api) {
        this.api = api;
    }

    public static void main(String[] args) {
        SpringApplication.run(Client.class, args);
    }

    @Override
    public void run(String... args) {
        LOGGER.info("{}", api.findPetById(1L));
    }
}
