// src/main/java/com/example/notifications/domain/AppointmentEvent.java
package com.example.notifications.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentEvent {
    private String id;
    private String patientId;
    private String appointmentDate;
    private String status;
}