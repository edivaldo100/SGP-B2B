package com.edivaldo.pedidos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartnerDTO {
    private Long id;

    @NotBlank(message = "O nome do parceiro não pode ser vazio")
    private String name;

    @NotNull(message = "O limite de crédito não pode ser nulo")
    @Min(value = 0, message = "O limite de crédito deve ser um valor positivo ou zero")
    private BigDecimal creditLimit;

    @NotNull(message = "O crédito atual não pode ser nulo")
    @Min(value = 0, message = "O crédito atual deve ser um valor positivo ou zero")
    private BigDecimal currentCredit;
}
