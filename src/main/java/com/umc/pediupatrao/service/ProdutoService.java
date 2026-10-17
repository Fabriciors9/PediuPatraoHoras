package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired(required = false)
    private AuditoriaService auditoriaService;

    public Produto novoProduto(Produto produto) {
        return salvar(produto);
    }

    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    public void excluir(String id) {
        if (auditoriaService != null) {
            produtoRepository.findById(id).ifPresent(p -> {
                auditoriaService.registrar("EXCLUSAO_PRODUTO", "Produto", id, "Produto removido: " + p.getNome());
            });
        }
        produtoRepository.deleteById(id);
    }

    public Optional<Produto> buscarPorId(String id) {
        return produtoRepository.findById(id);
    }

    public Produto salvar(Produto produto) {
        if (produto.getId() == null || produto.getId().trim().isEmpty()) {
            produto.setId(null);
            Produto salvo = produtoRepository.save(produto);
            if (auditoriaService != null) {
                auditoriaService.registrar("CADASTRO_PRODUTO", "Produto", salvo.getId(),
                        "Cadastro do produto: " + salvo.getNome() + " (" + salvo.getTipo() + ") - R$ " + salvo.getPreco());
            }
            return salvo;
        } else {
            if (produtoRepository.existsById(produto.getId())) {
                Produto salvo = produtoRepository.save(produto);
                if (auditoriaService != null) {
                    auditoriaService.registrar("ALTERACAO_PRODUTO", "Produto", salvo.getId(),
                            "Produto atualizado: " + salvo.getNome() + " (" + salvo.getTipo() + ") - R$ " + salvo.getPreco());
                }
                return salvo;
            } else {
                throw new IllegalArgumentException("Produto não encontrado para atualização.");
            }
        }
    }
}
