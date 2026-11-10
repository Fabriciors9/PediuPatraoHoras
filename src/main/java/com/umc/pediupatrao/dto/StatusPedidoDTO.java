package com.umc.pediupatrao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusPedidoDTO {
    private String status;
    private Long quantidade;
    private String corHex;
}
