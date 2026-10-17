package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Auditoria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditoriaRepository extends MongoRepository<Auditoria, String> {

    // Retorna todos os logs ordenados do mais recente para o mais antigo
    List<Auditoria> findAllByOrderByDataHoraDesc();

    // Filtro por entidade se precisar
    List<Auditoria> findByEntidadeAfetadaOrderByDataHoraDesc(String entidadeAfetada);
}
