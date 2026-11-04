package br.com.vitor.apicrud_alunos.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// A senha nunca é gravada em texto: grava-se o hash.
// O DelegatingPasswordEncoder escreve o algoritmo no começo do hash ({bcrypt}$2a$10$...). Por isso, no futuro,
// dá para trocar o algoritmo padrão sem invalidar as senhas antigas: elas continuam sendo conferidas pelo
// algoritmo indicado no prefixo, e só as novas usam o novo.
// Só o BCrypt está cadastrado, de propósito: o encoder padrão do Spring (createDelegatingPasswordEncoder) também
// aceita {noop} (texto puro), md5, sha e outros inseguros. Aqui um hash com prefixo desconhecido, ou sem prefixo,
// é recusado com erro em vez de ser aceito.
@Configuration
public class PasswordConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		Map<String, PasswordEncoder> encoders = new HashMap<>();
		encoders.put("bcrypt", new BCryptPasswordEncoder());
		return new DelegatingPasswordEncoder("bcrypt", encoders);
	}

}
