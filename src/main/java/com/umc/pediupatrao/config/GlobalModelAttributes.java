package com.umc.pediupatrao.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    // Recebe a autenticação direta da requisição HTTP do Spring Security
    @ModelAttribute
    public void adicionarUsuarioAoModelo(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            String username = authentication.getName();
            String perfil = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ATENDENTE")
                    .replace("ROLE_", "");

            model.addAttribute("usuarioLogado", username);
            model.addAttribute("perfilLogado", perfil);
            model.addAttribute("perfilAtual", perfil);
            model.addAttribute("isGerente", "GERENTE".equalsIgnoreCase(perfil));
            model.addAttribute("isAdmin", "ADMIN".equalsIgnoreCase(perfil));
            model.addAttribute("isAtendente", "ATENDENTE".equalsIgnoreCase(perfil));
        } else {
            model.addAttribute("usuarioLogado", null);
            model.addAttribute("perfilLogado", null);
            model.addAttribute("perfilAtual", null);
            model.addAttribute("isGerente", false);
            model.addAttribute("isAdmin", false);
            model.addAttribute("isAtendente", false);
        }
    }
}
