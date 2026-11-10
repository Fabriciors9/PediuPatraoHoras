package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private PedidoService pedidoService;

    private Pedido pedidoBase;

    @BeforeEach
    void setUp() {
        pedidoBase = new Pedido();
        pedidoBase.setId("PED-123");
        pedidoBase.setSabor("Pizza Calabresa Especial");
        pedidoBase.setQuantidade(2);
        pedidoBase.setPrecoUnitario(45.0);
        pedidoBase.setStatus("PRONTO");
    }

    @Test
    @DisplayName("Preco do catalogo deve sobrescrever valor enviado pelo cliente")
    void testCriacaoPedidoUsaPrecoDoServidor() {
        Produto produtoCatalogo = new Produto();
        produtoCatalogo.setNome("Pizza Calabresa Especial");
        produtoCatalogo.setPreco(55.0);

        when(produtoRepository.findAll()).thenReturn(List.of(produtoCatalogo));
        when(auditoriaService.getUsuarioAtual()).thenReturn("atendente_teste");
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido pedidoComPrecoManipulado = new Pedido();
        pedidoComPrecoManipulado.setSabor("Pizza Calabresa Especial");
        pedidoComPrecoManipulado.setPrecoUnitario(1.99);
        pedidoComPrecoManipulado.setQuantidade(2);

        Pedido resultado = pedidoService.criarPedido(pedidoComPrecoManipulado);

        assertEquals(55.0, resultado.getPrecoUnitario());
        assertEquals(110.0, resultado.getValorTotal());
    }

    @Test
    @DisplayName("Gerente pode aplicar desconto de ate 20% antes da saida")
    void testAplicarDescontoValidoComSucesso() {
        when(auditoriaService.getPerfilAtual()).thenReturn("GERENTE");
        when(pedidoRepository.findById("PED-123")).thenReturn(Optional.of(pedidoBase));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido atualizado = pedidoService.aplicarDesconto("PED-123", 10.0);

        assertEquals(10.0, atualizado.getPercentualDesconto());
        assertEquals(9.0, atualizado.getValorDesconto());
        assertEquals(81.0, atualizado.getValorTotal());
    }

    @Test
    @DisplayName("Deve rejeitar desconto acima de 20%")
    void testRejeitarDescontoAcimaDoLimite() {
        when(auditoriaService.getPerfilAtual()).thenReturn("GERENTE");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            pedidoService.aplicarDesconto("PED-123", 25.0);
        });

        assertTrue(ex.getMessage().contains("20.0%"));
    }

    @Test
    @DisplayName("Bloqueia desconto se o pedido ja saiu para entrega")
    void testBloquearDescontoAposSaida() {
        when(auditoriaService.getPerfilAtual()).thenReturn("GERENTE");
        pedidoBase.setStatus("SAIU_PARA_ENTREGA");
        pedidoBase.setDataHoraSaida(LocalDateTime.now());
        when(pedidoRepository.findById("PED-123")).thenReturn(Optional.of(pedidoBase));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            pedidoService.aplicarDesconto("PED-123", 10.0);
        });

        assertTrue(ex.getMessage().contains("Desconto bloqueado"));
    }

    @Test
    @DisplayName("Bloqueia cancelamento se o pedido ja saiu ou foi retirado")
    void testBloquearCancelamentoAposSaida() {
        when(auditoriaService.getPerfilAtual()).thenReturn("GERENTE");
        pedidoBase.setStatus("RETIRADO");
        pedidoBase.setDataHoraSaida(LocalDateTime.now());
        when(pedidoRepository.findById("PED-123")).thenReturn(Optional.of(pedidoBase));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            pedidoService.cancelarPedido("PED-123", "Cliente desistiu");
        });

        assertTrue(ex.getMessage().contains("Cancelamento bloqueado"));
    }

    @Test
    @DisplayName("Cancelamento exige justificativa textual")
    void testCancelamentoExigeJustificativa() {
        when(auditoriaService.getPerfilAtual()).thenReturn("GERENTE");

        assertThrows(IllegalArgumentException.class, () -> {
            pedidoService.cancelarPedido("PED-123", "   ");
        });
    }
}
