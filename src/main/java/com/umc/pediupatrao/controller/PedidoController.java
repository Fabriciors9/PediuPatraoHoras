package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @PostMapping
    public Pedido criarPedido(@RequestBody Pedido pedido) {
        return pedidoService.criarPedido(pedido);
    }

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(@PathVariable String id, @RequestParam String status) {
        try {
            Pedido atualizado = pedidoService.atualizarStatus(id, status);
            return ResponseEntity.ok(atualizado);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", e.getMessage()));
        }
    }

    // RF04: Endpoint REST para aplicação de desconto exclusivo do Gerente
    @PutMapping("/{id}/desconto")
    public ResponseEntity<?> aplicarDesconto(@PathVariable String id, @RequestParam Double percentual) {
        try {
            Pedido atualizado = pedidoService.aplicarDesconto(id, percentual);
            return ResponseEntity.ok(atualizado);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", e.getMessage()));
        }
    }

    // RF03 e RF04: Endpoint REST para cancelamento de pedido com justificativa
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarPedido(@PathVariable String id, @RequestParam(required = false) String justificativa) {
        try {
            Pedido cancelado = pedidoService.cancelarPedido(id, justificativa);
            return ResponseEntity.ok(cancelado);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", e.getMessage()));
        }
    }
    
    
}
