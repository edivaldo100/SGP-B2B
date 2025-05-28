package com.edivaldo.sgp.service;

import com.edivaldo.sgp.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j // Para logging
public class NotificationService {

    @Value("${notification.service.mock.enabled:true}")
    private boolean mockEnabled;

    public void notifyOrderStatusChange(Long orderId, OrderStatus oldStatus, OrderStatus newStatus) {
        if (mockEnabled) {
            log.info("Simulando notificação para o pedido ID: {} - Status alterado de {} para {}", orderId, oldStatus, newStatus);
        } else {
            log.debug("Serviço de notificação mock desabilitado.");
        }
    }
}
