// src/main/java/com/example/appointments/domain/EventPublisherPort.java
package com.example.appointments.domain;

public interface EventPublisherPort {
    void publishAppointmentCreated(Appointment appointment);
}