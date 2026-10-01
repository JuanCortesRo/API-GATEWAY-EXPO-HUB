// src/main/java/com/example/notifications/application/NotificationService.java
package com.example.notifications.application;

import com.example.notifications.domain.AppointmentEvent;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public void processAppointmentNotification(AppointmentEvent event) {
        // Simulation of a notification using logs
        System.out.println("==================================================");
        System.out.println(" [NOTIFICATION SENT]");
        System.out.println(" Patient ID: " + event.getPatientId());
        System.out.println(" Appointment ID (" + event.getId() + ") has been scheduled.");
        System.out.println(" Status: " + event.getStatus());
        System.out.println("==================================================");
    }
}