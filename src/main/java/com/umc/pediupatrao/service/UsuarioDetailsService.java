package com.umc.pediupatrao.service;

import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByUsername(username)
                .map(usuario -> {
                    // Pega a role cadastrada no banco, se tiver nulo define ATENDENTE como padrão
                    String role = usuario.getRole();
                    if (role == null || role.trim().isEmpty()) {
                        role = "ATENDENTE";
                    }
                    // Remove prefixo ROLE_ se houver para não duplicar no .roles()
                    if (role.startsWith("ROLE_")) {
                        role = role.substring(5);
                    }
                    return User.builder()
                            .username(usuario.getUsername())
                            .password(usuario.getPassword())
                            .roles(role.toUpperCase())
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
    }
}
