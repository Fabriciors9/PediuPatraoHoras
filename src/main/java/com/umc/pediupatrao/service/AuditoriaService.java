package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    // Registra uma operação genérica (criação, mudança de status, etc.)
    public void registrar(String tipoOperacao, String entidade, String idEntidade, String detalhe) {
        String usuario = getUsuarioAtual();
        String perfil = getPerfilAtual();

        Auditoria log = new Auditoria(usuario, perfil, tipoOperacao, entidade, idEntidade, 
                                      null, null, null, detalhe, null);
        auditoriaRepository.save(log);
    }

    // Registra a alteração detalhada de um campo específico com valor anterior e novo
    public void registrarMudancaCampo(String tipoOperacao, String entidade, String idEntidade, 
                                     String campo, String valorAnterior, String novoValor, 
                                     String detalhe, String justificativa) {
        String usuario = getUsuarioAtual();
        String perfil = getPerfilAtual();

        Auditoria log = new Auditoria(usuario, perfil, tipoOperacao, entidade, idEntidade,
                                      campo, valorAnterior, novoValor, detalhe, justificativa);
        auditoriaRepository.save(log);
    }

    // Registra operação com justificativa (ex: cancelamento de pedido)
    public void registrarComJustificativa(String tipoOperacao, String entidade, String idEntidade, 
                                         String detalhe, String justificativa) {
        String usuario = getUsuarioAtual();
        String perfil = getPerfilAtual();

        Auditoria log = new Auditoria(usuario, perfil, tipoOperacao, entidade, idEntidade,
                                      null, null, null, detalhe, justificativa);
        auditoriaRepository.save(log);
    }

    // Consulta os registros - exclusivo para ADMIN e GERENTE conforme SecurityConfig
    public List<Auditoria> listarTodas() {
        return auditoriaRepository.findAllByOrderByDataHoraDesc();
    }

    // Métodos auxiliares para pegar o usuário logado
    public String getUsuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return auth.getName();
        }
        return "SISTEMA";
    }

    public String getPerfilAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("SEM_PERFIL")
                    .replace("ROLE_", "");
        }
        return "SEM_PERFIL";
    }
}
