// src/main/java/com/example/appointments/infrastructure/persistence/MongoAppointmentRepositoryAdapter.java
package com.example.appointments.infrastructure.persistence;

import com.example.appointments.domain.Appointment;
import com.example.appointments.domain.AppointmentRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class MongoAppointmentRepositoryAdapter implements AppointmentRepositoryPort {

    private final SpringDataAppointmentRepository mongoRepository;

    public MongoAppointmentRepositoryAdapter(SpringDataAppointmentRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public Appointment save(Appointment appointment) {
        AppointmentDocument doc = new AppointmentDocument();
        doc.setPatientId(appointment.getPatientId());
        doc.setAppointmentDate(appointment.getAppointmentDate());
        doc.setStatus(appointment.getStatus());

        AppointmentDocument savedDoc = mongoRepository.save(doc);

        return new Appointment(
            savedDoc.getId(),
            savedDoc.getPatientId(),
            savedDoc.getAppointmentDate(),
            savedDoc.getStatus()
        );
    }
}