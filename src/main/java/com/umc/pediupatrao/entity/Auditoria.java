package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "auditorias")
public class Auditoria {

    @Id
    private String id;

    // Usuário autenticado que realizou a operação
    private String usuario;

    // Perfil do usuário (ADMIN, GERENTE, ATENDENTE)
    private String perfil;

    // Data e hora do evento
    private LocalDateTime dataHora;

    // Tipo da operação (ex: CRIACAO_PEDIDO, MUDANCA_STATUS, DESCONTO, CANCELAMENTO, ALTERACAO_CLIENTE)
    private String tipoOperacao;

    // Entidade afetada (Pedido, Cliente)
    private String entidadeAfetada;

    // Identificador do registro afetado
    private String idEntidade;

    // Campo alterado (ex: "logradouro", "status", "percentualDesconto")
    private String campoAlterado;

    // Valor antes da alteração
    private String valorAnterior;

    // Novo valor atribuído
    private String novoValor;

    // Resumo ou descrição detalhada da operação
    private String detalhe;

    // Justificativa quando exigido por regra de negócio (ex: cancelamento)
    private String justificativa;

    public Auditoria() {
        this.dataHora = LocalDateTime.now();
    }

    public Auditoria(String usuario, String perfil, String tipoOperacao, String entidadeAfetada, 
                     String idEntidade, String campoAlterado, String valorAnterior, 
                     String novoValor, String detalhe, String justificativa) {
        this.usuario = usuario;
        this.perfil = perfil;
        this.dataHora = LocalDateTime.now();
        this.tipoOperacao = tipoOperacao;
        this.entidadeAfetada = entidadeAfetada;
        this.idEntidade = idEntidade;
        this.campoAlterado = campoAlterado;
        this.valorAnterior = valorAnterior;
        this.novoValor = novoValor;
        this.detalhe = detalhe;
        this.justificativa = justificativa;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(String tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public String getEntidadeAfetada() {
        return entidadeAfetada;
    }

    public void setEntidadeAfetada(String entidadeAfetada) {
        this.entidadeAfetada = entidadeAfetada;
    }

    public String getIdEntidade() {
        return idEntidade;
    }

    public void setIdEntidade(String idEntidade) {
        this.idEntidade = idEntidade;
    }

    public String getCampoAlterado() {
        return campoAlterado;
    }

    public void setCampoAlterado(String campoAlterado) {
        this.campoAlterado = campoAlterado;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(String valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public String getNovoValor() {
        return novoValor;
    }

    public void setNovoValor(String novoValor) {
        this.novoValor = novoValor;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public void setDetalhe(String detalhe) {
        this.detalhe = detalhe;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }
}
