package com.edivaldo.pedidos.service;

import com.edivaldo.pedidos.dto.PartnerDTO;
import com.edivaldo.pedidos.exception.ResourceNotFoundException;
import com.edivaldo.pedidos.model.Partner;
import com.edivaldo.pedidos.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerRepository partnerRepository;


    private PartnerDTO toDTO(Partner partner) {
        return new PartnerDTO(partner.getId(), partner.getName(), partner.getCreditLimit(), partner.getCurrentCredit());
    }

    private Partner toEntity(PartnerDTO partnerDTO) {
        Partner partner = new Partner();
        partner.setId(partnerDTO.getId());
        partner.setName(partnerDTO.getName());
        partner.setCreditLimit(partnerDTO.getCreditLimit());
        partner.setCurrentCredit(partnerDTO.getCurrentCredit());
        return partner;
    }

    @Transactional
    public PartnerDTO createPartner(PartnerDTO partnerDTO) {
        Partner partner = toEntity(partnerDTO);
        if (partner.getCurrentCredit() == null) {
            partner.setCurrentCredit(partner.getCreditLimit());
        }
        Partner savedPartner = partnerRepository.save(partner);

        //List<Partner> all = partnerRepository.findAll();//teste
        return toDTO(savedPartner);
    }

    @Transactional(readOnly = true)
    public PartnerDTO getPartnerById(Long id) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parceiro não encontrado com ID: " + id));
        return toDTO(partner);
    }
    @Transactional(readOnly = true)
    public List<PartnerDTO> getAllPartners() {
        return partnerRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PartnerDTO updatePartner(Long id, PartnerDTO partnerDTO) {
        Partner existingPartner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parceiro não encontrado com ID: " + id));

        existingPartner.setName(partnerDTO.getName());
        existingPartner.setCreditLimit(partnerDTO.getCreditLimit());
        existingPartner.setCurrentCredit(partnerDTO.getCurrentCredit());

        Partner updatedPartner = partnerRepository.save(existingPartner);
        return toDTO(updatedPartner);
    }

    @Transactional
    public void deletePartner(Long id) {
        if (!partnerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Parceiro não encontrado com ID: " + id);
        }
        partnerRepository.deleteById(id);
    }

    @Transactional
    public Partner debitCredit(Partner partner, BigDecimal amount) {
        if (partner.getCurrentCredit().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Crédito insuficiente para debitar.");
        }
        partner.setCurrentCredit(partner.getCurrentCredit().subtract(amount));
        return partnerRepository.save(partner);
    }

    @Transactional
    public Partner creditCredit(Partner partner, BigDecimal amount) {
        partner.setCurrentCredit(partner.getCurrentCredit().add(amount));
        return partnerRepository.save(partner);
    }
}
