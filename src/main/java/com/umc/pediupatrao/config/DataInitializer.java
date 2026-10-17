package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, ProdutoRepository produtoRepo) {
        return args -> {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            // Usuário ADMIN (garante acesso básico ao sistema)
            if (repo.findByUsername("admin").isEmpty()) {
                Usuario admin = new Usuario();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("admin123"));
                admin.setRole("ADMIN");
                repo.save(admin);
                System.out.println("Usuário admin criado (senha: admin123)!");
            }

            // Inicializar produtos/pizzas do cardápio se não existirem
            if (produtoRepo.count() == 0) {
                cadastrarProduto(produtoRepo, "Pizza Calabresa Especial", "Pizza", 45.00);
                cadastrarProduto(produtoRepo, "Pizza Mussarela Tradicional", "Pizza", 42.00);
                cadastrarProduto(produtoRepo, "Pizza Quatro Queijos", "Pizza", 52.00);
                cadastrarProduto(produtoRepo, "Pizza Frango com Catupiry", "Pizza", 48.00);
                cadastrarProduto(produtoRepo, "Pizza Portuguesa Completa", "Pizza", 50.00);
                System.out.println("Pizzas padrão do cardápio cadastradas com sucesso!");
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