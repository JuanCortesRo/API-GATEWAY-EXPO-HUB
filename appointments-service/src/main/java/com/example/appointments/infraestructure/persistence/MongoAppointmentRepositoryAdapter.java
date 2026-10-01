// src/main/java/com/example/appointments/infrastructure/persistence/MongoAppointmentRepositoryAdapter.java
package com.example.appointments.infrastructure.persistence;

import com.example.appointments.domain.Appointment;
import com.example.appointments.domain.AppointmentRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

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

        return toDomain(savedDoc);
    }

    @Override
    public List<Appointment> findByPatientId(Long patientId) {
        return mongoRepository.findByPatientId(patientId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Appointment toDomain(AppointmentDocument doc) {
        return new Appointment(
                doc.getId(),
                doc.getPatientId(),
                doc.getAppointmentDate(),
                doc.getStatus()
        );
    }
}
