package com.umc.pediupatrao.entity;

import jakarta.persistence.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pedidos")
@Entity
public class Pedido {
    @Id
    private String id;
    private String clienteId;
    private String tipoPizza;
    private String status;
    private int quantidade;
    private String sabor;

    // RF02: Itens, preços, descontos e total calculado no servidor
    private Double precoUnitario = 0.0;
    private Double subtotal = 0.0;
    private Double percentualDesconto = 0.0;
    private Double valorDesconto = 0.0;
    private Double valorTotal = 0.0;

    // RF02 e RF03: Responsável pelo registro e data/hora de entrada
    private String responsavelRegistro;
    private java.time.LocalDateTime dataHoraEntrada;

    // RF03: Responsável pela saída e data/hora de saída (entrega/retirada)
    private String responsavelSaida;
    private java.time.LocalDateTime dataHoraSaida;

    // RF03 e RF04: Justificativa obrigatória caso seja cancelado pelo gerente
    private String justificativaCancelamento;

    // Calcula os valores no servidor (RF02) para impedir manipulação indevida via HTTP
    public void calcularValores() {
        if (this.precoUnitario == null || this.precoUnitario < 0) {
            this.precoUnitario = 0.0;
        }
        if (this.quantidade <= 0) {
            this.quantidade = 1;
        }
        this.subtotal = this.precoUnitario * this.quantidade;

        if (this.percentualDesconto == null || this.percentualDesconto < 0) {
            this.percentualDesconto = 0.0;
        }
        if (this.percentualDesconto > 20.0) {
            this.percentualDesconto = 20.0; // teto máximo de 20% conforme RF04
        }

        this.valorDesconto = this.subtotal * (this.percentualDesconto / 100.0);
        this.valorTotal = this.subtotal - this.valorDesconto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getTipoPizza() {
        return tipoPizza;
    }

    public void setTipoPizza(String tipoPizza) {
        this.tipoPizza = tipoPizza;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public String getSabor() {
        return sabor;
    }

    public void setSabor(String sabor) {
        this.sabor = sabor;
    }

    public Double getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(Double precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public Double getPercentualDesconto() {
        return percentualDesconto;
    }

    public void setPercentualDesconto(Double percentualDesconto) {
        this.percentualDesconto = percentualDesconto;
    }

    public Double getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(Double valorDesconto) {
        this.valorDesconto = valorDesconto;
    }

    public Double getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(Double valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getResponsavelRegistro() {
        return responsavelRegistro;
    }

    public void setResponsavelRegistro(String responsavelRegistro) {
        this.responsavelRegistro = responsavelRegistro;
    }

    public java.time.LocalDateTime getDataHoraEntrada() {
        return dataHoraEntrada;
    }

    public void setDataHoraEntrada(java.time.LocalDateTime dataHoraEntrada) {
        this.dataHoraEntrada = dataHoraEntrada;
    }

    public String getResponsavelSaida() {
        return responsavelSaida;
    }

    public void setResponsavelSaida(String responsavelSaida) {
        this.responsavelSaida = responsavelSaida;
    }

    public java.time.LocalDateTime getDataHoraSaida() {
        return dataHoraSaida;
    }

    public void setDataHoraSaida(java.time.LocalDateTime dataHoraSaida) {
        this.dataHoraSaida = dataHoraSaida;
    }

    public String getJustificativaCancelamento() {
        return justificativaCancelamento;
    }

    public void setJustificativaCancelamento(String justificativaCancelamento) {
        this.justificativaCancelamento = justificativaCancelamento;
    }
    
    
}
