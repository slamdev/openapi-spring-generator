package com.github.slamdev.openapispringgenerator.showcase.server;

import com.github.slamdev.openapispringgenerator.showcase.server.api.HomePet;
import com.github.slamdev.openapispringgenerator.showcase.server.api.PolymorphismApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PolymorphismController implements PolymorphismApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolymorphismController.class);

    @Override
    public void allof(Optional<HomePet> body) {
        LOGGER.info("allof: {}", body);
    }
}
