package com.marom.meditrack.repo;

import com.marom.meditrack.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    boolean existsByPatient_IdAndAppointment_AppointmentId(Long patientId, Long appointmentId);

    long countByAppointment_Doctor_Id(Long doctorId);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.appointment.doctor.id = :doctorId")
    Double averageRatingForDoctor(@Param("doctorId") Long doctorId);
}
