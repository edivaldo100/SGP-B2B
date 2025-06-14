package com.edivaldo.pedidos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "partners")
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal creditLimit;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal currentCredit; // Crédito disponível

    @Version // add
    private Long version; // Campo para o controle de versão otimista

    public Partner(Long id, String name, BigDecimal creditLimit, BigDecimal currentCredit) {
        this.id = id;
        this.name = name;
        this.creditLimit = creditLimit;
        this.currentCredit = currentCredit;
    }
}
