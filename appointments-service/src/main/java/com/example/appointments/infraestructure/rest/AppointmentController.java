// src/main/java/com/example/appointments/infrastructure/rest/AppointmentController.java
package com.example.appointments.infrastructure.rest;

import com.example.appointments.application.AppointmentUseCase;
import com.example.appointments.domain.Appointment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentUseCase appointmentUseCase;

    public AppointmentController(AppointmentUseCase appointmentUseCase) {
        this.appointmentUseCase = appointmentUseCase;
    }

    @PostMapping
    public ResponseEntity<Appointment> createAppointment(
            @RequestHeader("X-Patient-Id") Long patientId, // header inyected
            @RequestBody Appointment appointment) {

        appointment.setPatientId(patientId);

        Appointment created = appointmentUseCase.createAppointment(appointment);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Appointment>> listAppointments(
            @RequestHeader("X-Patient-Id") Long patientId) {
        return ResponseEntity.ok(appointmentUseCase.listAppointments(patientId));
    }
}
