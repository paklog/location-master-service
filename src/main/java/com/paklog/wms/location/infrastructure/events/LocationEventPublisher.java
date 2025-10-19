package com.paklog.wms.location.infrastructure.events;

import com.paklog.wms.location.domain.event.*;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Publishes location domain events as CloudEvents to Kafka
 */
@Component
public class LocationEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(LocationEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;
    private final String source;

    public LocationEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${cloudevents.kafka.topic}") String topic,
            @Value("${cloudevents.kafka.source}") String source
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.source = source;
    }

    /**
     * Publish LocationCreatedEvent
     */
    public void publishLocationCreated(LocationCreatedEvent event) {
        CloudEvent cloudEvent = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create(source))
            .withType("com.paklog.wms.location.created.v1")
            .withSubject(event.locationId())
            .withTime(OffsetDateTime.now())
            .withData("application/json", serializeEvent(event))
            .withExtension("warehouseid", event.warehouseId())
            .withExtension("locationtype", event.type().name())
            .build();

        publishEvent(cloudEvent, event.locationId());
    }

    /**
     * Publish LocationCapacityChangedEvent
     */
    public void publishLocationCapacityChanged(LocationCapacityChangedEvent event) {
        CloudEvent cloudEvent = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create(source))
            .withType("com.paklog.wms.location.capacity-changed.v1")
            .withSubject(event.locationId())
            .withTime(OffsetDateTime.now())
            .withData("application/json", serializeEvent(event))
            .withExtension("warehouseid", event.warehouseId())
            .build();

        publishEvent(cloudEvent, event.locationId());
    }

    /**
     * Publish LocationSlottingChangedEvent
     */
    public void publishLocationSlottingChanged(LocationSlottingChangedEvent event) {
        CloudEvent cloudEvent = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create(source))
            .withType("com.paklog.wms.location.slotting-changed.v1")
            .withSubject(event.locationId())
            .withTime(OffsetDateTime.now())
            .withData("application/json", serializeEvent(event))
            .withExtension("warehouseid", event.warehouseId())
            .withExtension("zone", event.zone())
            .withExtension("slottingclass", event.newClass().name())
            .build();

        publishEvent(cloudEvent, event.locationId());
    }

    /**
     * Publish LocationStatusChangedEvent
     */
    public void publishLocationStatusChanged(LocationStatusChangedEvent event) {
        CloudEvent cloudEvent = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create(source))
            .withType("com.paklog.wms.location.status-changed.v1")
            .withSubject(event.locationId())
            .withTime(OffsetDateTime.now())
            .withData("application/json", serializeEvent(event))
            .withExtension("warehouseid", event.warehouseId())
            .withExtension("newstatus", event.newStatus().name())
            .build();

        publishEvent(cloudEvent, event.locationId());
    }

    private void publishEvent(CloudEvent cloudEvent, String key) {
        try {
            kafkaTemplate.send(topic, key, cloudEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to publish event {} for key {}",
                            cloudEvent.getType(), key, ex);
                    } else {
                        logger.debug("Published event {} for key {}",
                            cloudEvent.getType(), key);
                    }
                });
        } catch (Exception e) {
            logger.error("Error publishing event {} for key {}",
                cloudEvent.getType(), key, e);
        }
    }

    private byte[] serializeEvent(Object event) {
        try {
            // Simple JSON serialization
            return com.fasterxml.jackson.databind.ObjectMapper.class
                .getDeclaredConstructor()
                .newInstance()
                .writeValueAsBytes(event);
        } catch (Exception e) {
            logger.error("Failed to serialize event", e);
            return new byte[0];
        }
    }
}
