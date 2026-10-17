package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public Cliente novoCliente(Cliente cliente) {
        return salvar(cliente);
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // Método para excluir cliente com auditoria
    public void excluir(String id) {
        clienteRepository.findById(id).ifPresent(c -> {
            auditoriaService.registrar("EXCLUSAO_CLIENTE", "Cliente", id, "Cliente removido: " + c.getNome());
        });
        clienteRepository.deleteById(id);
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Salva novo cliente ou audita alterações detalhadas se for edição
    public Cliente salvar(Cliente cliente) {
        // Novo cliente
        if (cliente.getId() == null || cliente.getId().trim().isEmpty()) {
            cliente.setId(null);
            Cliente salvo = clienteRepository.save(cliente);
            auditoriaService.registrar("CADASTRO_CLIENTE", "Cliente", salvo.getId(), 
                                      "Cadastro do cliente: " + salvo.getNome() + " (Tel: " + salvo.getTelefone() + ")");
            return salvo;
        }

        // Atualização de cliente existente: audita campos modificados conforme RF05
        Optional<Cliente> optAntigo = clienteRepository.findById(cliente.getId());
        if (optAntigo.isPresent()) {
            Cliente antigo = optAntigo.get();
            auditarCamposModificados(antigo, cliente);
            return clienteRepository.save(cliente);
        } else {
            throw new IllegalArgumentException("Cliente não encontrado para atualização.");
        }
    }

    // Compara campos e gera registro de auditoria com valor anterior e novo
    private void auditarCamposModificados(Cliente antigo, Cliente novo) {
        verificarEGravar(antigo.getId(), "nome", antigo.getNome(), novo.getNome());
        verificarEGravar(antigo.getId(), "telefone", antigo.getTelefone(), novo.getTelefone());
        verificarEGravar(antigo.getId(), "logradouro", antigo.getLogradouro(), novo.getLogradouro());
        verificarEGravar(antigo.getId(), "numero", antigo.getNumero(), novo.getNumero());
        verificarEGravar(antigo.getId(), "bairro", antigo.getBairro(), novo.getBairro());
        verificarEGravar(antigo.getId(), "cidade", antigo.getCidade(), novo.getCidade());
        verificarEGravar(antigo.getId(), "estado", antigo.getEstado(), novo.getEstado());
        verificarEGravar(antigo.getId(), "cep", antigo.getCep(), novo.getCep());
        verificarEGravar(antigo.getId(), "complemento", antigo.getComplemento(), novo.getComplemento());
    }

    private void verificarEGravar(String idCliente, String campo, String anterior, String novo) {
        String vAnt = anterior != null ? anterior.trim() : "";
        String vNov = novo != null ? novo.trim() : "";

        if (!vAnt.equals(vNov)) {
            String detalhe = "Campo '" + campo + "' alterado de '" + vAnt + "' para '" + vNov + "'";
            auditoriaService.registrarMudancaCampo("ALTERACAO_CLIENTE", "Cliente", idCliente, 
                                                   campo, vAnt, vNov, detalhe, null);
        }
    }

}
