package net.sparkworks.mapper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sparkworks.mapper.model.DoubleValueReading;
import net.sparkworks.mapper.model.ImuValueReading;
import net.sparkworks.mapper.model.SingleValueReading;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitService {

    private static final String MESSAGE_TEMPLATE = "%s,%f,%d";
    private static final String DEBUG_SEND_FORMAT = "Sending to [%s,%s] %s";

    @Value("${spring.rabbitmq.username}")
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
    @RabbitListener(queues = "ipn-mouse-data-sparks", containerFactory = "serverB")
    public void receiveFromSmartWork(final Message message) throws IOException {
        log.info("receiveFromSmartWork routing key: {}", message.getMessageProperties().getReceivedRoutingKey());
        log.info("receiveFromSmartWork body: {}", new String(message.getBody()));
        String[] splitedRoutingKey = message.getMessageProperties().getReceivedRoutingKey().split("\\.");
        String dataType = splitedRoutingKey[1];
        String uri = splitedRoutingKey[0] + "/" + splitedRoutingKey[1];
        switch (dataType) {
            case "temperature":
            case "skinresponse":
            case "heartrate":
            case "gripforce":
            case "hesitation":
            case "frustration":
            case "neutral":
                SingleValueReading singleValueReading = mapper.readValue(message.getBody(), SingleValueReading.class);
                sendMeasurement(uri, singleValueReading.getReading(), singleValueReading.getTimestamp());
                break;
            case "imu":
                ImuValueReading imuValueReading = mapper.readValue(message.getBody(), ImuValueReading.class);
                sendMeasurement(uri + "/acelX", imuValueReading.getAcelX(), imuValueReading.getTimestamp());
                sendMeasurement(uri + "/acelY", imuValueReading.getAcelY(), imuValueReading.getTimestamp());
                sendMeasurement(uri + "/acelZ", imuValueReading.getAcelZ(), imuValueReading.getTimestamp());
                sendMeasurement(uri + "/gyroX", imuValueReading.getGyroX(), imuValueReading.getTimestamp());
                sendMeasurement(uri + "/gyroY", imuValueReading.getGyroY(), imuValueReading.getTimestamp());
                sendMeasurement(uri + "/gyroZ", imuValueReading.getGyroZ(), imuValueReading.getTimestamp());
                sendMeasurement(uri + "/cmpX", imuValueReading.getCmpX(), imuValueReading.getTimestamp());
                sendMeasurement(uri + "/cmpY", imuValueReading.getCmpY(), imuValueReading.getTimestamp());
                sendMeasurement(uri + "/cmpZ", imuValueReading.getCmpZ(), imuValueReading.getTimestamp());
                break;
            case "mousepos":
                DoubleValueReading doubleValueReading = mapper.readValue(message.getBody(), DoubleValueReading.class);
                sendMeasurement(uri + "/x", doubleValueReading.getX(), doubleValueReading.getTimestamp());
                sendMeasurement(uri + "/y", doubleValueReading.getY(), doubleValueReading.getTimestamp());
                break;
            default:
                log.error("No suitable mapper found for measurement \'{}\'.", dataType);
        }
        
    }

}
