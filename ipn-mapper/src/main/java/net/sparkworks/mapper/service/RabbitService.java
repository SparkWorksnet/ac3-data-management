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

    private static final long TICKS_AT_EPOCH = 62135596800000L;
    private static final long TICKS_PER_MILLISECOND = 10000;

    private static final String MESSAGE_TEMPLATE = "%s,%f,%d";
    private static final String DEBUG_SEND_FORMAT = "Sending [skinresponse: %f] to [%s,%s] %s";
    private static final String QUEUE_DATA_V1 = "${rabbitmq.serverB.queueData}";
    private static final String QUEUE_DATA_V2 = "${rabbitmq.serverB.queueData2}";
    private static final String QUEUE_COMMAND = "${rabbitmq.queueCommands}";
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, Double> lastSkinResponseValue = new HashMap<>();
    private static final Set<String> SINGLE_VALUE_READING_DATA_TYPES = new HashSet<>(Arrays.asList("temperature", "skinresponse", "heartrate", "gripforce", "hesitation", "frustration", "neutral"));

    private static final Set<String> DOUBLE_VALUE_READING_DATA_TYPES = new HashSet<>(Arrays.asList("mousepos"));
    
    private static final Set<String> IMU_DATA_TYPE = new HashSet<>(Arrays.asList("imu"));

    private static final Set<String> VALID_DATA_TYPES = Stream.of(SINGLE_VALUE_READING_DATA_TYPES, DOUBLE_VALUE_READING_DATA_TYPES, IMU_DATA_TYPE).flatMap(Set::stream).collect(Collectors.toSet());

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
        if (uri.endsWith("skinresponse")) {
            lastSkinResponseValue.put(deviceName, reading);
        }
        if (lastSkinResponseValue.containsKey(deviceName) && lastSkinResponseValue.get(deviceName) > 5) {
            final String message = String.format(MESSAGE_TEMPLATE, uriPrefix + "-" + uri, reading, timestamp);
            log.info(String.format(DEBUG_SEND_FORMAT, lastSkinResponseValue.get(deviceName), rabbitQueueSend, rabbitQueueSend, message));
            rabbitTemplate.send(rabbitQueueSend, rabbitQueueSend, new Message(message.getBytes(), new MessageProperties()));
        } else {
            final String message = String.format(MESSAGE_TEMPLATE, uriPrefix + "-" + uri, reading, timestamp);
            log.warn(String.format("Will not send the following measurement: " + DEBUG_SEND_FORMAT, lastSkinResponseValue.get(deviceName), rabbitQueueSend, rabbitQueueSend, message));
        }
    }

    //RabbitMQ listener for commands from SPARKS
    @RabbitListener(queues = QUEUE_COMMAND)
    public void receiveCommandFromSparks(final Message message) {
        log.info("receiveCommandFromSparks '" + new String(message.getBody()) + "'");
    }

    //RabbitMQ listener for data from IPN Mouse - data in format 1
    @RabbitListener(queues = QUEUE_DATA_V1, containerFactory = "serverB")
    public void receiveFromSmartWorkV1(final Message message) {
        log.debug("received routing key: {} body: {}", message.getMessageProperties().getReceivedRoutingKey(), new String(message.getBody()));
        if (!isValidRoutingKeyV1(message.getMessageProperties().getReceivedRoutingKey())) {
            log.error("invalid routing key: {}", message.getMessageProperties().getReceivedRoutingKey());
            return;
        } else if (!isValidBodyV1(message.getBody())) {
            log.error("invalid body: {}", new String(message.getBody()));
            return;
        }
        try {
            sendReadingsV1(message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    //RabbitMQ listener for data from IPN Mouse - data in format 2
    @RabbitListener(queues = QUEUE_DATA_V2, containerFactory = "serverB")
    public void receiveFromSmartWorkV2(final Message message) {
        log.info("received routing key2: {} body: {}", message.getMessageProperties().getReceivedRoutingKey(), new String(message.getBody()));
        if (!isValidRoutingKeyV2(message.getMessageProperties().getReceivedRoutingKey())) {
            log.error("invalid routing key2: {}", message.getMessageProperties().getReceivedRoutingKey());
            return;
        } else if (!isValidBodyV2(message.getBody())) {
            log.error("invalid body2: {}", new String(message.getBody()));
            return;
        }
        try {
            sendReadingsV2(message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private boolean isValidBodyV1(final byte[] body) {
        return isValidBody(body);
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

    private boolean isValidRoutingKeyV1(final String routingKey) {
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

    private void sendReadingsV1(final Message message) throws IllegalAccessException, IOException {
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

    private boolean isValidBodyV2(final byte[] body) {
        return isValidBody(body);
    }

    private boolean isValidRoutingKeyV2(final String routingKey) {
        String[] splitRoutingKey = routingKey.split("\\.");
        if (splitRoutingKey.length < 3) {
            log.error("Invalid routing key: \'{}\'.", routingKey);
            return false;
        }
        if (!VALID_DATA_TYPES.contains(splitRoutingKey[2])) {
            log.error("Invalid data type \'{}\'.", splitRoutingKey[2]);
            return false;
        }
        return true;
    }

    private void sendReadingsV2(final Message message) throws IllegalAccessException, IOException {
        String[] splitRoutingKey = message.getMessageProperties().getReceivedRoutingKey().split("\\.");
        String dataType = splitRoutingKey[2];
        String baseUri = splitRoutingKey[0] + "/" + splitRoutingKey[2];
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
        final SingleValueReading singleValueReading = mapper.readValue(message.getBody(), SingleValueReading.class);
        long epochTime = singleValueReading.getTimestamp() - TICKS_AT_EPOCH;
        if (hasNullField(singleValueReading)) {
            log.error("baseUri: {}, null field {}", baseUri, singleValueReading);
            return;
        }
        sendMeasurement(baseUri, singleValueReading.getReading(), epochTime);
    }

    private void sendImuValueReading(final String baseUri, final Message message) throws IllegalAccessException, IOException {
        final ImuValueReading imuValueReading = mapper.readValue(message.getBody(), ImuValueReading.class);
        long epochTime = imuValueReading.getTimestamp() - TICKS_AT_EPOCH;
        if (hasNullField(imuValueReading)) {
            log.error("baseUri: {}, null field {}", baseUri, imuValueReading);
            return;
        }
        sendMeasurement(baseUri + "/acelX", imuValueReading.getAcelX(), epochTime);
        sendMeasurement(baseUri + "/acelY", imuValueReading.getAcelY(), epochTime);
        sendMeasurement(baseUri + "/acelZ", imuValueReading.getAcelZ(), epochTime);
        sendMeasurement(baseUri + "/gyroX", imuValueReading.getGyroX(), epochTime);
        sendMeasurement(baseUri + "/gyroY", imuValueReading.getGyroY(), epochTime);
        sendMeasurement(baseUri + "/gyroZ", imuValueReading.getGyroZ(), epochTime);
//        TODO: disabled for now
//        sendMeasurement(baseUri + "/cmpX", imuValueReading.getCmpX(), imuValueReading.getTimestamp());
//        sendMeasurement(baseUri + "/cmpY", imuValueReading.getCmpY(), imuValueReading.getTimestamp());
//        sendMeasurement(baseUri + "/cmpZ", imuValueReading.getCmpZ(), imuValueReading.getTimestamp());
    }

    private void sendDoubleValueReading(String baseUri, Message message) throws IllegalAccessException, IOException {
        final DoubleValueReading doubleValueReading = mapper.readValue(message.getBody(), DoubleValueReading.class);
        long epochTime = doubleValueReading.getTimestamp() - TICKS_AT_EPOCH;
        if (hasNullField(doubleValueReading)) {
            log.error("baseUri: {}, null field {}", baseUri, doubleValueReading);
            return;
        }
        sendMeasurement(baseUri + "/x", doubleValueReading.getX(), epochTime);
        sendMeasurement(baseUri + "/y", doubleValueReading.getY(), epochTime);
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
