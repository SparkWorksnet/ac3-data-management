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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitService {
    
    private static final String MESSAGE_TEMPLATE = "%s,%f,%d";
    private static final String DEBUG_SEND_FORMAT = "Sending [lastTemp: %f] to [%s,%s] %s";
    private static final String QUEUE_DATA = "${rabbitmq.serverB.queueData}";
    private static final String QUEUE_COMMAND = "${rabbitmq.queueCommands}";
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, Double> lastTemperatureValue = new HashMap<>();
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
    
    @Value("${rabbitmq.uriPrefix}")
    private String uriPrefix;

    @Value("${rabbitmq.queueSend}")
    private String rabbitQueueSend;

    private final RabbitTemplate rabbitTemplate;

    @Async
    public void sendMeasurement(final String uri, final Integer reading, final long timestamp) {
        sendMeasurement(uri, (double) reading, timestamp);
    }

    @Async
    public void sendMeasurement(final String uri, final Double reading, final long timestamp) {
        final String deviceName = uri.split("/")[0];
        if (uri.endsWith("temperature")) {
            lastTemperatureValue.put(deviceName, reading);
        }
        if (lastTemperatureValue.containsKey(deviceName) && lastTemperatureValue.get(deviceName) > 30) {
            final String message = String.format(MESSAGE_TEMPLATE, uriPrefix + "-" + uri, reading, timestamp);
            log.info(String.format(DEBUG_SEND_FORMAT, lastTemperatureValue.get(deviceName), rabbitQueueSend, rabbitQueueSend, message));
            rabbitTemplate.send(rabbitQueueSend, rabbitQueueSend, new Message(message.getBytes(), new MessageProperties()));
        } else {
            final String message = String.format(MESSAGE_TEMPLATE, uriPrefix + "-" + uri, reading, timestamp);
            log.warn(String.format("Will not send the following measurement: " + DEBUG_SEND_FORMAT, lastTemperatureValue.get(deviceName), rabbitQueueSend, rabbitQueueSend, message));
        }
    }

    //RabbitMQ listener for commands from SPARKS
    @RabbitListener(queues = QUEUE_COMMAND)
    public void receiveCommandFromSparks(final Message message) {
        log.info("receiveCommandFromSparks '" + new String(message.getBody()) + "'");
    }

    //RabbitMQ listener for data from IPN Mouse
    @RabbitListener(queues = QUEUE_DATA, containerFactory = "serverB")
    public void receiveFromSmartWork(final Message message) {
        log.debug("received routing key: {} body: {}", message.getMessageProperties().getReceivedRoutingKey(), new String(message.getBody()));
        if (!isValidRoutingKey(message.getMessageProperties().getReceivedRoutingKey())) {
            log.error("invalid routing key: {}", message.getMessageProperties().getReceivedRoutingKey());
            return;
        } else if (!isValidBody(message.getBody())) {
            log.error("invalid body: {}", new String(message.getBody()));
            return;
        }
        try {
            sendReadings(message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private boolean isValidBody(final byte[] body) {
        if (body.length == 0) {
            log.error("Empty body.");
            return false;
        }
        try {
            mapper.readTree(body);
            return true;
        } catch (IOException e) {
            log.error("Invalid JSON: {}", new String(body));
            return false;
        }
    }

    private boolean isValidRoutingKey(final String routingKey) {
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

    private void sendReadings(final Message message) throws IllegalAccessException, IOException {
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

    private void sendSingleValueReading(final String baseUri, final Message message) throws IllegalAccessException, IOException {
        final long now = System.currentTimeMillis();
        final SingleValueReading singleValueReading = mapper.readValue(message.getBody(), SingleValueReading.class);
        if (hasNullField(singleValueReading)) {
            log.error("baseUri: {}, null field {}", baseUri, singleValueReading);
            return;
        }
        sendMeasurement(baseUri, singleValueReading.getReading(), now);
    }

    private void sendImuValueReading(final String baseUri, final Message message) throws IllegalAccessException, IOException {
        final long now = System.currentTimeMillis();
        final ImuValueReading imuValueReading = mapper.readValue(message.getBody(), ImuValueReading.class);
        if (hasNullField(imuValueReading)) {
            log.error("baseUri: {}, null field {}", baseUri, imuValueReading);
            return;
        }
        sendMeasurement(baseUri + "/acelX", imuValueReading.getAcelX(), now);
        sendMeasurement(baseUri + "/acelY", imuValueReading.getAcelY(), now);
        sendMeasurement(baseUri + "/acelZ", imuValueReading.getAcelZ(), now);
        sendMeasurement(baseUri + "/gyroX", imuValueReading.getGyroX(), now);
        sendMeasurement(baseUri + "/gyroY", imuValueReading.getGyroY(), now);
        sendMeasurement(baseUri + "/gyroZ", imuValueReading.getGyroZ(), now);
//        TODO: disabled for now
//        sendMeasurement(baseUri + "/cmpX", imuValueReading.getCmpX(), imuValueReading.getTimestamp());
//        sendMeasurement(baseUri + "/cmpY", imuValueReading.getCmpY(), imuValueReading.getTimestamp());
//        sendMeasurement(baseUri + "/cmpZ", imuValueReading.getCmpZ(), imuValueReading.getTimestamp());
    }

    private void sendDoubleValueReading(String baseUri, Message message) throws IllegalAccessException, IOException {
        final long now = System.currentTimeMillis();
        final DoubleValueReading doubleValueReading = mapper.readValue(message.getBody(), DoubleValueReading.class);
        if (hasNullField(doubleValueReading)) {
            log.error("baseUri: {}, null field {}", baseUri, doubleValueReading);
            return;
        }
        sendMeasurement(baseUri + "/x", doubleValueReading.getX(), now);
        sendMeasurement(baseUri + "/y", doubleValueReading.getY(), now);
    }
    
    private boolean hasNullField(final Object valueReading) throws IllegalAccessException {
        final Set<String> nullFields = new HashSet<>();
        for (final Field f : valueReading.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.get(valueReading) == null) {
                nullFields.add(f.getName());
            }
        }
        if (!nullFields.isEmpty()) {
            log.error("null fields [{}]", String.join(", ", nullFields));
            return true;
        }
        return false;
    }
    
}
