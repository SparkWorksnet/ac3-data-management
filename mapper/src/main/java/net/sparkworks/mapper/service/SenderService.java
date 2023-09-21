package net.sparkworks.mapper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SenderService {

    private static final String SLASH = "/";

    private final RabbitService rabbitService;

    public void send(String uri, int value) {
        rabbitService.sendMeasurement(uri, value, System.currentTimeMillis());
    }

}
