package com.umc.pediupatrao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProdutoDTO {
    private String nome;
    private Integer quantidade;
    private Double faturamento;
    private Double percentual;
}
