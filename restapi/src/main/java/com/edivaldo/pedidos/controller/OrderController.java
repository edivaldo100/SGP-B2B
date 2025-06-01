package com.edivaldo.pedidos.controller;

import com.edivaldo.pedidos.dto.OrderRequestDTO;
import com.edivaldo.pedidos.dto.OrderResponseDTO;
import com.edivaldo.pedidos.enums.OrderStatus;
import com.edivaldo.pedidos.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "API para gerenciamento de pedidos B2B")
public class OrderController {

    private final OrderService orderService;

    /**
     * Cadastra um novo pedido.
     * @param orderRequestDTO Os dados do pedido a serem criados.
     * @return ResponseEntity com o OrderResponseDTO do pedido criado e status HTTP 201.
     */
    @Operation(summary = "Cadastra um novo pedido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou crédito insuficiente"),
            @ApiResponse(responseCode = "404", description = "Parceiro não encontrado")
    })
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO orderRequestDTO) {
        OrderResponseDTO createdOrder = orderService.createOrder(orderRequestDTO);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    /**
     * Consulta um pedido por ID.
     * @param id O ID do pedido.
     * @return ResponseEntity com o OrderResponseDTO do pedido encontrado e status HTTP 200.
     */
    @Operation(summary = "Consulta um pedido por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long id) {
        OrderResponseDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Consulta pedidos com base em diferentes critérios (ID do parceiro, período, status).
     * @param partnerId (Opcional) ID do parceiro para filtrar pedidos.
     * @param startDate (Opcional) Data de início para filtrar pedidos por período de criação (formato yyyy-MM-dd'T'HH:mm:ss).
     * @param endDate (Opcional) Data de fim para filtrar pedidos por período de criação (formato yyyy-MM-dd'T'HH:mm:ss).
     * @param status (Opcional) Status do pedido para filtrar.
     * @return ResponseEntity com uma lista de OrderResponseDTOs e status HTTP 200.
     */
    @Operation(summary = "Consulta pedidos por ID do parceiro, período de criação ou status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedidos consultados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de consulta inválidos")
    })
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> searchOrders(
            @Parameter(description = "ID do parceiro para filtrar pedidos")
            @RequestParam(required = false) Long partnerId,
            @Parameter(description = "Data de início para filtrar por período de criação (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Data de fim para filtrar por período de criação (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Status do pedido para filtrar")
            @RequestParam(required = false) OrderStatus status) {

        if (partnerId != null) {
            return ResponseEntity.ok(orderService.getOrdersByPartnerId(partnerId));
        } else if (startDate != null && endDate != null) {
            return ResponseEntity.ok(orderService.getOrdersByCreationPeriod(startDate, endDate));
        } else if (status != null) {
            return ResponseEntity.ok(orderService.getOrdersByStatus(status));
        } else {
            return ResponseEntity.ok(orderService.getOrders());
        }
    }

    /**
     * Atualiza o status de um pedido.
     * @param id O ID do pedido.
     * @param newStatus O novo status a ser aplicado.
     * @return ResponseEntity com o OrderResponseDTO do pedido atualizado e status HTTP 200.
     */
    @Operation(summary = "Atualiza o status de um pedido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status do pedido atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Transição de status inválida ou crédito insuficiente"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(@PathVariable Long id,
                                                              @Parameter(description = "Novo status do pedido", required = true)
                                                              @RequestParam OrderStatus newStatus) {
        OrderResponseDTO updatedOrder = orderService.updateOrderStatus(id, newStatus);
        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * Cancela um pedido.
     * @param id O ID do pedido a ser cancelado.
     * @return ResponseEntity com o OrderResponseDTO do pedido cancelado e status HTTP 200.
     */
    @Operation(summary = "Cancela um pedido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido cancelado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não é possível cancelar o pedido neste status"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable Long id) {
        OrderResponseDTO cancelledOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(cancelledOrder);
    }
}
