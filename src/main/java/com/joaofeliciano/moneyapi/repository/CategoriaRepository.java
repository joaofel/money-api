package com.joaofeliciano.moneyapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.joaofeliciano.moneyapi.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long>{

}
