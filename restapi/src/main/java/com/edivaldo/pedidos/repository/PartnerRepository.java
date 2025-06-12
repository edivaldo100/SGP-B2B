package com.edivaldo.pedidos.repository;

import com.edivaldo.pedidos.model.Partner;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {

    // Método para buscar um parceiro pelo ID com bloqueio de escrita (PESSIMISTIC_WRITE)
    // Isso aplica um SELECT ... FOR UPDATE no banco de dados.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Partner> findById(Long id); // Sobrescreve o findById padrão para aplicar o bloqueio

    Optional<Partner> findByName(String name);
}
