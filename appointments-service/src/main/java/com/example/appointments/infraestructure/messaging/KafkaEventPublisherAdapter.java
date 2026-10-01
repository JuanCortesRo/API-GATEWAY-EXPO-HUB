// src/main/java/com/example/appointments/infrastructure/messaging/KafkaEventPublisherAdapter.java
package com.example.appointments.infrastructure.messaging;

import com.example.appointments.domain.Appointment;
import com.example.appointments.domain.EventPublisherPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "appointment-created-topic";

    public KafkaEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishAppointmentCreated(Appointment appointment) {
        kafkaTemplate.send(TOPIC, appointment);
        System.out.println(" [Kafka Event Published] Appointment ID: " + appointment.getId());
    }
}