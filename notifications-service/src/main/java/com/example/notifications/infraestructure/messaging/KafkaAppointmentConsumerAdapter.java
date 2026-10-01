// src/main/java/com/example/notifications/infrastructure/messaging/KafkaAppointmentConsumerAdapter.java
package com.example.notifications.infrastructure.messaging;

import com.example.notifications.application.NotificationService;
import com.example.notifications.domain.AppointmentEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaAppointmentConsumerAdapter {

    private final NotificationService notificationService;

    public KafkaAppointmentConsumerAdapter(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "appointment-created-topic", groupId = "notification-group")
    public void consumeAppointmentEvent(AppointmentEvent event) {
        System.out.println(" [Kafka Consumer] Event received form Kafka: " + event.getId());
        notificationService.processAppointmentNotification(event);
    }
}