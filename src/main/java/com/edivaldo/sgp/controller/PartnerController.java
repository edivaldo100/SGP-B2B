package com.edivaldo.sgp.controller;

import com.edivaldo.sgp.dto.PartnerDTO;
import com.edivaldo.sgp.service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gerenciar operações de parceiros.
 */
@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
@Tag(name = "Parceiros", description = "API para gerenciamento de parceiros comerciais")
public class PartnerController {

    private final PartnerService partnerService;

    /**
     * Cria um novo parceiro.
     * @param partnerDTO Os dados do parceiro a serem criados.
     * @return ResponseEntity com o PartnerDTO do parceiro criado e status HTTP 201.
     */
    @Operation(summary = "Cria um novo parceiro")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Parceiro criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida")
    })
    @PostMapping
    public ResponseEntity<PartnerDTO> createPartner(@Valid @RequestBody PartnerDTO partnerDTO) {
        PartnerDTO createdPartner = partnerService.createPartner(partnerDTO);
        return new ResponseEntity<>(createdPartner, HttpStatus.CREATED);
    }

    /**
     * Busca um parceiro pelo ID.
     * @param id O ID do parceiro.
     * @return ResponseEntity com o PartnerDTO do parceiro encontrado e status HTTP 200.
     */
    @Operation(summary = "Busca um parceiro pelo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parceiro encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Parceiro não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PartnerDTO> getPartnerById(@PathVariable Long id) {
        PartnerDTO partner = partnerService.getPartnerById(id);
        return ResponseEntity.ok(partner);
    }

    /**
     * Busca todos os parceiros.
     * @return ResponseEntity com uma lista de PartnerDTOs e status HTTP 200.
     */
    @Operation(summary = "Busca todos os parceiros")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de parceiros retornada com sucesso")
    })
    @GetMapping
    public ResponseEntity<List<PartnerDTO>> getAllPartners() {
        List<PartnerDTO> partners = partnerService.getAllPartners();
        return ResponseEntity.ok(partners);
    }

    /**
     * Atualiza um parceiro existente.
     * @param id O ID do parceiro a ser atualizado.
     * @param partnerDTO Os novos dados do parceiro.
     * @return ResponseEntity com o PartnerDTO do parceiro atualizado e status HTTP 200.
     */
    @Operation(summary = "Atualiza um parceiro existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parceiro atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "404", description = "Parceiro não encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PartnerDTO> updatePartner(@PathVariable Long id, @Valid @RequestBody PartnerDTO partnerDTO) {
        PartnerDTO updatedPartner = partnerService.updatePartner(id, partnerDTO);
        return ResponseEntity.ok(updatedPartner);
    }

    /**
     * Deleta um parceiro pelo ID.
     * @param id O ID do parceiro a ser deletado.
     * @return ResponseEntity com status HTTP 204 (No Content).
     */
    @Operation(summary = "Deleta um parceiro pelo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Parceiro deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Parceiro não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePartner(@PathVariable Long id) {
        partnerService.deletePartner(id);
        return ResponseEntity.noContent().build();
    }
}
