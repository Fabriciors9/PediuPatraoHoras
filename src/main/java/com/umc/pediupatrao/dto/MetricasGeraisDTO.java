package com.umc.pediupatrao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricasGeraisDTO {
    private Double faturamentoTotal;
    private Long quantidadePedidos;
    private Long pedidosFaturados;
    private Double ticketMedio;
    private Double totalDescontos;
    private Long pedidosComDesconto;
    private Long pedidosCancelados;
    private Double taxaCancelamento;
}
