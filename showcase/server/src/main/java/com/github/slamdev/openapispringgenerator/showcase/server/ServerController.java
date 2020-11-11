package com.github.slamdev.openapispringgenerator.showcase.server;

import com.github.slamdev.openapispringgenerator.showcase.server.api.NewPet;
import com.github.slamdev.openapispringgenerator.showcase.server.api.Pet;
import com.github.slamdev.openapispringgenerator.showcase.server.api.ServerApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class ServerController implements ServerApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolymorphismController.class);

    @Override
    public Pet addPet(NewPet body) {
        return findPetById(1L);
    }

    @Override
    public void deletePet(Long id) {
    }

    @Override
    public Pet findPetById(Long id) {
        return Pet.builder()
                .id(id)
                .name("aaa")
                .tag("test")
                .build();
    }

    @Override
    public List<Pet> findPets(Optional<List<String>> tags, Optional<Integer> limit) {
        return Collections.singletonList(findPetById(1L));
    }
}
