package com.edivaldo.sgp.service;

import com.edivaldo.sgp.dto.PartnerDTO;
import com.edivaldo.sgp.exception.ResourceNotFoundException;
import com.edivaldo.sgp.model.Partner;
import com.edivaldo.sgp.repository.PartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para o PartnerService.
 */
@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    @Mock
    private PartnerRepository partnerRepository;

    @InjectMocks
    private PartnerService partnerService;

    private Partner testPartner;
    private PartnerDTO testPartnerDTO;

    @BeforeEach
    void setUp() {
        testPartner = new Partner(1L, "Test Partner", new BigDecimal("1000.00"), new BigDecimal("1000.00"));
        testPartnerDTO = new PartnerDTO(1L, "Test Partner", new BigDecimal("1000.00"), new BigDecimal("1000.00"));

        reset(partnerRepository);
    }

    @Test
    void createPartner_ShouldCreatePartnerSuccessfully() {

        when(partnerRepository.save(any(Partner.class))).thenReturn(testPartner);

        PartnerDTO newPartnerDTO = new PartnerDTO(null, "Test Partner", new BigDecimal("1000.00"), null);

        PartnerDTO createdPartner = partnerService.createPartner(newPartnerDTO);

        assertNotNull(createdPartner);
        assertEquals("Test Partner", createdPartner.getName());
        assertEquals(new BigDecimal("1000.00"), createdPartner.getCreditLimit());
        assertEquals(new BigDecimal("1000.00"), createdPartner.getCurrentCredit());
        verify(partnerRepository, times(1)).save(any(Partner.class));
    }

    @Test
    void getPartnerById_ShouldReturnPartner_WhenPartnerExists() {
        when(partnerRepository.findById(testPartner.getId())).thenReturn(Optional.of(testPartner));
        PartnerDTO foundPartner = partnerService.getPartnerById(testPartner.getId());
        assertNotNull(foundPartner);
        assertEquals(testPartner.getId(), foundPartner.getId());
        assertEquals(testPartner.getName(), foundPartner.getName());
        verify(partnerRepository, times(1)).findById(testPartner.getId());
    }

    @Test
    void getPartnerById_ShouldThrowResourceNotFoundException_WhenPartnerDoesNotExist() {
        when(partnerRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> partnerService.getPartnerById(99L));
        verify(partnerRepository, times(1)).findById(99L);
    }

    @Test
    void getAllPartners_ShouldReturnListOfPartners() {

        List<Partner> partners = Arrays.asList(testPartner, new Partner(2L, "Partner B", new BigDecimal("2000.00"), new BigDecimal("2000.00")));
        when(partnerRepository.findAll()).thenReturn(partners);
        List<PartnerDTO> foundPartners = partnerService.getAllPartners();

        assertNotNull(foundPartners);
        assertEquals(2, foundPartners.size());
        assertEquals(testPartner.getName(), foundPartners.get(0).getName());
        assertEquals("Partner B", foundPartners.get(1).getName());
        verify(partnerRepository, times(1)).findAll();
    }

    @Test
    void updatePartner_ShouldUpdatePartnerSuccessfully() {
        when(partnerRepository.findById(testPartner.getId())).thenReturn(Optional.of(testPartner));
        when(partnerRepository.save(any(Partner.class))).thenReturn(testPartner);
        PartnerDTO updatedPartnerDTO = new PartnerDTO(testPartner.getId(), "Updated Partner", new BigDecimal("1500.00"), new BigDecimal("1200.00"));
        PartnerDTO updatedPartner = partnerService.updatePartner(testPartner.getId(), updatedPartnerDTO);
        assertNotNull(updatedPartner);
        assertEquals("Updated Partner", updatedPartner.getName());
        assertEquals(new BigDecimal("1500.00"), updatedPartner.getCreditLimit());
        assertEquals(new BigDecimal("1200.00"), updatedPartner.getCurrentCredit());
        verify(partnerRepository, times(1)).findById(testPartner.getId());
        verify(partnerRepository, times(1)).save(testPartner);
    }

    @Test
    void updatePartner_ShouldThrowResourceNotFoundException_WhenPartnerToUpdateDoesNotExist() {
        when(partnerRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> partnerService.updatePartner(99L, testPartnerDTO));
        verify(partnerRepository, times(1)).findById(99L);
        verify(partnerRepository, never()).save(any(Partner.class));
    }

    @Test
    void deletePartner_ShouldDeletePartnerSuccessfully() {
        when(partnerRepository.existsById(testPartner.getId())).thenReturn(true);
        assertDoesNotThrow(() -> partnerService.deletePartner(testPartner.getId()));
        verify(partnerRepository, times(1)).existsById(testPartner.getId());
        verify(partnerRepository, times(1)).deleteById(testPartner.getId());
    }

    @Test
    void deletePartner_ShouldThrowResourceNotFoundException_WhenPartnerToDeleteDoesNotExist() {
        when(partnerRepository.existsById(anyLong())).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> partnerService.deletePartner(99L));
        verify(partnerRepository, times(1)).existsById(99L);
        verify(partnerRepository, never()).deleteById(anyLong());
    }

    @Test
    void debitCredit_ShouldDebitCreditSuccessfully() {
        testPartner.setCurrentCredit(new BigDecimal("500.00"));
        BigDecimal debitAmount = new BigDecimal("100.00");
        when(partnerRepository.save(any(Partner.class))).thenReturn(testPartner);

        Partner updatedPartner = partnerService.debitCredit(testPartner, debitAmount);
        assertNotNull(updatedPartner);
        assertEquals(new BigDecimal("400.00"), updatedPartner.getCurrentCredit());
        verify(partnerRepository, times(1)).save(testPartner);
    }

    @Test
    void debitCredit_ShouldThrowIllegalArgumentException_WhenInsufficientCredit() {
        // Configura o parceiro com crédito insuficiente
        testPartner.setCurrentCredit(new BigDecimal("50.00"));
        BigDecimal debitAmount = new BigDecimal("100.00");
        assertThrows(IllegalArgumentException.class, () -> partnerService.debitCredit(testPartner, debitAmount));
        verify(partnerRepository, never()).save(any(Partner.class));
    }

    @Test
    void creditCredit_ShouldCreditCreditSuccessfully() {
        testPartner.setCurrentCredit(new BigDecimal("500.00"));
        BigDecimal creditAmount = new BigDecimal("100.00");

        when(partnerRepository.save(any(Partner.class))).thenReturn(testPartner);

        Partner updatedPartner = partnerService.creditCredit(testPartner, creditAmount);

        assertNotNull(updatedPartner);
        assertEquals(new BigDecimal("600.00"), updatedPartner.getCurrentCredit());
        verify(partnerRepository, times(1)).save(testPartner);
    }
}

