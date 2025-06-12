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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço para gerenciar operações relacionadas a pedidos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final PartnerRepository partnerRepository;
    private final NotificationService notificationService;

    /**
     * Converte uma entidade Order para um DTO de resposta.
     * @param order A entidade Order.
     * @return O OrderResponseDTO correspondente.
     */
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

    /**
     * Cria um novo pedido.
     * O pedido é inicialmente criado com status PENDENTE. A verificação e débito de crédito
     * ocorrerão somente na transição para o status APROVADO.
     * @param orderRequestDTO Os dados do pedido a serem criados.
     * @return O OrderResponseDTO do pedido criado.
     * @throws ResourceNotFoundException se o parceiro não for encontrado.
     */
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO orderRequestDTO) {
        // Busca o parceiro SEM bloqueio aqui, pois o pedido é PENDENTE inicialmente
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

        // Calcula o valor total do pedido
        BigDecimal totalOrderValue = order.calculateTotalValue();
        order.setTotalValue(totalOrderValue);

        Order savedOrder = orderRepository.save(order);
        log.info("Pedido ID {} criado para o parceiro ID {}", savedOrder.getId(), partner.getId());
        notificationService.notifyOrderStatusChange(savedOrder.getId(), null, savedOrder.getStatus()); // Notifica a criação

        return toResponseDTO(savedOrder);
    }

    /**
     * Busca um pedido pelo ID.
     * @param id O ID do pedido.
     * @return O OrderResponseDTO do pedido encontrado.
     * @throws ResourceNotFoundException se o pedido não for encontrado.
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        return toResponseDTO(order);
    }

    /**
     * Busca pedidos por ID do parceiro.
     * @param partnerId O ID do parceiro.
     * @return Uma lista de OrderResponseDTOs.
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByPartnerId(Long partnerId) {
        List<Order> orders = orderRepository.findByPartnerId(partnerId);
        return orders.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    /**
     * Busca pedidos por período de criação.
     * @param startDate A data de início do período.
     * @param endDate A data de fim do período.
     * @return Uma lista de OrderResponseDTOs.
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByCreationPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findByCreatedAtBetween(startDate, endDate);
        return orders.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    /**
     * Busca pedidos por status.
     * @param status O status do pedido.
     * @return Uma lista de OrderResponseDTOs.
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        return orders.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    /**
     * Atualiza o status de um pedido.
     * Implementa a lógica de débito/crédito de acordo com a mudança de status,
     * utilizando bloqueio pessimista para garantir a consistência do crédito do parceiro.
     * @param id O ID do pedido.
     * @param newStatus O novo status a ser aplicado.
     * @return O OrderResponseDTO do pedido atualizado.
     * @throws ResourceNotFoundException se o pedido ou parceiro não forem encontrados.
     * @throws CreditLimitExceededException se o limite de crédito for excedido ao aprovar.
     * @throws IllegalArgumentException se a transição de status for inválida.
     */
    @Transactional
    public OrderResponseDTO updateOrderStatus(Long id, OrderStatus newStatus) {
        // Primeiro, busca o pedido para obter informações básicas.
        // Nota: A entidade 'order' aqui não está bloqueada em si,
        // mas precisamos do 'partner.id' dela.
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));

        OrderStatus oldStatus = order.getStatus();

        // Se o status não mudou, apenas retorna o pedido atual.
        if (oldStatus == newStatus) {
            log.warn("Tentativa de atualizar o pedido ID {} para o mesmo status: {}", id, newStatus);
            return toResponseDTO(order);
        }

        // --- Ponto Crítico: Bloqueio Pessimista do Parceiro para operações de crédito ---
        // A chave aqui é buscar o Partner usando o método 'findById' do PartnerRepository,
        // que agora está anotado com @Lock(LockModeType.PESSIMISTIC_WRITE).
        // Isso força o banco de dados a bloquear a linha do Partner, impedindo que outras
        // transações a modifiquem até que esta transação seja concluída.
        Partner partner = partnerRepository.findById(order.getPartner().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Parceiro não encontrado para o pedido ID: " + id));
        // Agora, 'partner' é a entidade bloqueada, e podemos fazer a verificação e o débito/crédito.

        BigDecimal orderValue = order.getTotalValue();

        switch (newStatus) {
            case APROVADO:
                if (oldStatus == OrderStatus.PENDENTE) {
                    if (partner.getCurrentCredit().compareTo(orderValue) < 0) {
                        throw new CreditLimitExceededException("Parceiro ID " + partner.getId() + " não tem crédito suficiente para aprovar este pedido. Crédito disponível: " + partner.getCurrentCredit() + ", Valor do pedido: " + orderValue);
                    }
                    // Debita o crédito. Como o partner está bloqueado, esta operação é segura.
                    partner.setCurrentCredit(partner.getCurrentCredit().subtract(orderValue));
                    partnerRepository.save(partner); // O save liberará o bloqueio ao final da transação
                    log.info("Crédito de {} debitado do parceiro ID {} para o pedido ID {}", orderValue, partner.getId(), id);
                } else {
                    throw new IllegalArgumentException("Não é possível aprovar um pedido com status " + oldStatus);
                }
                break;
            case CANCELADO:
                if (oldStatus == OrderStatus.APROVADO || oldStatus == OrderStatus.EM_PROCESSAMENTO) {
                    // Estorna o crédito. Como o partner está bloqueado, esta operação é segura.
                    partner.setCurrentCredit(partner.getCurrentCredit().add(orderValue));
                    partnerRepository.save(partner); // O save liberará o bloqueio ao final da transação
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
        Order updatedOrder = orderRepository.save(order); // Salva a mudança de status do pedido

        log.info("Status do pedido ID {} alterado de {} para {}", id, oldStatus, newStatus);
        notificationService.notifyOrderStatusChange(id, oldStatus, newStatus);

        return toResponseDTO(updatedOrder);
    }

    /**
     * Cancela um pedido.
     * Estorna o crédito do parceiro se o pedido já estava APROVADO ou EM_PROCESSAMENTO.
     * @param id O ID do pedido a ser cancelado.
     * @return O OrderResponseDTO do pedido cancelado.
     * @throws ResourceNotFoundException se o pedido não for encontrado.
     * @throws IllegalArgumentException se o pedido já estiver em um status final (ENTREGUE, CANCELADO).
     */
    @Transactional
    public OrderResponseDTO cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));

        OrderStatus oldStatus = order.getStatus();

        if (oldStatus == OrderStatus.CANCELADO || oldStatus == OrderStatus.ENTREGUE) {
            throw new IllegalArgumentException("Não é possível cancelar um pedido com status " + oldStatus);
        }

        // --- Ponto Crítico: Bloqueio Pessimista do Parceiro para operações de crédito ---
        // Assim como no updateOrderStatus, buscamos o Partner com bloqueio para garantir a atomicidade do estorno.
        Partner partner = partnerRepository.findById(order.getPartner().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Parceiro não encontrado para o pedido ID: " + id));

        BigDecimal orderValue = order.getTotalValue();

        // Estorna o crédito se o pedido já havia sido aprovado ou estava em processamento
        if (oldStatus == OrderStatus.APROVADO || oldStatus == OrderStatus.EM_PROCESSAMENTO) {
            partner.setCurrentCredit(partner.getCurrentCredit().add(orderValue));
            partnerRepository.save(partner); // O save liberará o bloqueio ao final da transação
            log.info("Crédito de {} estornado para o parceiro ID {} devido ao cancelamento do pedido ID {}", orderValue, partner.getId(), id);
        }

        order.setStatus(OrderStatus.CANCELADO);
        order.setUpdatedAt(LocalDateTime.now());
        Order cancelledOrder = orderRepository.save(order);

        log.info("Pedido ID {} cancelado. Status anterior: {}", id, oldStatus);
        notificationService.notifyOrderStatusChange(id, oldStatus, OrderStatus.CANCELADO);

        return toResponseDTO(cancelledOrder);
    }

    public List<OrderResponseDTO> getOrders() {
        List<Order> all = orderRepository.findAll();
        List<OrderResponseDTO> collect = all.stream().map(this::toResponseDTO).collect(Collectors.toList());
        return collect;
    }
}
