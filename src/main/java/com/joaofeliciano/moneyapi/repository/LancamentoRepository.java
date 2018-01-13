package com.joaofeliciano.moneyapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.joaofeliciano.moneyapi.model.Lancamento;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long>{
	
}
