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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final PartnerRepository partnerRepository;
    private final NotificationService notificationService;

    private OrderResponseDTO toResponseDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(item -> new OrderItemDTO(item.getProduct(), item.getQuantity(), item.getUnitPrice()))
                .collect(Collectors.toList());
        return new OrderResponseDTO(
                order.getId(),
                order.getPartner().getId(),
                order.getPartner().getName(),
                itemDTOs,
                order.getTotalValue(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO orderRequestDTO) {
        Partner partner = partnerRepository.findById(orderRequestDTO.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Parceiro não encontrado com ID: " + orderRequestDTO.getPartnerId()));

        Order order = new Order();
        order.setPartner(partner);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDENTE); // Pedido começa como PENDENTE por padrão

        // Adiciona itens ao pedido
        orderRequestDTO.getItems().forEach(itemDTO -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(itemDTO.getProduct());
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setUnitPrice(itemDTO.getUnitPrice());
            order.addOrderItem(orderItem);
        });

        BigDecimal totalOrderValue = order.calculateTotalValue();
        order.setTotalValue(totalOrderValue);

        if (order.getStatus() == OrderStatus.APROVADO && partner.getCurrentCredit().compareTo(totalOrderValue) < 0) {
            throw new CreditLimitExceededException("Parceiro ID " + partner.getId() + " não tem crédito suficiente para este pedido. Crédito disponível: " + partner.getCurrentCredit() + ", Valor do pedido: " + totalOrderValue);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Pedido ID {} criado para o parceiro ID {}", savedOrder.getId(), partner.getId());
        notificationService.notifyOrderStatusChange(savedOrder.getId(), null, savedOrder.getStatus()); // Notifica a criação

        return toResponseDTO(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        return toResponseDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByPartnerId(Long partnerId) {
        List<Order> orders = orderRepository.findByPartnerId(partnerId);
        return orders.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByCreationPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findByCreatedAtBetween(startDate, endDate);
        return orders.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        return orders.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional
    public OrderResponseDTO updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));

        OrderStatus oldStatus = order.getStatus();


        if (oldStatus == newStatus) {
            log.warn("Tentativa de atualizar o pedido ID {} para o mesmo status: {}", id, newStatus);
            return toResponseDTO(order); // Nenhuma mudança, retorna o pedido atual
        }

        Partner partner = order.getPartner();
        BigDecimal orderValue = order.getTotalValue();

        switch (newStatus) {
            case APROVADO:
                if (oldStatus == OrderStatus.PENDENTE) {
                    if (partner.getCurrentCredit().compareTo(orderValue) < 0) {
                        throw new CreditLimitExceededException("Parceiro ID " + partner.getId() + " não tem crédito suficiente para aprovar este pedido. Crédito disponível: " + partner.getCurrentCredit() + ", Valor do pedido: " + orderValue);
                    }
                    partner.setCurrentCredit(partner.getCurrentCredit().subtract(orderValue));
                    partnerRepository.save(partner);
                    log.info("Crédito de {} debitado do parceiro ID {} para o pedido ID {}", orderValue, partner.getId(), id);
                } else {
                    throw new IllegalArgumentException("Não é possível aprovar um pedido com status " + oldStatus);
                }
                break;
            case CANCELADO:
                if (oldStatus == OrderStatus.APROVADO || oldStatus == OrderStatus.EM_PROCESSAMENTO) {
                    // Se o pedido foi aprovado ou está em processamento, o crédito deve ser estornado
                    partner.setCurrentCredit(partner.getCurrentCredit().add(orderValue));
                    partnerRepository.save(partner);
                    log.info("Crédito de {} estornado para o parceiro ID {} devido ao cancelamento do pedido ID {}", orderValue, partner.getId(), id);
                }
                // Pedidos PENDENTES podem ser cancelados sem estorno de crédito
                break;
            case EM_PROCESSAMENTO:
                if (oldStatus != OrderStatus.APROVADO) {
                    throw new IllegalArgumentException("Um pedido só pode entrar em processamento se estiver APROVADO.");
                }
                break;
            case ENVIADO:
                if (oldStatus != OrderStatus.EM_PROCESSAMENTO) {
                    throw new IllegalArgumentException("Um pedido só pode ser enviado se estiver EM_PROCESSAMENTO.");
                }
                break;
            case ENTREGUE:
                if (oldStatus != OrderStatus.ENVIADO) {
                    throw new IllegalArgumentException("Um pedido só pode ser entregue se estiver ENVIADO.");
                }
                break;
            default:
                // Outros status podem ser atualizados diretamente ou com regras específicas
                break;
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);

        log.info("Status do pedido ID {} alterado de {} para {}", id, oldStatus, newStatus);
        notificationService.notifyOrderStatusChange(id, oldStatus, newStatus);

        return toResponseDTO(updatedOrder);
    }

    @Transactional
    public OrderResponseDTO cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));

        OrderStatus oldStatus = order.getStatus();

        if (oldStatus == OrderStatus.CANCELADO || oldStatus == OrderStatus.ENTREGUE) {
            throw new IllegalArgumentException("Não é possível cancelar um pedido com status " + oldStatus);
        }

        Partner partner = order.getPartner();
        BigDecimal orderValue = order.getTotalValue();

        // Estorna o crédito se o pedido já havia sido aprovado ou estava em processamento
        if (oldStatus == OrderStatus.APROVADO || oldStatus == OrderStatus.EM_PROCESSAMENTO) {
            partner.setCurrentCredit(partner.getCurrentCredit().add(orderValue));
            partnerRepository.save(partner);
            log.info("Crédito de {} estornado para o parceiro ID {} devido ao cancelamento do pedido ID {}", orderValue, partner.getId(), id);
        }

        order.setStatus(OrderStatus.CANCELADO);
        order.setUpdatedAt(LocalDateTime.now());
        Order cancelledOrder = orderRepository.save(order);

        log.info("Pedido ID {} cancelado. Status anterior: {}", id, oldStatus);
        notificationService.notifyOrderStatusChange(id, oldStatus, OrderStatus.CANCELADO);

        return toResponseDTO(cancelledOrder);
    }
}
