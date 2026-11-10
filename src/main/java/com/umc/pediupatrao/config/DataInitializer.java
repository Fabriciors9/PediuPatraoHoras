package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Value("${app.admin.username:admin}")
    private String adminUser;

    @Value("${app.admin.password:admin123}")
    private String adminPass;

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, ProdutoRepository produtoRepo) {
        return args -> {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            if (repo.findByUsername(adminUser).isEmpty()) {
                Usuario admin = new Usuario();
                admin.setUsername(adminUser);
                admin.setPassword(encoder.encode(adminPass));
                admin.setRole("ADMIN");
                repo.save(admin);
                System.out.println("Usuario administrador criado: " + adminUser);
            }

            if (repo.findByUsername("gerente").isEmpty()) {
                Usuario gerente = new Usuario();
                gerente.setUsername("gerente");
                gerente.setPassword(encoder.encode("gerente123"));
                gerente.setRole("GERENTE");
                repo.save(gerente);
            }

            if (repo.findByUsername("atendente").isEmpty()) {
                Usuario atendente = new Usuario();
                atendente.setUsername("atendente");
                atendente.setPassword(encoder.encode("atendente123"));
                atendente.setRole("ATENDENTE");
                repo.save(atendente);
            }

            if (produtoRepo.count() == 0) {
                cadastrarProduto(produtoRepo, "Pizza Calabresa Especial", "Pizza", 45.00);
                cadastrarProduto(produtoRepo, "Pizza Mussarela Tradicional", "Pizza", 42.00);
                cadastrarProduto(produtoRepo, "Pizza Quatro Queijos", "Pizza", 52.00);
                cadastrarProduto(produtoRepo, "Pizza Frango com Catupiry", "Pizza", 48.00);
                cadastrarProduto(produtoRepo, "Pizza Portuguesa Completa", "Pizza", 50.00);
            }
        };
    }

    private void cadastrarProduto(ProdutoRepository repo, String nome, String tipo, Double preco) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setTipo(tipo);
        p.setPreco(preco);
        p.setAtivo(true);
        repo.save(p);
    }
}
