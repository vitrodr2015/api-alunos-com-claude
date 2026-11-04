package br.com.vitor.apicrud_alunos.model;

import org.springframework.security.core.GrantedAuthority;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Nomes e tamanhos das colunas precisam bater com db/migration/V4__create_tables_users_and_permissions.sql
@Entity
@Table(name = "permission")
public class Permission implements GrantedAuthority {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "description", nullable = false, unique = true, length = 100)
	private String description;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	// o Spring Security enxerga a permissão pelo texto da descrição (ex.: ADMIN)
	@Override
	public String getAuthority() {
		return description;
	}

}
