package com.umc.pediupatrao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendasPeriodoDTO {
    private String data;
    private Double totalVendas;
    private Long quantidadePedidos;
}
