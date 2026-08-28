package com.marom.meditrack.service;

import com.marom.meditrack.dto.SpecialtyRequest;
import com.marom.meditrack.dto.SpecialtyResponse;
import com.marom.meditrack.exception.DuplicateResourceException;
import com.marom.meditrack.model.Specialty;
import com.marom.meditrack.repo.SpecialtyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecialtyServiceTest {

    @Mock
    private SpecialtyRepository specialtyRepo;

    @InjectMocks
    private SpecialtyService specialtyService;

    @Test
    void should_throwDuplicateResourceException_when_nameOrSlugAlreadyExists() {
        // Arrange
        when(specialtyRepo.existsByNameOrSlug("Cardiology", "cardiology")).thenReturn(true);
        SpecialtyRequest request = SpecialtyRequest.builder()
                .name("Cardiology").slug("cardiology").description("dup").build();

        // Act & Assert
        assertThatThrownBy(() -> specialtyService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists with name or slug");
        verify(specialtyRepo, never()).save(any());
    }

    @Test
    void should_saveSpecialty_when_nameAndSlugAreFree() {
        // Arrange
        when(specialtyRepo.existsByNameOrSlug("Neurology", "neurology")).thenReturn(false);
        when(specialtyRepo.save(any(Specialty.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SpecialtyRequest request = SpecialtyRequest.builder()
                .name("Neurology").slug("neurology").description("Brain and nervous system").build();

        // Act
        SpecialtyResponse result = specialtyService.create(request);

        // Assert
        assertThat(result.getName()).isEqualTo("Neurology");
        assertThat(result.getSlug()).isEqualTo("neurology");
        assertThat(result.getCreatedAt()).isNotNull();
    }
}
