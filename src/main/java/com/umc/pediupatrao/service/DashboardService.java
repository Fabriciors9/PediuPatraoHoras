package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.*;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private PedidoRepository pedidoRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DashboardResumoDTO obterResumoCompleto(LocalDate dataInicio, LocalDate dataFim) {
        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime fim = dataFim != null ? dataFim.atTime(23, 59, 59) : LocalDate.now().atTime(23, 59, 59);

        List<Pedido> pedidos = pedidoRepository.findAll().stream()
                .filter(p -> p.getDataHoraEntrada() != null &&
                        !p.getDataHoraEntrada().isBefore(inicio) &&
                        !p.getDataHoraEntrada().isAfter(fim))
                .collect(Collectors.toList());

        Map<String, String> periodo = new HashMap<>();
        periodo.put("inicio", inicio.toLocalDate().format(DATE_FORMATTER));
        periodo.put("fim", fim.toLocalDate().format(DATE_FORMATTER));

        return DashboardResumoDTO.builder()
                .periodo(periodo)
                .metricas(calcularMetricas(pedidos))
                .vendasTempo(calcularVendasPorPeriodo(pedidos, inicio.toLocalDate(), fim.toLocalDate()))
                .statusPedidos(calcularStatusPedidos(pedidos))
                .topProdutos(calcularTopProdutos(pedidos, 5))
                .build();
    }

    public MetricasGeraisDTO calcularMetricas(List<Pedido> pedidos) {
        long totalPedidos = pedidos.size();
        List<Pedido> concluidos = pedidos.stream()
                .filter(p -> !"CANCELADO".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());

        double faturamento = concluidos.stream()
                .mapToDouble(p -> p.getValorTotal() != null ? p.getValorTotal() : 0.0)
                .sum();

        long qtdFaturados = concluidos.size();
        double ticketMedio = qtdFaturados > 0 ? faturamento / qtdFaturados : 0.0;

        double totalDescontos = concluidos.stream()
                .mapToDouble(p -> p.getValorDesconto() != null ? p.getValorDesconto() : 0.0)
                .sum();

        long pedidosComDesconto = concluidos.stream()
                .filter(p -> p.getValorDesconto() != null && p.getValorDesconto() > 0.0)
                .count();

        long cancelados = pedidos.stream()
                .filter(p -> "CANCELADO".equalsIgnoreCase(p.getStatus()))
                .count();

        double taxaCancelamento = totalPedidos > 0 ? ((double) cancelados / totalPedidos) * 100.0 : 0.0;

        return MetricasGeraisDTO.builder()
                .faturamentoTotal(arredondar(faturamento))
                .quantidadePedidos(totalPedidos)
                .pedidosFaturados(qtdFaturados)
                .ticketMedio(arredondar(ticketMedio))
                .totalDescontos(arredondar(totalDescontos))
                .pedidosComDesconto(pedidosComDesconto)
                .pedidosCancelados(cancelados)
                .taxaCancelamento(arredondar(taxaCancelamento))
                .build();
    }

    public List<VendasPeriodoDTO> calcularVendasPorPeriodo(List<Pedido> pedidos, LocalDate dataInicio, LocalDate dataFim) {
        Map<String, List<Pedido>> pedidosPorDia = pedidos.stream()
                .filter(p -> !"CANCELADO".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.groupingBy(p -> p.getDataHoraEntrada().toLocalDate().format(DATE_FORMATTER)));

        List<VendasPeriodoDTO> resultado = new ArrayList<>();
        LocalDate atual = dataInicio;
        while (!atual.isAfter(dataFim)) {
            String chave = atual.format(DATE_FORMATTER);
            List<Pedido> lista = pedidosPorDia.getOrDefault(chave, Collections.emptyList());
            double total = lista.stream().mapToDouble(p -> p.getValorTotal() != null ? p.getValorTotal() : 0.0).sum();
            resultado.add(new VendasPeriodoDTO(chave, arredondar(total), (long) lista.size()));
            atual = atual.plusDays(1);
        }
        return resultado;
    }

    public List<StatusPedidoDTO> calcularStatusPedidos(List<Pedido> pedidos) {
        Map<String, String> cores = Map.of(
                "RECEBIDO", "#6c757d",
                "EM_PREPARACAO", "#ffc107",
                "PRONTO", "#0dcaf0",
                "SAIU_PARA_ENTREGA", "#0d6efd",
                "RETIRADO", "#6f42c1",
                "FINALIZADO", "#198754",
                "CANCELADO", "#dc3545"
        );

        Map<String, Long> contagens = pedidos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getStatus() != null ? p.getStatus().toUpperCase() : "RECEBIDO",
                        Collectors.counting()
                ));

        List<StatusPedidoDTO> lista = new ArrayList<>();
        contagens.forEach((status, count) -> {
            lista.add(new StatusPedidoDTO(status, count, cores.getOrDefault(status, "#adb5bd")));
        });

        lista.sort(Comparator.comparing(StatusPedidoDTO::getQuantidade).reversed());
        return lista;
    }

    public List<TopProdutoDTO> calcularTopProdutos(List<Pedido> pedidos, int limite) {
        List<Pedido> concluidos = pedidos.stream()
                .filter(p -> !"CANCELADO".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());

        Map<String, List<Pedido>> porProduto = concluidos.stream()
                .filter(p -> p.getSabor() != null && !p.getSabor().trim().isEmpty())
                .collect(Collectors.groupingBy(p -> p.getSabor().trim()));

        int totalItensVendidos = concluidos.stream()
                .mapToInt(p -> p.getQuantidade() > 0 ? p.getQuantidade() : 1)
                .sum();

        List<TopProdutoDTO> ranking = new ArrayList<>();
        porProduto.forEach((sabor, lista) -> {
            int qtd = lista.stream().mapToInt(p -> p.getQuantidade() > 0 ? p.getQuantidade() : 1).sum();
            double fat = lista.stream().mapToDouble(p -> p.getValorTotal() != null ? p.getValorTotal() : 0.0).sum();
            double perc = totalItensVendidos > 0 ? ((double) qtd / totalItensVendidos) * 100.0 : 0.0;
            ranking.add(new TopProdutoDTO(sabor, qtd, arredondar(fat), arredondar(perc)));
        });

        ranking.sort(Comparator.comparing(TopProdutoDTO::getQuantidade).reversed());
        return ranking.stream().limit(limite).collect(Collectors.toList());
    }

    private double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
