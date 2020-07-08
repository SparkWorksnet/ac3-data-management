package net.sparkworks.mapper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sparkworks.mapper.model.DoubleValueReading;
import net.sparkworks.mapper.model.ImuValueReading;
import net.sparkworks.mapper.model.SingleValueReading;
import net.sparkworks.mapper.model.input.MouseDataWrapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitService {

    private static final long TICKS_AT_EPOCH = 62135596800000L;
    private static final long TICKS_PER_MILLISECOND = 10000;

    private static final String MESSAGE_TEMPLATE = "%s,%f,%d";
    private static final String DEBUG_FORMAT = "to [%s,%s] %s";
    private static final String DEBUG_SEND_FORMAT = "Sending: " + DEBUG_FORMAT;
    private static final String DEBUG_NOT_SEND_FORMAT = "Will not process the following measurement: " + DEBUG_FORMAT;
    private static final String QUEUE_DATA_V1 = "${rabbitmq.serverB.queueData}";
    private static final String QUEUE_DATA_V2 = "${rabbitmq.serverB.queueData2}";
    private static final String QUEUE_DATA_V3 = "${rabbitmq.serverB.queueData3}";
    private static final String QUEUE_COMMAND = "${rabbitmq.queueCommands}";

    @Value("${mapper.skinresponse.threshold}")
    private Double skinResponseThreshold;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, Double> lastSkinResponseValue = new HashMap<>();
    private static final Set<String> SINGLE_VALUE_READING_DATA_TYPES = new HashSet<>(Arrays.asList("temperature", "skinresponse", "heartrate", "gripforce", "hesitation", "frustration", "neutral"));

    private static final Set<String> DOUBLE_VALUE_READING_DATA_TYPES = new HashSet<>(Collections.singletonList("mousepos"));
    
    private static final Set<String> IMU_DATA_TYPE = new HashSet<>(Collections.singletonList("imu"));

    private static final Set<String> VALID_DATA_TYPES = Stream.of(SINGLE_VALUE_READING_DATA_TYPES, DOUBLE_VALUE_READING_DATA_TYPES, IMU_DATA_TYPE).flatMap(Set::stream).collect(Collectors.toSet());

    @Value("${rabbitmq.uriPrefix}")
    private String uriPrefix;

    @Value("${rabbitmq.queueSend}")
    private String rabbitQueueSend;

    private final RabbitTemplate rabbitTemplate;

    private final ResourceService resourceService;
    
    private final MeterRegistry meterRegistry;
    
    private Counter inputCounterV1, inputCounterV2, inputCounterV3, outputCounter;
    
    @PostConstruct
    public void init(){
        inputCounterV1 = Counter.builder("ipn-mapper.input.messages")
                .tag("format", "1")
                .description("format 1 input messages")
                .register(meterRegistry);
        inputCounterV2 = Counter.builder("ipn-mapper.input.messages")
                .tag("format", "2")
                .description("format 2 input messages")
                .register(meterRegistry);
        inputCounterV3 = Counter.builder("ipn-mapper.input.messages")
                .tag("format", "3")
                .description("format 3 input messages")
                .register(meterRegistry);
        outputCounter = Counter.builder("ipn-mapper.output.messages")
                .description("Output messages")
                .register(meterRegistry);
    }

    public Collection<String> sendMeasurement(final String uri, final Integer reading, final long timestamp) {
        return sendMeasurement(uri, (double) reading, timestamp);
    }

    public Collection<String> sendMeasurement(final String uri, final Double reading, final long timestamp) {
        outputCounter.increment();
        
        final String message = String.format(MESSAGE_TEMPLATE, uriPrefix + "-" + uri, reading, timestamp);
        log.debug(String.format(DEBUG_SEND_FORMAT, rabbitQueueSend, rabbitQueueSend, message));
        rabbitTemplate.send(rabbitQueueSend, rabbitQueueSend, new Message(message.getBytes(), new MessageProperties()));
        return Collections.singletonList(uriPrefix + "-" + uri);
    }

    //RabbitMQ listener for commands from SPARKS
    @RabbitListener(queues = QUEUE_COMMAND)
    public void receiveCommandFromSparks(final Message message) {
        log.info("[{}] command '{}'", QUEUE_COMMAND, new String(message.getBody()));
    }

    //RabbitMQ listener for data from IPN Mouse - data in format 1
    @RabbitListener(queues = QUEUE_DATA_V1, containerFactory = "serverB")
    public void receiveFromSmartWorkV1(final Message message) {
        inputCounterV1.increment();
        
        log.debug("[{}] routingKey:{} body:{}", QUEUE_DATA_V1, message.getMessageProperties().getReceivedRoutingKey(), new String(message.getBody()));
        if (!isValidRoutingKeyV1(message.getMessageProperties().getReceivedRoutingKey())) {
            log.error("[{}] invalid routingKey:{}", QUEUE_DATA_V1, message.getMessageProperties().getReceivedRoutingKey());
            return;
        } else if (!isValidBodyV1(message.getBody())) {
            log.error("[{}] invalid body:{}", QUEUE_DATA_V1, new String(message.getBody()));
            return;
        }
        try {
            sendReadingsV1(message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private boolean isValidRoutingKeyV1(final String routingKey) {
        String[] splitRoutingKey = routingKey.split("\\.");
        if (splitRoutingKey.length < 2) {
            log.error("Invalid routing key: '{}'.", routingKey);
            return false;
        }
        if (!VALID_DATA_TYPES.contains(splitRoutingKey[1])) {
            log.error("Invalid data type '{}'.", splitRoutingKey[1]);
            return false;
        }
        return true;
    }

    private boolean isValidBodyV1(final byte[] body) {
        return isValidBody(body);
    }

    private void sendReadingsV1(final Message message) throws IllegalAccessException, IOException {
        final String[] splitRoutingKey = message.getMessageProperties().getReceivedRoutingKey().split("\\.");
        final String dataType = splitRoutingKey[1];
        final String baseUri = splitRoutingKey[0] + "/" + splitRoutingKey[1];
        sendReadings(dataType, baseUri, message);
    }

    //RabbitMQ listener for data from IPN Mouse - data in format 2
    @RabbitListener(queues = QUEUE_DATA_V2, containerFactory = "serverB")
    public void receiveFromSmartWorkV2(final Message message) {
        inputCounterV2.increment();
        
        log.debug("[{}] routingKey:{} body:{}", QUEUE_DATA_V2, message.getMessageProperties().getReceivedRoutingKey(), new String(message.getBody()));
        if (!isValidRoutingKeyV2(message.getMessageProperties().getReceivedRoutingKey())) {
            log.error("[{}] invalid routingKey:{}", QUEUE_DATA_V2, message.getMessageProperties().getReceivedRoutingKey());
            return;
        } else if (!isValidBodyV2(message.getBody())) {
            log.error("[{}] invalid body:{}", QUEUE_DATA_V2, new String(message.getBody()));
            return;
        }
        try {
            sendReadingsV2(message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private boolean isValidRoutingKeyV2(final String routingKey) {
        String[] splitRoutingKey = routingKey.split("\\.");
        if (splitRoutingKey.length < 3) {
            log.error("Invalid routing key: '{}'.", routingKey);
            return false;
        }
        if (!VALID_DATA_TYPES.contains(splitRoutingKey[2])) {
            log.error("Invalid data type '{}'.", splitRoutingKey[2]);
            return false;
        }
        return true;
    }

    private boolean isValidBodyV2(final byte[] body) {
        return isValidBody(body);
    }

    private void sendReadingsV2(final Message message) throws IllegalAccessException, IOException {
        final String[] splitRoutingKey = message.getMessageProperties().getReceivedRoutingKey().split("\\.");
        final String dataType = splitRoutingKey[2];
        final String baseUri = splitRoutingKey[0] + "/" + splitRoutingKey[2];
        Collection<String> systemNames = sendReadings(dataType, baseUri, message);
        try {
            final UUID groupUuid = UUID.fromString(splitRoutingKey[1]);
            resourceService.placeInCorrectGroup(groupUuid, systemNames);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
    
    //RabbitMQ listener for data from IPN Mouse - data in format 3
    @RabbitListener(queues = QUEUE_DATA_V3, containerFactory = "serverB")
    public void receiveFromSmartWorkV3(final Message message) {
        inputCounterV3.increment();
    
        log.debug("[{}] routingKey:{} body:{}", QUEUE_DATA_V3, message.getMessageProperties().getReceivedRoutingKey(), new String(message.getBody()));
        if (!isValidRoutingKeyV3(message.getMessageProperties().getReceivedRoutingKey())) {
            log.error("[{}] invalid routingKey:{}", QUEUE_DATA_V3, message.getMessageProperties().getReceivedRoutingKey());
            return;
        } else {
            try {
                sendReadingsV3(message);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            return;
        }
    }
    
    private boolean isValidRoutingKeyV3(final String routingKey) {
        String[] splitRoutingKey = routingKey.split("\\.");
        if (splitRoutingKey.length < 2) {
            log.error("Invalid routing key: '{}'.", routingKey);
            return false;
        }
        return true;
    }
    
    private void sendReadingsV3(final Message message) throws IOException, IllegalAccessException {
        final String[] splitRoutingKey = message.getMessageProperties().getReceivedRoutingKey().split("\\.");
        final String baseUri = splitRoutingKey[0];
        Collection<String> systemNames = sendReadings3(baseUri, message);
        try {
            final UUID groupUuid = UUID.fromString(splitRoutingKey[1]);
            resourceService.placeInCorrectGroup(groupUuid, systemNames);
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

    private Collection<String> sendReadings(final String dataType, final String baseUri, final Message message) throws IllegalAccessException, IOException {
        if (SINGLE_VALUE_READING_DATA_TYPES.contains(dataType)) {
            return sendSingleValueReading(baseUri, message);
        } else if (IMU_DATA_TYPE.contains(dataType)) {
            return sendImuValueReading(baseUri, message);
        } else if (DOUBLE_VALUE_READING_DATA_TYPES.contains(dataType)) {
            return sendDoubleValueReading(baseUri, message);
        } else {
            log.error("No suitable mapper found for routing key '{}'.", message.getMessageProperties().getReceivedRoutingKey());
            return Collections.emptySet();
        }
    }
    
    private Collection<String> sendReadings3(final String baseUri, final Message message) throws IllegalAccessException, IOException {
        
        final MouseDataWrapper md = mapper.readValue(new String(message.getBody()),
                MouseDataWrapper.class);
        log.debug("[{}] body:{}", QUEUE_DATA_V3, md);
        if (!md.getUserData().getIsActive()) {
            return Collections.emptySet();
        }
        final Set<String> keys = new HashSet<>();
        keys.addAll(sendPreparedReading(baseUri + "/frustration", md.getUserData().getFrustration(), md.getMouseData().getTimestamp()));
        keys.addAll(sendPreparedReading(baseUri + "/hesitation", md.getUserData().getHesitation(), md.getMouseData().getTimestamp()));
        keys.addAll(sendPreparedReading(baseUri + "/neutral", md.getUserData().getNeutral(), md.getMouseData().getTimestamp()));
    
        keys.addAll(sendPreparedReading(baseUri + "/gripforce", md.getMouseData().getGripForce(), md.getMouseData().getTimestamp()));
        keys.addAll(sendPreparedReading(baseUri + "/heartrate", md.getMouseData().getHeartRate(), md.getMouseData().getTimestamp()));
        keys.addAll(sendPreparedReading(baseUri + "/skinresponse", md.getMouseData().getSkinResponse(), md.getMouseData().getTimestamp()));
        keys.addAll(sendPreparedReading(baseUri + "/temperature", md.getMouseData().getTemperature(), md.getMouseData().getTimestamp()));
    
        keys.addAll(sendPreparedReading(baseUri + "/imu/acelX", md.getMouseData().getImu().getAcelX(), md.getMouseData().getTimestamp()));
        keys.addAll(sendPreparedReading(baseUri + "/imu/acelY", md.getMouseData().getImu().getAcelY(), md.getMouseData().getTimestamp()));
        keys.addAll(sendPreparedReading(baseUri + "/imu/acelZ", md.getMouseData().getImu().getAcelZ(), md.getMouseData().getTimestamp()));
        keys.addAll(sendPreparedReading(baseUri + "/imu/gyroX", md.getMouseData().getImu().getGyroX(), md.getMouseData().getTimestamp()));
        keys.addAll(sendPreparedReading(baseUri + "/imu/gyroY", md.getMouseData().getImu().getGyroY(), md.getMouseData().getTimestamp()));
        keys.addAll(sendPreparedReading(baseUri + "/imu/gyroZ", md.getMouseData().getImu().getGyroZ(), md.getMouseData().getTimestamp()));
    
        keys.addAll(sendPreparedReading(baseUri + "/mousepos/x", md.getMouseData().getPosition().getX(), md.getMouseData().getTimestamp()));
        keys.addAll(sendPreparedReading(baseUri + "/mousepos/y", md.getMouseData().getPosition().getY(), md.getMouseData().getTimestamp()));
        
        return keys;
    }
    
    private Collection<String> sendPreparedReading(final String baseUri, final Double reading, final Long timestamp) throws IllegalAccessException {
        long epochTime = timestamp - TICKS_AT_EPOCH;
        return sendMeasurement(baseUri, reading, epochTime);
    }
    
    private Collection<String> sendSingleValueReading(final String baseUri, final Message message) throws IllegalAccessException, IOException {
        final SingleValueReading singleValueReading = mapper.readValue(message.getBody(), SingleValueReading.class);
        long epochTime = singleValueReading.getTimestamp() - TICKS_AT_EPOCH;
        if (hasNullField(singleValueReading)) {
            log.error("baseUri: {}, null field {}", baseUri, singleValueReading);
            return Collections.emptySet();
        }
        return sendMeasurement(baseUri, singleValueReading.getReading(), epochTime);
    }

    private Collection<String> sendImuValueReading(final String baseUri, final Message message) throws IllegalAccessException, IOException {
        final Set<String> systemNames = new HashSet<>();
        final ImuValueReading imuValueReading = mapper.readValue(message.getBody(), ImuValueReading.class);
        long epochTime = imuValueReading.getTimestamp() - TICKS_AT_EPOCH;
        if (hasNullField(imuValueReading)) {
            log.error("baseUri: {}, null field {}", baseUri, imuValueReading);
            return systemNames;
        }
        systemNames.addAll(sendMeasurement(baseUri + "/acelX", imuValueReading.getAcelX(), epochTime));
        systemNames.addAll(sendMeasurement(baseUri + "/acelY", imuValueReading.getAcelY(), epochTime));
        systemNames.addAll(sendMeasurement(baseUri + "/acelZ", imuValueReading.getAcelZ(), epochTime));
        systemNames.addAll(sendMeasurement(baseUri + "/gyroX", imuValueReading.getGyroX(), epochTime));
        systemNames.addAll(sendMeasurement(baseUri + "/gyroY", imuValueReading.getGyroY(), epochTime));
        systemNames.addAll(sendMeasurement(baseUri + "/gyroZ", imuValueReading.getGyroZ(), epochTime));
//        TODO: disabled for now
//        sendMeasurement(baseUri + "/cmpX", imuValueReading.getCmpX(), imuValueReading.getTimestamp());
//        sendMeasurement(baseUri + "/cmpY", imuValueReading.getCmpY(), imuValueReading.getTimestamp());
//        sendMeasurement(baseUri + "/cmpZ", imuValueReading.getCmpZ(), imuValueReading.getTimestamp());
        return systemNames;
    }

    private Collection<String> sendDoubleValueReading(String baseUri, Message message) throws IllegalAccessException, IOException {
        final Set<String> systemNames = new HashSet<>();
        final DoubleValueReading doubleValueReading = mapper.readValue(message.getBody(), DoubleValueReading.class);
        long epochTime = doubleValueReading.getTimestamp() - TICKS_AT_EPOCH;
        if (hasNullField(doubleValueReading)) {
            log.error("baseUri: {}, null field {}", baseUri, doubleValueReading);
            return systemNames;
        }
        systemNames.addAll(sendMeasurement(baseUri + "/x", doubleValueReading.getX(), epochTime));
        systemNames.addAll(sendMeasurement(baseUri + "/y", doubleValueReading.getY(), epochTime));
        return systemNames;
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
