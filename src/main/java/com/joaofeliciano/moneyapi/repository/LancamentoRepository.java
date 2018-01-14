package com.joaofeliciano.moneyapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.joaofeliciano.moneyapi.model.Lancamento;
import com.joaofeliciano.moneyapi.repository.lancamento.LancamentoRepositoryQuery;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long>, LancamentoRepositoryQuery{
	
}
