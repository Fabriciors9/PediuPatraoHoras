package com.umc.pediupatrao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResumoDTO {
    private Map<String, String> periodo;
    private MetricasGeraisDTO metricas;
    private List<VendasPeriodoDTO> vendasTempo;
    private List<StatusPedidoDTO> statusPedidos;
    private List<TopProdutoDTO> topProdutos;
}
