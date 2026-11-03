package br.com.vitor.apicrud_alunos.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import br.com.vitor.apicrud_alunos.model.Permission;
import br.com.vitor.apicrud_alunos.model.User;

// Nunca devolve a senha (nem o hash)
@JsonPropertyOrder({ "id", "username", "fullName", "enabled", "permissions" })
public record UserResponse(Long id, String username, String fullName, boolean enabled, List<String> permissions) {

	public static UserResponse from(User usuario) {
		return new UserResponse(usuario.getId(), usuario.getUsername(), usuario.getFullName(), usuario.isEnabled(),
				usuario.getPermissions().stream().map(Permission::getDescription).toList());
	}

}
