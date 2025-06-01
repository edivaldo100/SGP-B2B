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
public class OrderItemDTO {

    @NotBlank(message = "O nome do produto não pode ser vazio")
    private String product;

    @NotNull(message = "A quantidade não pode ser nula")
    @Min(value = 1, message = "A quantidade deve ser no mínimo 1")
    private Integer quantity;

    @NotNull(message = "O preço unitário não pode ser nulo")
    @Min(value = 0, message = "O preço unitário deve ser um valor positivo")
    private BigDecimal unitPrice;
}
