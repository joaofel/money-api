package com.joaofeliciano.moneyapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.joaofeliciano.moneyapi.model.Pessoa;

public interface PessoaRepository extends JpaRepository<Pessoa, Long>{

}
