package com.edivaldo.pedidos;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

/**
 * Classe de execução
 * 
 * @author Edivaldo
 * @version 1.0.0
 * @since Release 01 da aplicação
 */
@Component
public class RunApp {

	@Transactional
	public void appStart() {

		System.out.println("Salva algo na base");
	}
}
