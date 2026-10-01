// src/main/java/com/example/appointments/domain/AppointmentRepositoryPort.java
package com.example.appointments.domain;

import java.util.List;

public interface AppointmentRepositoryPort {
    Appointment save(Appointment appointment);

    List<Appointment> findByPatientId(String patientId);
}
