package com.paklog.wms.location.infrastructure.events;

import com.paklog.wms.location.domain.event.*;
import com.paklog.wms.location.domain.valueobject.LocationStatus;
import com.paklog.wms.location.domain.valueobject.LocationType;
import com.paklog.wms.location.domain.valueobject.SlottingClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private LocationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new LocationEventPublisher(kafkaTemplate, "location-events", "urn:paklog:test");
        when(kafkaTemplate.send(anyString(), anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    @DisplayName("publishLocationCreated builds CloudEvent and sends to Kafka")
    void publishLocationCreated() {
        LocationCreatedEvent event = LocationCreatedEvent.of(
            "BIN-1", "WH-1", "Bin 1", LocationType.BIN, "LEVEL-1", 5, "PICK", "tester"
        );

        publisher.publishLocationCreated(event);

        ArgumentCaptor<io.cloudevents.CloudEvent> captor = ArgumentCaptor.forClass(io.cloudevents.CloudEvent.class);
        verify(kafkaTemplate).send(eq("location-events"), eq("BIN-1"), captor.capture());

        io.cloudevents.CloudEvent cloudEvent = captor.getValue();
        assertEquals("com.paklog.wms.location.created.v1", cloudEvent.getType());
        assertEquals("BIN-1", cloudEvent.getSubject());
        assertEquals("WH-1", cloudEvent.getExtension("warehouseid"));
        assertNotNull(cloudEvent.getData());
    }

    @Test
    @DisplayName("publishLocationCapacityChanged emits capacity update event")
    void publishCapacityChanged() {
        LocationCapacityChangedEvent event = LocationCapacityChangedEvent.of(
            "BIN-1", "WH-1", 100, 150,
            null, null,
            null, null,
            "tester", "Expansion"
        );

        publisher.publishLocationCapacityChanged(event);

        ArgumentCaptor<io.cloudevents.CloudEvent> captor = ArgumentCaptor.forClass(io.cloudevents.CloudEvent.class);
        verify(kafkaTemplate).send(eq("location-events"), eq("BIN-1"), captor.capture());
        assertEquals("com.paklog.wms.location.capacity-changed.v1", captor.getValue().getType());
    }

    @Test
    @DisplayName("publishLocationSlottingChanged emits slotting update event with extensions")
    void publishSlottingChanged() {
        LocationSlottingChangedEvent event = new LocationSlottingChangedEvent(
            java.util.UUID.randomUUID().toString(),
            "BIN-1",
            "WH-1",
            "PICK",
            SlottingClass.A,
            SlottingClass.B,
            100,
            LocalDateTime.now(),
            "tester",
            "Reason"
        );

        publisher.publishLocationSlottingChanged(event);

        ArgumentCaptor<io.cloudevents.CloudEvent> captor = ArgumentCaptor.forClass(io.cloudevents.CloudEvent.class);
        verify(kafkaTemplate).send(eq("location-events"), eq("BIN-1"), captor.capture());
        io.cloudevents.CloudEvent cloudEvent = captor.getValue();
        assertEquals("com.paklog.wms.location.slotting-changed.v1", cloudEvent.getType());
        assertEquals("PICK", cloudEvent.getExtension("zone"));
        assertEquals("B", cloudEvent.getExtension("slottingclass"));
    }

    @Test
    @DisplayName("publishLocationStatusChanged emits status change event")
    void publishStatusChanged() {
        LocationStatusChangedEvent event = new LocationStatusChangedEvent(
            java.util.UUID.randomUUID().toString(),
            "BIN-1",
            "WH-1",
            LocationStatus.ACTIVE,
            LocationStatus.BLOCKED,
            "Maintenance",
            LocalDateTime.now(),
            "tester"
        );

        publisher.publishLocationStatusChanged(event);

        ArgumentCaptor<io.cloudevents.CloudEvent> captor = ArgumentCaptor.forClass(io.cloudevents.CloudEvent.class);
        verify(kafkaTemplate).send(eq("location-events"), eq("BIN-1"), captor.capture());
        assertEquals("BLOCKED", captor.getValue().getExtension("newstatus"));
    }

    @Test
    @DisplayName("Failures during send are caught and do not propagate")
    void publishHandlesSendFailure() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
            .thenThrow(new RuntimeException("Kafka unavailable"));

        assertDoesNotThrow(() -> publisher.publishLocationCreated(LocationCreatedEvent.of(
            "BIN-2", "WH-1", "Bin 2", LocationType.BIN, "LEVEL-1", 5, "PICK", "tester"
        )));
    }
}
