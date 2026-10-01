// src/main/java/com/example/appointments/application/AppointmentUseCase.java
package com.example.appointments.application;

import com.example.appointments.domain.Appointment;
import com.example.appointments.domain.AppointmentRepositoryPort;
import com.example.appointments.domain.EventPublisherPort;
import org.springframework.stereotype.Service;

@Service
public class AppointmentUseCase {

    private final AppointmentRepositoryPort repositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public AppointmentUseCase(AppointmentRepositoryPort repositoryPort, EventPublisherPort eventPublisherPort) {
        this.repositoryPort = repositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    public Appointment createAppointment(Appointment appointment) {
        appointment.setStatus("PENDING");
        Appointment savedAppointment = repositoryPort.save(appointment);
        
        // Send the event to kafka
        eventPublisherPort.publishAppointmentCreated(savedAppointment);
        
        return savedAppointment;
    }
}