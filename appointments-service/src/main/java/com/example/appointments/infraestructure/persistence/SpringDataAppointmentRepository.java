// src/main/java/com/example/appointments/infrastructure/persistence/SpringDataAppointmentRepository.java
package com.example.appointments.infrastructure.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataAppointmentRepository extends MongoRepository<AppointmentDocument, String> {

    List<AppointmentDocument> findByPatientId(String patientId);
}
