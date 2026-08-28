package com.marom.meditrack.service;

import com.marom.meditrack.dto.DoctorRequest;
import com.marom.meditrack.dto.DoctorResponse;
import com.marom.meditrack.exception.DuplicateResourceException;
import com.marom.meditrack.exception.ResourceNotFoundException;
import com.marom.meditrack.model.Doctor;
import com.marom.meditrack.model.Specialty;
import com.marom.meditrack.repo.DoctorRepository;
import com.marom.meditrack.repo.SpecialtyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepo;
    @Mock
    private SpecialtyRepository specialtyRepo;

    @InjectMocks
    private DoctorService doctorService;

    @Test
    void should_throwResourceNotFoundException_when_specialtyMissingOnCreate() {
        // Arrange
        when(specialtyRepo.findById(9L)).thenReturn(Optional.empty());
        DoctorRequest request = request(9L, "LIC-NEW-001");

        // Act & Assert
        assertThatThrownBy(() -> doctorService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Specialty not found");
        verify(doctorRepo, never()).save(any());
    }

    @Test
    void should_throwDuplicateResourceException_when_licenseAlreadyExistsOnCreate() {
        // Arrange
        when(specialtyRepo.findById(1L)).thenReturn(Optional.of(new Specialty()));
        when(doctorRepo.existsByLicenseNo("LIC-CARD-001")).thenReturn(true);
        DoctorRequest request = request(1L, "LIC-CARD-001");

        // Act & Assert
        assertThatThrownBy(() -> doctorService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists with license");
        verify(doctorRepo, never()).save(any());
    }

    @Test
    void should_saveDoctor_when_specialtyExistsAndLicenseIsFree() {
        // Arrange
        Specialty specialty = new Specialty();
        specialty.setId(1L);
        specialty.setName("Cardiology");
        when(specialtyRepo.findById(1L)).thenReturn(Optional.of(specialty));
        when(doctorRepo.existsByLicenseNo("LIC-NEW-001")).thenReturn(false);
        when(doctorRepo.save(any(Doctor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DoctorResponse result = doctorService.create(request(1L, "LIC-NEW-001"));

        // Assert
        assertThat(result.getName()).isEqualTo("Dr. New");
        assertThat(result.getLicenseNo()).isEqualTo("LIC-NEW-001");
        assertThat(result.getConsultationFee()).isEqualByComparingTo("200.00");
        assertThat(result.getSpecialty().getName()).isEqualTo("Cardiology");
    }

    @Test
    void should_throwResourceNotFoundException_when_doctorMissingOnFindById() {
        // Arrange
        when(doctorRepo.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> doctorService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Doctor not found");
    }

    private DoctorRequest request(Long specialtyId, String licenseNo) {
        return DoctorRequest.builder()
                .name("Dr. New")
                .licenseNo(licenseNo)
                .consultationFee(new BigDecimal("200.00"))
                .dailySlotCapacity(4)
                .active(true)
                .specialtyId(specialtyId)
                .build();
    }
}
