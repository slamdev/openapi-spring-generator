package com.github.slamdev.openapispringgenerator.showcase.client;

import com.github.slamdev.openapispringgenerator.showcase.client.stream.ClientStreamProducer;
import com.github.slamdev.openapispringgenerator.showcase.client.stream.CompanyEvent;
import com.github.slamdev.openapispringgenerator.showcase.client.stream.UserEvent;
import com.github.slamdev.openapispringgenerator.showcase.server.api.ServerApi;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@RequiredArgsConstructor
@SpringBootApplication
public class Client implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

    private final ServerApi api;

    private final ClientStreamProducer stream;

    public static void main(String[] args) {
        SpringApplication.run(Client.class, args);
    }

    @Override
    public void run(String... args) {
        LOGGER.info("{}", api.findPetById(1L));
        stream.sendCompanies(CompanyEvent.builder().name("test").build(), UUID.randomUUID().toString());
        stream.sendUsers(UserEvent.builder().email("test@gmail.com").build(), UUID.randomUUID().toString());
    }
}
