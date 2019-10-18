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
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitService {
    
    private static final String MESSAGE_TEMPLATE = "%s,%f,%d";
    private static final String DEBUG_SEND_FORMAT = "Sending to [%s,%s] %s";
    
    private static final Set<String> SINGLE_VALUE_READING_DATA_TYPES = new HashSet<>(Arrays.asList(
            "temperature",
            "skinresponse",
            "heartrate",
            "gripforce",
            "hesitation",
            "frustration",
            "neutral"));
    
    private static final Set<String> DOUBLE_VALUE_READING_DATA_TYPES = new HashSet<>(Arrays.asList("mousepos"));
    
    private static final Set<String> IMU_DATA_TYPE = new HashSet<>(Arrays.asList("imu"));
    
    private static final Set<String> VALID_DATA_TYPES = Stream.of(SINGLE_VALUE_READING_DATA_TYPES, DOUBLE_VALUE_READING_DATA_TYPES, IMU_DATA_TYPE)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
    
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
    @RabbitListener(queues = "ipn-mouse-data-sparks", containerFactory = "serverB")
    public void receiveFromSmartWork(final Message message) {
        log.info("receiveFromSmartWork routing key: {}", message.getMessageProperties().getReceivedRoutingKey());
        log.info("receiveFromSmartWork body: {}", new String(message.getBody()));
        if (!isValidRoutingKey(message.getMessageProperties().getReceivedRoutingKey()) || !isValidBody(message.getBody())) {
            return;
        }
        try {
            sendReadings(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean isValidBody(byte[] body) {
        if (body.length == 0) {
            log.error("Empty body.");
            return false;
        }
        try {
            new ObjectMapper().readTree(body);
            return true;
        } catch (IOException e) {
            log.error("Invalid JSON: {}", new String(body));
            return false;
        }
    }
    
    private boolean isValidRoutingKey(String routingKey) {
        String[] splitRoutingKey = routingKey.split("\\.");
        if (splitRoutingKey.length < 2) {
            log.error("Invalid routing key: \'{}\'.", routingKey);
            return false;
        }
        if (!VALID_DATA_TYPES.contains(splitRoutingKey[1])) {
            log.error("Invalid data type \'{}\'.", splitRoutingKey[1]);
            return false;
        }
        return true;
    }
    
    private void sendReadings(Message message) throws Exception {
        String[] splitRoutingKey = message.getMessageProperties().getReceivedRoutingKey().split("\\.");
        String dataType = splitRoutingKey[1];
        String baseUri = splitRoutingKey[0] + "/" + splitRoutingKey[1];
        if (SINGLE_VALUE_READING_DATA_TYPES.contains(dataType)) {
            sendSingleValueReading(baseUri, message);
        } else if (IMU_DATA_TYPE.contains(dataType)) {
            sendImuValueReading(baseUri, message);
        } else if (DOUBLE_VALUE_READING_DATA_TYPES.contains(dataType)) {
            sendDoubleValueReading(baseUri, message);
        } else {
            log.error("No suitable mapper found for routing key \'{}\'.", message.getMessageProperties().getReceivedRoutingKey());
        }
    }
    
    private void sendSingleValueReading(String baseUri, Message message) throws Exception {
        SingleValueReading singleValueReading = mapper.readValue(message.getBody(), SingleValueReading.class);
        if (hasNullField(singleValueReading)) {
            return;
        }
        sendMeasurement(baseUri, singleValueReading.getReading(), singleValueReading.getTimestamp());
    }
    
    private void sendImuValueReading(String baseUri, Message message) throws Exception {
        ImuValueReading imuValueReading = mapper.readValue(message.getBody(), ImuValueReading.class);
        if (hasNullField(imuValueReading)) {
            return;
        }
        sendMeasurement(baseUri + "/acelX", imuValueReading.getAcelX(), imuValueReading.getTimestamp());
        sendMeasurement(baseUri + "/acelY", imuValueReading.getAcelY(), imuValueReading.getTimestamp());
        sendMeasurement(baseUri + "/acelZ", imuValueReading.getAcelZ(), imuValueReading.getTimestamp());
        sendMeasurement(baseUri + "/gyroX", imuValueReading.getGyroX(), imuValueReading.getTimestamp());
        sendMeasurement(baseUri + "/gyroY", imuValueReading.getGyroY(), imuValueReading.getTimestamp());
        sendMeasurement(baseUri + "/gyroZ", imuValueReading.getGyroZ(), imuValueReading.getTimestamp());
        sendMeasurement(baseUri + "/cmpX", imuValueReading.getCmpX(), imuValueReading.getTimestamp());
        sendMeasurement(baseUri + "/cmpY", imuValueReading.getCmpY(), imuValueReading.getTimestamp());
        sendMeasurement(baseUri + "/cmpZ", imuValueReading.getCmpZ(), imuValueReading.getTimestamp());
    }
    
    private void sendDoubleValueReading(String baseUri, Message message) throws Exception {
        DoubleValueReading doubleValueReading = mapper.readValue(message.getBody(), DoubleValueReading.class);
        if (hasNullField(doubleValueReading)) {
            return;
        }
        sendMeasurement(baseUri + "/x", doubleValueReading.getX(), doubleValueReading.getTimestamp());
        sendMeasurement(baseUri + "/y", doubleValueReading.getY(), doubleValueReading.getTimestamp());
    }
    
    private boolean hasNullField(Object valueReading) throws IllegalAccessException {
        Set<String> nullFields = new HashSet<>();
        for (Field f : valueReading.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.get(valueReading) == null) {
                nullFields.add(f.getName());
            }
        }
        if (nullFields.size() > 0) {
            log.error("Field{} {} are null.", nullFields.size() > 1 ? "s" : "", String.join(", ", nullFields));
            return true;
        }
        return false;
    }
    
}
