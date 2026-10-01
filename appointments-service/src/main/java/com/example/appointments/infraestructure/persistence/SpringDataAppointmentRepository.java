// src/main/java/com/example/appointments/infrastructure/persistence/SpringDataAppointmentRepository.java
package com.example.appointments.infrastructure.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAppointmentRepository extends MongoRepository<AppointmentDocument, String> {
}