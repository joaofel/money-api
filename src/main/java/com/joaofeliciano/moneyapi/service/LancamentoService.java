package com.joaofeliciano.moneyapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.joaofeliciano.moneyapi.model.Lancamento;
import com.joaofeliciano.moneyapi.model.Pessoa;
import com.joaofeliciano.moneyapi.repository.LancamentoRepository;
import com.joaofeliciano.moneyapi.repository.PessoaRepository;
import com.joaofeliciano.moneyapi.service.exception.PessoaInexistenteOuInativaException;

@Service
public class LancamentoService {
	
	@Autowired
	private LancamentoRepository lancamentoRepository;
	
	@Autowired
	private PessoaRepository pessoaRepository;

	public Lancamento salvar(Lancamento lancamento) {
		Pessoa pessoa = pessoaRepository.findOne(lancamento.getPessoa().getCodigo());
		
		if(pessoa == null || pessoa.isInativo()) {
			throw new PessoaInexistenteOuInativaException();
		}
		
		return lancamentoRepository.save(lancamento);
	}
	
}
