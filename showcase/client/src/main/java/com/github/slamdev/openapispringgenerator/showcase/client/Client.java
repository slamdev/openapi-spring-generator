package com.github.slamdev.openapispringgenerator.showcase.client;

import com.github.slamdev.openapispringgenerator.showcase.server.api.ServerApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Client implements CommandLineRunner {

    private final ServerApi api;

    public Client(ServerApi api) {
        this.api = api;
    }

    public static void main(String[] args) {
        SpringApplication.run(Client.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println(api.findPetById(1L));
    }
}
