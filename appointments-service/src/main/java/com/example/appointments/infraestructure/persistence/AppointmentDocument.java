// src/main/java/com/example/appointments/infrastructure/persistence/AppointmentDocument.java
package com.example.appointments.infrastructure.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Document(collection = "appointments")
@Data
public class AppointmentDocument {
    @Id
    private String id;
    private Long patientId;
    private LocalDateTime appointmentDate;
    private String status;
}