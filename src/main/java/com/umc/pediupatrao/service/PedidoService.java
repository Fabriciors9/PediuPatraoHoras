package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    // Criacao de pedido com busca de preco no catalogo
    @Transactional
    public Pedido criarPedido(Pedido pedido) {
        pedido.setStatus("RECEBIDO");
        pedido.setDataHoraEntrada(LocalDateTime.now());
        pedido.setResponsavelRegistro(auditoriaService.getUsuarioAtual());

        if (pedido.getTipoPizza() == null || pedido.getTipoPizza().trim().isEmpty()) {
            pedido.setTipoPizza("8 fatias");
        }

        // Pega o preco direto do cadastro do produto para evitar manipulacao pelo cliente
        double precoCalculado = 45.0;
        if (pedido.getSabor() != null && !pedido.getSabor().trim().isEmpty()) {
            Optional<Produto> produtoOptional = produtoRepository.findAll().stream()
                    .filter(p -> p.getNome().trim().equalsIgnoreCase(pedido.getSabor().trim()))
                    .findFirst();

            if (produtoOptional.isPresent() && produtoOptional.get().getPreco() != null) {
                precoCalculado = produtoOptional.get().getPreco();
            }
        }
        pedido.setPrecoUnitario(precoCalculado);

        if (pedido.getQuantidade() <= 0) {
            pedido.setQuantidade(1);
        }

        pedido.setPercentualDesconto(0.0);
        pedido.calcularValores();

        Pedido salvo = pedidoRepository.save(pedido);

        auditoriaService.registrar("CRIACAO_PEDIDO", "Pedido", salvo.getId(),
                "Pedido cadastrado por " + salvo.getResponsavelRegistro() + " - " + salvo.getQuantidade() + 
                "x " + salvo.getSabor() + " (R$ " + precoCalculado + " un) - Total calculado: R$ " + salvo.getValorTotal());

        return salvo;
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public Pedido buscarPorId(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido nao encontrado: " + id));
    }

    // Transicoes de status do pedido
    @Transactional
    public Pedido atualizarStatus(String id, String novoStatus) {
        Pedido pedido = buscarPorId(id);
        String statusAtual = pedido.getStatus() != null ? pedido.getStatus().toUpperCase() : "RECEBIDO";
        String statusDestino = novoStatus != null ? novoStatus.toUpperCase() : "";

        if ("CANCELADO".equals(statusAtual)) {
            throw new IllegalStateException("O pedido #" + id + " esta cancelado e nao pode mudar de status.");
        }
        if ("FINALIZADO".equals(statusAtual)) {
            throw new IllegalStateException("O pedido #" + id + " ja esta finalizado.");
        }

        boolean transicaoValida = false;
        if ("RECEBIDO".equals(statusAtual) && ("EM_PREPARACAO".equals(statusDestino) || "EM_PREPARO".equals(statusDestino))) {
            transicaoValida = true;
        } else if (("EM_PREPARACAO".equals(statusAtual) || "EM_PREPARO".equals(statusAtual)) && "PRONTO".equals(statusDestino)) {
            transicaoValida = true;
        } else if ("PRONTO".equals(statusAtual) && ("SAIU_PARA_ENTREGA".equals(statusDestino) || "RETIRADO".equals(statusDestino))) {
            transicaoValida = true;
            pedido.setDataHoraSaida(LocalDateTime.now());
            pedido.setResponsavelSaida(auditoriaService.getUsuarioAtual());
        } else if (("SAIU_PARA_ENTREGA".equals(statusAtual) || "RETIRADO".equals(statusAtual)) && "FINALIZADO".equals(statusDestino)) {
            transicaoValida = true;
        }

        if (!transicaoValida) {
            throw new IllegalArgumentException("Transicao invalida de '" + statusAtual + "' para '" + statusDestino + "'. Fluxo: RECEBIDO -> EM_PREPARACAO -> PRONTO -> SAIU_PARA_ENTREGA/RETIRADO -> FINALIZADO.");
        }

        pedido.setStatus(statusDestino);
        Pedido atualizado = pedidoRepository.save(pedido);

        auditoriaService.registrarMudancaCampo("TRANSICAO_STATUS", "Pedido", id, "status", 
                statusAtual, statusDestino, "Status alterado de " + statusAtual + " para " + statusDestino, null);

        return atualizado;
    }

    // Desconto exclusivo do gerente, apenas antes da saida
    @Transactional
    public Pedido aplicarDesconto(String id, Double percentual) {
        String perfil = auditoriaService.getPerfilAtual();
        if (!"GERENTE".equalsIgnoreCase(perfil)) {
            throw new SecurityException("Acesso negado: Somente o gerente pode aplicar descontos.");
        }

        if (percentual == null || percentual < 0 || percentual > 20.0) {
            throw new IllegalArgumentException("O desconto deve ficar entre 0% e 20.0%.");
        }

        Pedido pedido = buscarPorId(id);
        String status = pedido.getStatus() != null ? pedido.getStatus().toUpperCase() : "";

        // Nao permite desconto se o pedido ja saiu para entrega ou foi finalizado
        if ("SAIU_PARA_ENTREGA".equals(status) || "RETIRADO".equals(status) 
                || "FINALIZADO".equals(status) || "CANCELADO".equals(status) 
                || pedido.getDataHoraSaida() != null) {
            throw new IllegalStateException("Desconto bloqueado: pedido ja saiu para entrega ou foi concluido.");
        }

        Double totalAntigo = pedido.getValorTotal();
        Double descAntigo = pedido.getPercentualDesconto();

        pedido.setPercentualDesconto(percentual);
        pedido.calcularValores();

        Pedido salvo = pedidoRepository.save(pedido);

        String descMsg = percentual == 0.0 ? 
                "Desconto removido pelo gerente. Total anterior: R$ " + totalAntigo + " -> Novo total: R$ " + salvo.getValorTotal() :
                "Desconto de " + percentual + "% aplicado pelo gerente. Total anterior: R$ " + totalAntigo + " -> Novo total: R$ " + salvo.getValorTotal();

        auditoriaService.registrarMudancaCampo("DESCONTO_PEDIDO", "Pedido", id, "percentualDesconto",
                String.valueOf(descAntigo) + "%", String.valueOf(percentual) + "%", descMsg,
                percentual == 0.0 ? "Desconto zerado pelo gerente" : "Desconto autorizado pelo gerente");

        return salvo;
    }

    // Cancelamento com justificativa, apenas antes da saida
    @Transactional
    public Pedido cancelarPedido(String id, String justificativa) {
        String perfil = auditoriaService.getPerfilAtual();
        if (!"GERENTE".equalsIgnoreCase(perfil)) {
            throw new SecurityException("Acesso negado: Somente o gerente pode cancelar pedidos.");
        }

        if (justificativa == null || justificativa.trim().isEmpty()) {
            throw new IllegalArgumentException("A justificativa do cancelamento e obrigatoria.");
        }

        Pedido pedido = buscarPorId(id);
        String status = pedido.getStatus() != null ? pedido.getStatus().toUpperCase() : "";

        // Nao cancela se ja saiu da pizzaria
        if ("SAIU_PARA_ENTREGA".equals(status) || "RETIRADO".equals(status) 
                || "FINALIZADO".equals(status) || "CANCELADO".equals(status)
                || pedido.getDataHoraSaida() != null) {
            throw new IllegalStateException("Cancelamento bloqueado: pedido ja saiu para entrega ou foi finalizado.");
        }

        String statusAnterior = pedido.getStatus();
        pedido.setStatus("CANCELADO");
        pedido.setJustificativaCancelamento(justificativa.trim());

        Pedido salvo = pedidoRepository.save(pedido);

        auditoriaService.registrarMudancaCampo("CANCELAMENTO_PEDIDO", "Pedido", id, "status",
                statusAnterior, "CANCELADO", 
                "Pedido cancelado pelo gerente. Justificativa: " + justificativa, justificativa);

        return salvo;
    }
}
