package com.edivaldo.sgp.service;

import com.edivaldo.sgp.dto.OrderItemDTO;
import com.edivaldo.sgp.dto.OrderRequestDTO;
import com.edivaldo.sgp.dto.OrderResponseDTO;
import com.edivaldo.sgp.enums.OrderStatus;
import com.edivaldo.sgp.exception.CreditLimitExceededException;
import com.edivaldo.sgp.exception.ResourceNotFoundException;
import com.edivaldo.sgp.model.Order;
import com.edivaldo.sgp.model.OrderItem;
import com.edivaldo.sgp.model.Partner;
import com.edivaldo.sgp.repository.OrderRepository;
import com.edivaldo.sgp.repository.PartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para o OrderService.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

    private Partner testPartner;
    private Order testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {

        testPartner = new Partner(1L, "Test Partner", new BigDecimal("1000.00"), new BigDecimal("1000.00"));

        testOrderItem = new OrderItem(null, null, "Product A", 2, new BigDecimal("50.00"));

        testOrder = new Order(1L, testPartner, Arrays.asList(testOrderItem), new BigDecimal("100.00"),
                OrderStatus.PENDENTE, LocalDateTime.now(), LocalDateTime.now());
        testOrderItem.setOrder(testOrder);

        reset(orderRepository, partnerRepository, notificationService);
    }

    @Test
    void createOrder_ShouldCreateOrderSuccessfully_WhenPartnerHasEnoughCredit() {

        when(partnerRepository.findById(testPartner.getId())).thenReturn(Optional.of(testPartner));

        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderItemDTO itemDTO = new OrderItemDTO("Product A", 2, new BigDecimal("50.00"));
        OrderRequestDTO requestDTO = new OrderRequestDTO(testPartner.getId(), Collections.singletonList(itemDTO));

        OrderResponseDTO responseDTO = orderService.createOrder(requestDTO);

        assertNotNull(responseDTO);
        assertEquals(testOrder.getId(), responseDTO.getId());
        assertEquals(OrderStatus.PENDENTE, responseDTO.getStatus());
        assertEquals(testOrder.getTotalValue(), responseDTO.getTotalValue());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(notificationService, times(1)).notifyOrderStatusChange(testOrder.getId(), null, OrderStatus.PENDENTE);
    }

    @Test
    void createOrder_ShouldThrowResourceNotFoundException_WhenPartnerDoesNotExist() {
        when(partnerRepository.findById(anyLong())).thenReturn(Optional.empty());

        OrderRequestDTO requestDTO = new OrderRequestDTO(99L, Collections.singletonList(new OrderItemDTO("Product B", 1, new BigDecimal("10.00"))));

        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(requestDTO));

        verify(orderRepository, never()).save(any(Order.class));
        verify(notificationService, never()).notifyOrderStatusChange(anyLong(), any(), any());
    }

    @Test
    void getOrderById_ShouldReturnOrder_WhenOrderExists() {
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        OrderResponseDTO responseDTO = orderService.getOrderById(testOrder.getId());
        assertNotNull(responseDTO);
        assertEquals(testOrder.getId(), responseDTO.getId());
        assertEquals(testOrder.getStatus(), responseDTO.getStatus());
        assertEquals(testOrder.getPartner().getId(), responseDTO.getPartnerId());
        verify(orderRepository, times(1)).findById(testOrder.getId());
    }

    @Test
    void getOrderById_ShouldThrowResourceNotFoundException_WhenOrderDoesNotExist() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(99L));
        verify(orderRepository, times(1)).findById(99L);
    }

    @Test
    void getOrdersByPartnerId_ShouldReturnListOfOrders_WhenOrdersExistForPartner() {
        Order anotherOrder = new Order(2L, testPartner, Collections.emptyList(), new BigDecimal("50.00"),
                OrderStatus.APROVADO, LocalDateTime.now(), LocalDateTime.now());
        List<Order> orders = Arrays.asList(testOrder, anotherOrder);

        when(orderRepository.findByPartnerId(testPartner.getId())).thenReturn(orders);
        List<OrderResponseDTO> responseDTOs = orderService.getOrdersByPartnerId(testPartner.getId());
        assertNotNull(responseDTOs);
        assertEquals(2, responseDTOs.size());
        assertEquals(testOrder.getId(), responseDTOs.get(0).getId());
        assertEquals(anotherOrder.getId(), responseDTOs.get(1).getId());
        verify(orderRepository, times(1)).findByPartnerId(testPartner.getId());
    }

    @Test
    void getOrdersByCreationPeriod_ShouldReturnListOfOrders_WhenOrdersExistInPeriod() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<Order> orders = Collections.singletonList(testOrder);
        when(orderRepository.findByCreatedAtBetween(start, end)).thenReturn(orders);
        List<OrderResponseDTO> responseDTOs = orderService.getOrdersByCreationPeriod(start, end);
        assertNotNull(responseDTOs);
        assertEquals(1, responseDTOs.size());
        assertEquals(testOrder.getId(), responseDTOs.get(0).getId());
        verify(orderRepository, times(1)).findByCreatedAtBetween(start, end);
    }

    @Test
    void getOrdersByStatus_ShouldReturnListOfOrders_WhenOrdersExistWithStatus() {
        List<Order> orders = Collections.singletonList(testOrder); // testOrder está PENDENTE
        when(orderRepository.findByStatus(OrderStatus.PENDENTE)).thenReturn(orders);

        List<OrderResponseDTO> responseDTOs = orderService.getOrdersByStatus(OrderStatus.PENDENTE);

        assertNotNull(responseDTOs);
        assertEquals(1, responseDTOs.size());
        assertEquals(testOrder.getId(), responseDTOs.get(0).getId());
        verify(orderRepository, times(1)).findByStatus(OrderStatus.PENDENTE);
    }

    @Test
    void updateOrderStatus_ShouldApproveOrderAndDebitCredit_WhenPendingAndEnoughCredit() {
        testOrder.setStatus(OrderStatus.PENDENTE);
        testOrder.setTotalValue(new BigDecimal("100.00"));
        testPartner.setCurrentCredit(new BigDecimal("200.00")); // Crédito suficiente

        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder); // Retorna o mesmo pedido atualizado
        when(partnerRepository.save(any(Partner.class))).thenReturn(testPartner); // Retorna o mesmo parceiro atualizado
        OrderResponseDTO responseDTO = orderService.updateOrderStatus(testOrder.getId(), OrderStatus.APROVADO);

        assertNotNull(responseDTO);
        assertEquals(OrderStatus.APROVADO, responseDTO.getStatus());
        assertEquals(new BigDecimal("100.00"), testPartner.getCurrentCredit()); // 200 - 100 = 100
        verify(orderRepository, times(1)).save(testOrder);
        verify(partnerRepository, times(1)).save(testPartner);
        verify(notificationService, times(1)).notifyOrderStatusChange(testOrder.getId(), OrderStatus.PENDENTE, OrderStatus.APROVADO);
    }

    @Test
    void updateOrderStatus_ShouldThrowCreditLimitExceededException_WhenApprovingAndInsufficientCredit() {
        testOrder.setStatus(OrderStatus.PENDENTE);
        testOrder.setTotalValue(new BigDecimal("100.00"));
        testPartner.setCurrentCredit(new BigDecimal("50.00")); // Crédito insuficiente

        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

        assertThrows(CreditLimitExceededException.class,
                () -> orderService.updateOrderStatus(testOrder.getId(), OrderStatus.APROVADO));

        // Verifica que o save do pedido e do parceiro não foram chamados
        verify(orderRepository, never()).save(any(Order.class));
        verify(partnerRepository, never()).save(any(Partner.class));
        verify(notificationService, never()).notifyOrderStatusChange(anyLong(), any(), any());
    }

    @Test
    void updateOrderStatus_ShouldCancelOrderAndCreditBack_WhenApprovedAndCancelling() {
        // Configura o pedido como APROVADO
        testOrder.setStatus(OrderStatus.APROVADO);
        testOrder.setTotalValue(new BigDecimal("100.00"));
        testPartner.setCurrentCredit(new BigDecimal("900.00")); // Crédito após débito inicial

        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(partnerRepository.save(any(Partner.class))).thenReturn(testPartner);

        //  cancelar o pedido
        OrderResponseDTO responseDTO = orderService.updateOrderStatus(testOrder.getId(), OrderStatus.CANCELADO);

        // Verifica se o status foi atualizado e o crédito foi estornado
        assertNotNull(responseDTO);
        assertEquals(OrderStatus.CANCELADO, responseDTO.getStatus());
        assertEquals(new BigDecimal("1000.00"), testPartner.getCurrentCredit()); // 900 + 100 = 1000
        verify(orderRepository, times(1)).save(testOrder);
        verify(partnerRepository, times(1)).save(testPartner);
        verify(notificationService, times(1)).notifyOrderStatusChange(testOrder.getId(), OrderStatus.APROVADO, OrderStatus.CANCELADO);
    }

    @Test
    void updateOrderStatus_ShouldThrowIllegalArgumentException_WhenInvalidStatusTransition() {
        // Tenta aprovar um pedido que já está EM_PROCESSAMENTO (transição inválida)
        testOrder.setStatus(OrderStatus.EM_PROCESSAMENTO);
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

        assertThrows(IllegalArgumentException.class,
                () -> orderService.updateOrderStatus(testOrder.getId(), OrderStatus.APROVADO));

        verify(orderRepository, never()).save(any(Order.class));
        verify(partnerRepository, never()).save(any(Partner.class));
        verify(notificationService, never()).notifyOrderStatusChange(anyLong(), any(), any());
    }

    @Test
    void cancelOrder_ShouldCancelOrderAndCreditBack_WhenApprovedAndCancellingViaCancelEndpoint() {
        // Configura o pedido como APROVADO
        testOrder.setStatus(OrderStatus.APROVADO);
        testOrder.setTotalValue(new BigDecimal("100.00"));
        testPartner.setCurrentCredit(new BigDecimal("900.00")); // Crédito após débito inicial

        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(partnerRepository.save(any(Partner.class))).thenReturn(testPartner);

        // cancelar o pedido
        OrderResponseDTO responseDTO = orderService.cancelOrder(testOrder.getId());

        // Verifica se o status foi atualizado e o crédito foi estornado
        assertNotNull(responseDTO);
        assertEquals(OrderStatus.CANCELADO, responseDTO.getStatus());
        assertEquals(new BigDecimal("1000.00"), testPartner.getCurrentCredit()); // 900 + 100 = 1000
        verify(orderRepository, times(1)).save(testOrder);
        verify(partnerRepository, times(1)).save(testPartner);
        verify(notificationService, times(1)).notifyOrderStatusChange(testOrder.getId(), OrderStatus.APROVADO, OrderStatus.CANCELADO);
    }

    @Test
    void cancelOrder_ShouldCancelOrderWithoutCreditBack_WhenPendingAndCancellingViaCancelEndpoint() {
        // Configura o pedido como PENDENTE
        testOrder.setStatus(OrderStatus.PENDENTE);
        testOrder.setTotalValue(new BigDecimal("100.00"));
        testPartner.setCurrentCredit(new BigDecimal("1000.00")); // Crédito não foi debitado

        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderResponseDTO responseDTO = orderService.cancelOrder(testOrder.getId());

        // Verifica se o status foi atualizado e o crédito NÃO foi estornado
        assertNotNull(responseDTO);
        assertEquals(OrderStatus.CANCELADO, responseDTO.getStatus());
        assertEquals(new BigDecimal("1000.00"), testPartner.getCurrentCredit()); // Permanece 1000
        verify(orderRepository, times(1)).save(testOrder);
        verify(partnerRepository, never()).save(any(Partner.class)); // partnerRepository.save não deve ser chamado
        verify(notificationService, times(1)).notifyOrderStatusChange(testOrder.getId(), OrderStatus.PENDENTE, OrderStatus.CANCELADO);
    }

    @Test
    void cancelOrder_ShouldThrowIllegalArgumentException_WhenAlreadyDelivered() {
        // Tenta cancelar um pedido que já está ENTREGUE
        testOrder.setStatus(OrderStatus.ENTREGUE);
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

        assertThrows(IllegalArgumentException.class,
                () -> orderService.cancelOrder(testOrder.getId()));

        verify(orderRepository, never()).save(any(Order.class));
        verify(partnerRepository, never()).save(any(Partner.class));
        verify(notificationService, never()).notifyOrderStatusChange(anyLong(), any(), any());
    }
}

