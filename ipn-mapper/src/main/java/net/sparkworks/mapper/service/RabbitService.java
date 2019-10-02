package net.sparkworks.mapper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitService {

    private static final String MESSAGE_TEMPLATE = "%s,%f,%d";
    private static final String DEBUG_SEND_FORMAT = "Sending to [%s,%s] %s";

    @Value("${rabbitmq.username}")
    private String clientId;

    @Value("${rabbitmq.queueSend}")
    private String rabbitQueueSend;

    private final RabbitTemplate rabbitTemplate;

    @Async
    public void sendMeasurement(final String uri, final Integer reading, final long timestamp) {
        sendMeasurement(uri, (double) reading, timestamp);
    }

    @Async
    public void sendMeasurement(final String uri, final Double reading, final long timestamp) {
        final String message = String.format(MESSAGE_TEMPLATE, clientId + "-" + uri, reading, timestamp);
        log.info(String.format(DEBUG_SEND_FORMAT, rabbitQueueSend, rabbitQueueSend, message));
        rabbitTemplate.send(rabbitQueueSend, rabbitQueueSend, new Message(message.getBytes(), new MessageProperties()));
    }

    private final ObjectMapper mapper = new ObjectMapper();

    //RabbitMQ listener for commands from SPARKS
    @RabbitListener(queues = "smartwork-ipn-mapper-commands")
    public void receiveCommandFromSparks(final Message message) {
        log.info("receiveCommandFromSparks '" + new String(message.getBody()) + "'");
    }

    //RabbitMQ listener for data from IPN Mouse
    @RabbitListener(queues = "test-queue", containerFactory = "serverB")
    public void receiveFromSmartWork(final Message message) {
        log.info("receiveFromSmartWork '" + new String(message.getBody()) + "'");
    }

}
