package com.github.slamdev.openapispringgenerator.showcase.server;

import com.github.slamdev.openapispringgenerator.showcase.client.stream.ClientStreamConsumer;
import com.github.slamdev.openapispringgenerator.showcase.client.stream.CompanyEvent;
import com.github.slamdev.openapispringgenerator.showcase.client.stream.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventsConsumer implements ClientStreamConsumer.Companies, ClientStreamConsumer.Users {

    @Override
    public void processCompanies(CompanyEvent event, MessageHeaders headers) {
        log.info("company {}", event);
    }

    @Override
    public void processUsers(UserEvent event, MessageHeaders headers) {
        log.info("user {}", event);
    }
}
