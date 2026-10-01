// src/main/java/com/example/appointments/domain/AppointmentRepositoryPort.java
package com.example.appointments.domain;

public interface AppointmentRepositoryPort {
    Appointment save(Appointment appointment);
}