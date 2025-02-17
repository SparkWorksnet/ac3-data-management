package net.sparkworks.mapper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sparkworks.mapper.model.EnvMessage;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitService {

    private static final String MESSAGE_TEMPLATE = "%s,%f,%d";
    private static final String DEBUG_FORMAT = "to [%s,%s] %s";
    private static final String DEBUG_SEND_FORMAT = "Sending: " + DEBUG_FORMAT;
    private static final String QUEUE_INPUT = "${rabbitmq.queue.input}";
    private static final String QUEUE_COMMAND = "${rabbitmq.queue.commands}";
    private static final String PROCESSING_TIMER_NAME = "mapper.mapped.latency";
    private static final String TAG_DEVICE = "device";
    private static final String TAG_SENSOR = "sensor";

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${rabbitmq.queue.output}")
    private String rabbitQueueOutput;

    private final RabbitTemplate rabbitTemplate;

    private final MeterRegistry meterRegistry;

    private Counter inputCounter, outputCounter;
    private AtomicInteger processingTime = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        inputCounter = Counter.builder("mapper.input.messages").description("input messages counter").register(meterRegistry);
        outputCounter = Counter.builder("mapper.output.messages").description("output messages counter").register(meterRegistry);
        Gauge.builder(PROCESSING_TIMER_NAME, processingTime, AtomicInteger::get).tags(TAG_DEVICE, "", TAG_SENSOR, "").register(meterRegistry);
    }

    public Collection<String> sendMeasurement(final String uri, final Integer reading, final long timestamp) {
        return sendMeasurement(uri, (double) reading, timestamp);
    }

    public Collection<String> sendMeasurement(final String uri, final Double reading, final long timestamp) {
        outputCounter.increment();
        final String message = String.format(MESSAGE_TEMPLATE, uri, reading, timestamp);

        try {
            write2file(message);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        log.debug(String.format(DEBUG_SEND_FORMAT, rabbitQueueOutput, rabbitQueueOutput, message));
        rabbitTemplate.send(rabbitQueueOutput, rabbitQueueOutput, new Message(message.getBytes(), new MessageProperties()));
        return Collections.singletonList(uri);
    }

    private void write2file(final String message) throws IOException {
        final File file = new File("/srv/data.csv");
        final FileWriter fr = new FileWriter(file, true);
        fr.write(message + "\n");
        fr.close();
    }

    //RabbitMQ listener for commands from SPARKS
    @RabbitListener(queues = QUEUE_COMMAND)
    public void receiveCommandFromSparks(final Message message) {
        log.info("[{}] command '{}'", QUEUE_COMMAND, new String(message.getBody()));
    }

    //RabbitMQ listener for data from IPN Mouse - data in format 3
    @RabbitListener(queues = QUEUE_INPUT)
    public void receiveFromSmartWorkV3(final Message message) {
        log.info("[{}] routingKey:{} body:{}", QUEUE_INPUT, message.getMessageProperties().getReceivedRoutingKey(), new String(message.getBody()));
        try {
            inputCounter.increment();
            long start = System.currentTimeMillis();
            final EnvMessage envMessage = mapper.readValue(new String(message.getBody()), EnvMessage.class);
            log.info("[{}] parsedMessage: {}", QUEUE_INPUT, envMessage);
            final String baseUri = message.getMessageProperties().getReceivedRoutingKey();
            long took = System.currentTimeMillis() - start;
            extractAndSendReadings(took, baseUri, envMessage);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void extractAndSendReadings(final long took, final String baseUri, final EnvMessage envMessage) {
        sendMeasurement(baseUri + "/temperature", envMessage.getTemperature(), envMessage.getTimestamp());
        sendMeasurement(baseUri + "/humidity", envMessage.getHumidity(), envMessage.getTimestamp());
        sendMeasurement(baseUri + "/iaq", envMessage.getIaq(), envMessage.getTimestamp());
        sendMeasurement(baseUri + "/iaqAccuracy", envMessage.getIaqAccuracy(), envMessage.getTimestamp());
        sendMeasurement(baseUri + "/co2", envMessage.getCo2(), envMessage.getTimestamp());
        String device = baseUri.split("/")[2];
        Metrics.timer(PROCESSING_TIMER_NAME, TAG_DEVICE, device, TAG_SENSOR, "temperature").record(took, TimeUnit.MILLISECONDS);
        Metrics.timer(PROCESSING_TIMER_NAME, TAG_DEVICE, device, TAG_SENSOR, "humidity").record(took, TimeUnit.MILLISECONDS);
        Metrics.timer(PROCESSING_TIMER_NAME, TAG_DEVICE, device, TAG_SENSOR, "iaq").record(took, TimeUnit.MILLISECONDS);
        Metrics.timer(PROCESSING_TIMER_NAME, TAG_DEVICE, device, TAG_SENSOR, "iaqAccuracy").record(took, TimeUnit.MILLISECONDS);
        Metrics.timer(PROCESSING_TIMER_NAME, TAG_DEVICE, device, TAG_SENSOR, "co2").record(took, TimeUnit.MILLISECONDS);

        outputCounter.increment(5);
    }

}
