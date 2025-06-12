package com.edivaldo.pedidos.service;

import com.edivaldo.pedidos.dto.OrderItemDTO;
import com.edivaldo.pedidos.dto.OrderRequestDTO;
import com.edivaldo.pedidos.dto.OrderResponseDTO;
import com.edivaldo.pedidos.enums.OrderStatus;
import com.edivaldo.pedidos.exception.CreditLimitExceededException;
import com.edivaldo.pedidos.exception.ResourceNotFoundException;
import com.edivaldo.pedidos.model.Order;
import com.edivaldo.pedidos.model.OrderItem;
import com.edivaldo.pedidos.model.Partner;
import com.edivaldo.pedidos.repository.OrderRepository;
import com.edivaldo.pedidos.repository.PartnerRepository;
import com.edivaldo.pedidos.service.NotificationService;
import com.edivaldo.pedidos.service.OrderService;
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
 * Focando na lógica de negócio e na interação com repositórios,
 * incluindo o comportamento esperado com bloqueio pessimista.
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
        // Configura um parceiro de teste
        testPartner = new Partner(1L, "Test Partner", new BigDecimal("1000.00"), new BigDecimal("1000.00"));

        // Configura um item de pedido de teste
        testOrderItem = new OrderItem(null, null, "Product A", 2, new BigDecimal("50.00"));

        // Configura um pedido de teste
        testOrder = new Order(1L, testPartner, Arrays.asList(testOrderItem), new BigDecimal("100.00"),
                OrderStatus.PENDENTE, LocalDateTime.now(), LocalDateTime.now());
        // Garante a ligação bidirecional entre Order e OrderItem
        testOrderItem.setOrder(testOrder);

        // Reseta os mocks antes de cada teste para garantir isolamento
        reset(orderRepository, partnerRepository, notificationService);
    }

    @Test
    void createOrder_ShouldCreateOrderSuccessfully() {
        // Mock do repositório de parceiros para retornar o parceiro de teste
        when(partnerRepository.findById(testPartner.getId())).thenReturn(Optional.of(testPartner));
        // Mock do repositório de pedidos para retornar o pedido salvo
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Cria um DTO de requisição de pedido
        OrderItemDTO itemDTO = new OrderItemDTO("Product A", 2, new BigDecimal("50.00"));
        OrderRequestDTO requestDTO = new OrderRequestDTO(testPartner.getId(), Collections.singletonList(itemDTO));

        // Chama o método a ser testado
        OrderResponseDTO responseDTO = orderService.createOrder(requestDTO);

        // Verifica se o pedido foi salvo e a notificação foi enviada
        assertNotNull(responseDTO);
        assertEquals(testOrder.getId(), responseDTO.getId());
        assertEquals(OrderStatus.PENDENTE, responseDTO.getStatus());
        assertEquals(testOrder.getTotalValue(), responseDTO.getTotalValue());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(notificationService, times(1)).notifyOrderStatusChange(testOrder.getId(), null, OrderStatus.PENDENTE);
        // Garante que o partnerRepository.findById foi chamado para obter o parceiro (sem bloqueio neste caso de criação)
        verify(partnerRepository, times(1)).findById(testPartner.getId());
    }

    @Test
    void createOrder_ShouldThrowResourceNotFoundException_WhenPartnerDoesNotExist() {
        // Mock do repositório de parceiros para retornar Optional.empty()
        when(partnerRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Cria um DTO de requisição de pedido com um ID de parceiro inexistente
        OrderRequestDTO requestDTO = new OrderRequestDTO(99L, Collections.singletonList(new OrderItemDTO("Product B", 1, new BigDecimal("10.00"))));

        // Verifica se a exceção esperada é lançada
        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(requestDTO));
        // Verifica que o método save do repositório de pedidos nunca foi chamado
        verify(orderRepository, never()).save(any(Order.class));
        verify(notificationService, never()).notifyOrderStatusChange(anyLong(), any(), any());
        verify(partnerRepository, times(1)).findById(anyLong()); // Garante que a busca pelo parceiro ocorreu
    }

    @Test
    void getOrderById_ShouldReturnOrder_WhenOrderExists() {
        // Mock do repositório de pedidos para retornar o pedido de teste
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

        // Chama o método a ser testado
        OrderResponseDTO responseDTO = orderService.getOrderById(testOrder.getId());

        // Verifica se o pedido retornado é o esperado
        assertNotNull(responseDTO);
        assertEquals(testOrder.getId(), responseDTO.getId());
        assertEquals(testOrder.getStatus(), responseDTO.getStatus());
        assertEquals(testOrder.getPartner().getId(), responseDTO.getPartnerId());
        verify(orderRepository, times(1)).findById(testOrder.getId());
    }

    @Test
    void getOrderById_ShouldThrowResourceNotFoundException_WhenOrderDoesNotExist() {
        // Mock do repositório de pedidos para retornar Optional.empty()
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Verifica se a exceção esperada é lançada
        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(99L));
        verify(orderRepository, times(1)).findById(99L);
    }

    @Test
    void getOrdersByPartnerId_ShouldReturnListOfOrders_WhenOrdersExistForPartner() {
        // Cria outro pedido para o mesmo parceiro
        Order anotherOrder = new Order(2L, testPartner, Collections.emptyList(), new BigDecimal("50.00"),
                OrderStatus.APROVADO, LocalDateTime.now(), LocalDateTime.now());
        List<Order> orders = Arrays.asList(testOrder, anotherOrder);

        // Mock do repositório de pedidos para retornar a lista de pedidos
        when(orderRepository.findByPartnerId(testPartner.getId())).thenReturn(orders);

        // Chama o método a ser testado
        List<OrderResponseDTO> responseDTOs = orderService.getOrdersByPartnerId(testPartner.getId());

        // Verifica se a lista de pedidos é retornada corretamente
        assertNotNull(responseDTOs);
        assertEquals(2, responseDTOs.size());
        assertEquals(testOrder.getId(), responseDTOs.get(0).getId());
        assertEquals(anotherOrder.getId(), responseDTOs.get(1).getId());
        verify(orderRepository, times(1)).findByPartnerId(testPartner.getId());
    }

    @Test
    void getOrdersByCreationPeriod_ShouldReturnListOfOrders_WhenOrdersExistInPeriod() {
        // Define um período de tempo
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<Order> orders = Collections.singletonList(testOrder);

        // Mock do repositório de pedidos
        when(orderRepository.findByCreatedAtBetween(start, end)).thenReturn(orders);

        // Chama o método a ser testado
        List<OrderResponseDTO> responseDTOs = orderService.getOrdersByCreationPeriod(start, end);

        // Verifica se a lista de pedidos é retornada corretamente
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
        // Configura o pedido como PENDENTE e o parceiro com crédito suficiente
        testOrder.setStatus(OrderStatus.PENDENTE);
        testOrder.setTotalValue(new BigDecimal("100.00"));
        testPartner.setCurrentCredit(new BigDecimal("200.00")); // Crédito suficiente
        testOrder.setPartner(testPartner); // Garante que o pedido está associado ao parceiro

        // Mocks para OrderRepository.findById e PartnerRepository.findById (agora com bloqueio)
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(partnerRepository.findById(testPartner.getId())).thenReturn(Optional.of(testPartner)); // Simula o bloqueio

        // Mocks para saves
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(partnerRepository.save(any(Partner.class))).thenReturn(testPartner);

        // Chama o método para aprovar o pedido
        OrderResponseDTO responseDTO = orderService.updateOrderStatus(testOrder.getId(), OrderStatus.APROVADO);

        // Verifica se o status foi atualizado e o crédito foi debitado
        assertNotNull(responseDTO);
        assertEquals(OrderStatus.APROVADO, responseDTO.getStatus());
        assertEquals(new BigDecimal("100.00"), testPartner.getCurrentCredit()); // 200 - 100 = 100
        verify(orderRepository, times(1)).findById(testOrder.getId());
        verify(partnerRepository, times(1)).findById(testPartner.getId()); // Chamada do método bloqueador
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(partnerRepository, times(1)).save(any(Partner.class));
        verify(notificationService, times(1)).notifyOrderStatusChange(testOrder.getId(), OrderStatus.PENDENTE, OrderStatus.APROVADO);
    }

    @Test
    void updateOrderStatus_ShouldThrowCreditLimitExceededException_WhenApprovingAndInsufficientCredit() {
        // Configura o pedido como PENDENTE e o parceiro com crédito insuficiente
        testOrder.setStatus(OrderStatus.PENDENTE);
        testOrder.setTotalValue(new BigDecimal("100.00"));
        testPartner.setCurrentCredit(new BigDecimal("50.00")); // Crédito insuficiente
        testOrder.setPartner(testPartner);

        // Mocks para OrderRepository.findById e PartnerRepository.findById (agora com bloqueio)
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(partnerRepository.findById(testPartner.getId())).thenReturn(Optional.of(testPartner)); // Simula o bloqueio

        // Verifica se a exceção de limite de crédito é lançada
        assertThrows(CreditLimitExceededException.class,
                () -> orderService.updateOrderStatus(testOrder.getId(), OrderStatus.APROVADO));

        // Verifica que os saves e a notificação não foram chamados devido à exceção
        verify(orderRepository, times(1)).findById(testOrder.getId());
        verify(partnerRepository, times(1)).findById(testPartner.getId()); // Chamada do método bloqueador
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
        testOrder.setPartner(testPartner);

        // Mocks para OrderRepository.findById e PartnerRepository.findById (agora com bloqueio)
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(partnerRepository.findById(testPartner.getId())).thenReturn(Optional.of(testPartner)); // Simula o bloqueio

        // Mocks para saves
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(partnerRepository.save(any(Partner.class))).thenReturn(testPartner);

        // Chama o método para cancelar o pedido
        OrderResponseDTO responseDTO = orderService.updateOrderStatus(testOrder.getId(), OrderStatus.CANCELADO);

        // Verifica se o status foi atualizado e o crédito foi estornado
        assertNotNull(responseDTO);
        assertEquals(OrderStatus.CANCELADO, responseDTO.getStatus());
        assertEquals(new BigDecimal("1000.00"), testPartner.getCurrentCredit()); // 900 + 100 = 1000
        verify(orderRepository, times(1)).findById(testOrder.getId());
        verify(partnerRepository, times(1)).findById(testPartner.getId()); // Chamada do método bloqueador
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(partnerRepository, times(1)).save(any(Partner.class));
        verify(notificationService, times(1)).notifyOrderStatusChange(testOrder.getId(), OrderStatus.APROVADO, OrderStatus.CANCELADO);
    }

    @Test
    void updateOrderStatus_ShouldThrowIllegalArgumentException_WhenInvalidStatusTransition() {
        // Tenta aprovar um pedido que já está EM_PROCESSAMENTO (transição inválida)
        testOrder.setStatus(OrderStatus.EM_PROCESSAMENTO);
        testOrder.setPartner(testPartner); // Necessário para a busca do parceiro no service

        // Mocks para OrderRepository.findById e PartnerRepository.findById (agora com bloqueio)
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(partnerRepository.findById(testPartner.getId())).thenReturn(Optional.of(testPartner)); // Simula o bloqueio

        assertThrows(IllegalArgumentException.class,
                () -> orderService.updateOrderStatus(testOrder.getId(), OrderStatus.APROVADO));

        verify(orderRepository, times(1)).findById(testOrder.getId());
        // REMOVIDO: verify(partnerRepository, times(1)).findById(testPartner.getId());
        // A chamada a partnerRepository.findById não ocorre porque a exceção é lançada antes.
        verify(partnerRepository, never()).save(any(Partner.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(notificationService, never()).notifyOrderStatusChange(anyLong(), any(), any());
    }

    @Test
    void cancelOrder_ShouldCancelOrderAndCreditBack_WhenApprovedAndCancellingViaCancelEndpoint() {
        // Configura o pedido como APROVADO
        testOrder.setStatus(OrderStatus.APROVADO);
        testOrder.setTotalValue(new BigDecimal("100.00"));
        testPartner.setCurrentCredit(new BigDecimal("900.00")); // Crédito após débito inicial
        testOrder.setPartner(testPartner);

        // Mocks para OrderRepository.findById e PartnerRepository.findById (agora com bloqueio)
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(partnerRepository.findById(testPartner.getId())).thenReturn(Optional.of(testPartner)); // Simula o bloqueio

        // Mocks para saves
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(partnerRepository.save(any(Partner.class))).thenReturn(testPartner);

        // Chama o método para cancelar o pedido
        OrderResponseDTO responseDTO = orderService.cancelOrder(testOrder.getId());

        // Verifica se o status foi atualizado e o crédito foi estornado
        assertNotNull(responseDTO);
        assertEquals(OrderStatus.CANCELADO, responseDTO.getStatus());
        assertEquals(new BigDecimal("1000.00"), testPartner.getCurrentCredit()); // 900 + 100 = 1000
        verify(orderRepository, times(1)).findById(testOrder.getId());
        verify(partnerRepository, times(1)).findById(testPartner.getId()); // Chamada do método bloqueador
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(partnerRepository, times(1)).save(any(Partner.class));
        verify(notificationService, times(1)).notifyOrderStatusChange(testOrder.getId(), OrderStatus.APROVADO, OrderStatus.CANCELADO);
    }

    @Test
    void cancelOrder_ShouldCancelOrderWithoutCreditBack_WhenPendingAndCancellingViaCancelEndpoint() {
        // Configura o pedido como PENDENTE
        testOrder.setStatus(OrderStatus.PENDENTE);
        testOrder.setTotalValue(new BigDecimal("100.00"));
        testPartner.setCurrentCredit(new BigDecimal("1000.00")); // Crédito não foi debitado
        testOrder.setPartner(testPartner);

        // Mocks para OrderRepository.findById e PartnerRepository.findById (agora com bloqueio)
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(partnerRepository.findById(testPartner.getId())).thenReturn(Optional.of(testPartner)); // Simula o bloqueio

        // Mocks para saves
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Chama o método para cancelar o pedido
        OrderResponseDTO responseDTO = orderService.cancelOrder(testOrder.getId());

        // Verifica se o status foi atualizado e o crédito NÃO foi estornado
        assertNotNull(responseDTO);
        assertEquals(OrderStatus.CANCELADO, responseDTO.getStatus());
        assertEquals(new BigDecimal("1000.00"), testPartner.getCurrentCredit()); // Permanece 1000
        verify(orderRepository, times(1)).findById(testOrder.getId());
        verify(partnerRepository, times(1)).findById(testPartner.getId()); // Chamada do método bloqueador
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(partnerRepository, never()).save(any(Partner.class)); // partnerRepository.save não deve ser chamado
        verify(notificationService, times(1)).notifyOrderStatusChange(testOrder.getId(), OrderStatus.PENDENTE, OrderStatus.CANCELADO);
    }

    @Test
    void cancelOrder_ShouldThrowIllegalArgumentException_WhenAlreadyDelivered() {
        // Tenta cancelar um pedido que já está ENTREGUE
        testOrder.setStatus(OrderStatus.ENTREGUE);
        testOrder.setPartner(testPartner);

        // Mocks para OrderRepository.findById
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        // NÃO MOCKAMOS partnerRepository.findById AQUI, pois ele não é chamado neste fluxo

        assertThrows(IllegalArgumentException.class,
                () -> orderService.cancelOrder(testOrder.getId()));

        verify(orderRepository, times(1)).findById(testOrder.getId());
        // Verificamos que partnerRepository.findById NUNCA é chamado.
        verify(partnerRepository, never()).findById(anyLong()); // Adicionado para clareza
        verify(orderRepository, never()).save(any(Order.class));
        verify(partnerRepository, never()).save(any(Partner.class));
        verify(notificationService, never()).notifyOrderStatusChange(anyLong(), any(), any());
    }
}
