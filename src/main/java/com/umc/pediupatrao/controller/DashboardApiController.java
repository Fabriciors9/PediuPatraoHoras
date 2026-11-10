package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.*;
import com.umc.pediupatrao.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class DashboardApiController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/resumo-completo")
    public ResponseEntity<DashboardResumoDTO> getResumoCompleto(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(dashboardService.obterResumoCompleto(inicio, fim));
    }

    @GetMapping("/metricas")
    public ResponseEntity<MetricasGeraisDTO> getMetricas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(dashboardService.obterResumoCompleto(inicio, fim).getMetricas());
    }

    @GetMapping("/vendas-tempo")
    public ResponseEntity<List<VendasPeriodoDTO>> getVendasTempo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(dashboardService.obterResumoCompleto(inicio, fim).getVendasTempo());
    }

    @GetMapping("/status-pedidos")
    public ResponseEntity<List<StatusPedidoDTO>> getStatusPedidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(dashboardService.obterResumoCompleto(inicio, fim).getStatusPedidos());
    }

    @GetMapping("/top-produtos")
    public ResponseEntity<List<TopProdutoDTO>> getTopProdutos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "5") int limite) {
        return ResponseEntity.ok(dashboardService.obterResumoCompleto(inicio, fim).getTopProdutos());
    }
}
