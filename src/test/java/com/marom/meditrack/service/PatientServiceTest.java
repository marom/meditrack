package com.marom.meditrack.service;

import com.marom.meditrack.dto.PatientRequest;
import com.marom.meditrack.dto.PatientResponse;
import com.marom.meditrack.exception.DuplicateResourceException;
import com.marom.meditrack.model.Patient;
import com.marom.meditrack.repo.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepo;

    @InjectMocks
    private PatientService patientService;

    @Test
    void should_throwDuplicateResourceException_when_emailAlreadyRegistered() {
        // Arrange
        when(patientRepo.existsByEmail("john.doe@example.com")).thenReturn(true);
        PatientRequest request = request("john.doe@example.com");

        // Act & Assert
        assertThatThrownBy(() -> patientService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists with email");
        verify(patientRepo, never()).save(any());
    }

    @Test
    void should_savePatient_when_emailIsFree() {
        // Arrange
        when(patientRepo.existsByEmail("alice.wong@example.com")).thenReturn(false);
        when(patientRepo.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PatientResponse result = patientService.register(request("alice.wong@example.com"));

        // Assert
        assertThat(result.getFirstName()).isEqualTo("Alice");
        assertThat(result.getEmail()).isEqualTo("alice.wong@example.com");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    private PatientRequest request(String email) {
        return PatientRequest.builder()
                .firstName("Alice")
                .lastName("Wong")
                .email(email)
                .phone("+1-202-555-0000")
                .dateOfBirth(LocalDate.parse("1995-06-15"))
                .build();
    }
}
