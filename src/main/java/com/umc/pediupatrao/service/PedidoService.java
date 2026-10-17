package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    // RF02: Criação de pedido com valores calculados no servidor
    public Pedido criarPedido(Pedido pedido) {
        pedido.setStatus("RECEBIDO");
        pedido.setDataHoraEntrada(java.time.LocalDateTime.now());
        pedido.setResponsavelRegistro(auditoriaService.getUsuarioAtual());

        // Tamanho padrão fixo em 8 fatias
        if (pedido.getTipoPizza() == null || pedido.getTipoPizza().trim().isEmpty()) {
            pedido.setTipoPizza("8 fatias");
        }

        // Preço padrão se não informado
        if (pedido.getPrecoUnitario() == null || pedido.getPrecoUnitario() <= 0) {
            pedido.setPrecoUnitario(45.0); // preço médio de pizza na casa
        }
        if (pedido.getQuantidade() <= 0) {
            pedido.setQuantidade(1);
        }

        // Não permite desconto no ato de criação por atendente (desconto é exclusivo do gerente)
        pedido.setPercentualDesconto(0.0);
        pedido.calcularValores();

        Pedido salvo = pedidoRepository.save(pedido);

        // RF05: Auditoria de novo pedido
        auditoriaService.registrar("CRIACAO_PEDIDO", "Pedido", salvo.getId(),
                "Pedido cadastrado por " + salvo.getResponsavelRegistro() + " - " + salvo.getQuantidade() + 
                "x " + salvo.getSabor() + " - Total calculado: R$ " + salvo.getValorTotal());

        return salvo;
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public Pedido buscarPorId(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + id));
    }

    // RF03: Controle estrito de transições de status
    public Pedido atualizarStatus(String id, String novoStatus) {
        Pedido pedido = buscarPorId(id);
        String statusAtual = pedido.getStatus() != null ? pedido.getStatus().toUpperCase() : "RECEBIDO";
        String statusDestino = novoStatus != null ? novoStatus.toUpperCase() : "";

        // Se já está cancelado, bloqueia qualquer mudança
        if ("CANCELADO".equals(statusAtual)) {
            throw new IllegalStateException("O pedido #" + id + " está CANCELADO e não pode ter o status alterado.");
        }

        // Se já está finalizado, encerrou o fluxo
        if ("FINALIZADO".equals(statusAtual)) {
            throw new IllegalStateException("O pedido #" + id + " já está FINALIZADO.");
        }

        // Validação da máquina de estados obrigatória (RF03)
        boolean transicaoValida = false;

        if ("RECEBIDO".equals(statusAtual) && ("EM_PREPARACAO".equals(statusDestino) || "EM_PREPARO".equals(statusDestino))) {
            transicaoValida = true;
        } else if (("EM_PREPARACAO".equals(statusAtual) || "EM_PREPARO".equals(statusAtual)) && "PRONTO".equals(statusDestino)) {
            transicaoValida = true;
        } else if ("PRONTO".equals(statusAtual) && ("SAIU_PARA_ENTREGA".equals(statusDestino) || "RETIRADO".equals(statusDestino))) {
            transicaoValida = true;
            // RF03: Registrar obrigatoriamente data/hora e responsável pela saída
            pedido.setDataHoraSaida(java.time.LocalDateTime.now());
            pedido.setResponsavelSaida(auditoriaService.getUsuarioAtual());
        } else if (("SAIU_PARA_ENTREGA".equals(statusAtual) || "RETIRADO".equals(statusAtual)) && "FINALIZADO".equals(statusDestino)) {
            transicaoValida = true;
        }

        if (!transicaoValida) {
            throw new IllegalArgumentException("Transição inválida! Não é permitido mudar de '" + statusAtual + "' para '" + statusDestino + "'. Siga o fluxo: RECEBIDO -> EM_PREPARACAO -> PRONTO -> SAIU_PARA_ENTREGA/RETIRADO -> FINALIZADO.");
        }

        pedido.setStatus(statusDestino);
        Pedido atualizado = pedidoRepository.save(pedido);

        // RF05: Auditoria de transição de status
        auditoriaService.registrarMudancaCampo("TRANSICAO_STATUS", "Pedido", id, "status", 
                statusAtual, statusDestino, "Status alterado de " + statusAtual + " para " + statusDestino, null);

        return atualizado;
    }

    // RF04: Aplicação de desconto exclusiva do GERENTE (máx 20% e antes da saída)
    public Pedido aplicarDesconto(String id, Double percentual) {
        String perfil = auditoriaService.getPerfilAtual();
        if (!"GERENTE".equalsIgnoreCase(perfil)) {
            throw new SecurityException("Acesso negado: Somente o GERENTE tem permissão para conceder descontos.");
        }

        if (percentual == null || percentual < 0 || percentual > 20.0) {
            throw new IllegalArgumentException("O desconto deve ser entre 0% e 20.0% (limite do RF04).");
        }

        Pedido pedido = buscarPorId(id);
        String status = pedido.getStatus() != null ? pedido.getStatus().toUpperCase() : "";

        // Só não pode aplicar se o pedido já estiver finalizado ou cancelado
        if ("FINALIZADO".equals(status) || "CANCELADO".equals(status)) {
            throw new IllegalStateException("Desconto não permitido! O pedido já foi finalizado ou cancelado.");
        }

        Double totalAntigo = pedido.getValorTotal();
        Double descAntigo = pedido.getPercentualDesconto();

        pedido.setPercentualDesconto(percentual);
        pedido.calcularValores();

        Pedido salvo = pedidoRepository.save(pedido);

        // RF05: Auditoria de desconto aplicado ou removido
        String descMsg = percentual == 0.0 ? "Desconto removido pelo Gerente. Total anterior: R$ " + totalAntigo + " -> Novo total: R$ " + salvo.getValorTotal() : "Desconto de " + percentual + "% aplicado pelo gerente. Total anterior: R$ " + totalAntigo + " -> Novo total: R$ " + salvo.getValorTotal();
        auditoriaService.registrarMudancaCampo("DESCONTO_PEDIDO", "Pedido", id, "percentualDesconto",
                String.valueOf(descAntigo) + "%", String.valueOf(percentual) + "%",
                descMsg,
                percentual == 0.0 ? "Desconto zerado/removido pelo Gerente" : "Desconto comercial autorizado pelo Gerente");

        return salvo;
    }

    // RF03 e RF04: Cancelamento exclusivo do GERENTE com justificativa
    public Pedido cancelarPedido(String id, String justificativa) {
        String perfil = auditoriaService.getPerfilAtual();
        if (!"GERENTE".equalsIgnoreCase(perfil)) {
            throw new SecurityException("Acesso negado: Somente o GERENTE tem autorização para cancelar pedidos.");
        }

        if (justificativa == null || justificativa.trim().isEmpty()) {
            throw new IllegalArgumentException("A justificativa do cancelamento é obrigatória (RF04).");
        }

        Pedido pedido = buscarPorId(id);
        String status = pedido.getStatus() != null ? pedido.getStatus().toUpperCase() : "";

        if ("CANCELADO".equals(status) || "FINALIZADO".equals(status)) {
            throw new IllegalStateException("Não é permitido cancelar pedido que já foi finalizado ou cancelado.");
        }

        String statusAnterior = pedido.getStatus();
        pedido.setStatus("CANCELADO");
        pedido.setJustificativaCancelamento(justificativa.trim());

        Pedido salvo = pedidoRepository.save(pedido);

        // RF05: Auditoria de cancelamento com justificativa
        auditoriaService.registrarMudancaCampo("CANCELAMENTO_PEDIDO", "Pedido", id, "status",
                statusAnterior, "CANCELADO", 
                "Pedido cancelado pelo Gerente. Justificativa: " + justificativa, 
                justificativa);

        return salvo;
    }

}
