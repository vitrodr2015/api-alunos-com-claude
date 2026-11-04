package br.com.vitor.apicrud_alunos.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.vitor.apicrud_alunos.model.Permission;
import br.com.vitor.apicrud_alunos.model.User;
import br.com.vitor.apicrud_alunos.repository.PermissionRepository;
import br.com.vitor.apicrud_alunos.repository.UserRepository;

// Cria o PRIMEIRO administrador por um meio controlado, sem nenhuma senha no repositório.
//
// Como usar (numa execução única): defina a variável de ambiente ADMIN_BOOTSTRAP_PASSWORD (e, se quiser outro nome
// que não "admin", ADMIN_BOOTSTRAP_USERNAME), suba a aplicação uma vez e DEPOIS REMOVA A VARIÁVEL. Na AWS, a senha
// vem do Secrets Manager (como a do banco), nunca escrita no task-definition.json.
//
// Regras:
// - desligado por padrão: sem a variável, não faz nada;
// - só cria se ainda NÃO existir nenhum usuário com a permissão ADMIN (então reiniciar com a variável ainda
//   definida não recria nem troca nada);
// - a senha precisa ter de 12 caracteres a 72 bytes e não pode ter espaço no começo ou no fim;
// - se já existir um usuário COMUM com o nome pedido, a subida falha em vez de promovê-lo sem aviso;
// - a senha nunca é escrita no log.
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

	static final int TAMANHO_MINIMO_DA_SENHA = 12;
	static final int LIMITE_DA_SENHA_EM_BYTES = 72;
	private static final Pattern NOME_VALIDO = Pattern.compile("^[A-Za-z0-9._@-]{3,100}$");

	private final UserRepository userRepository;
	private final PermissionRepository permissionRepository;
	private final PasswordEncoder passwordEncoder;
	private final String username;
	private final String password;

	public AdminBootstrapRunner(UserRepository userRepository, PermissionRepository permissionRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.admin-bootstrap.username:admin}") String username,
			@Value("${app.admin-bootstrap.password:}") String password) {
		this.userRepository = userRepository;
		this.permissionRepository = permissionRepository;
		this.passwordEncoder = passwordEncoder;
		this.username = username;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (password == null || password.isEmpty()) {
			return; // desligado (o padrão)
		}
		validarConfiguracao();

		if (userRepository.existsByPermissionsDescription(Authorities.ADMIN)) {
			log.warn("ADMIN_BOOTSTRAP_PASSWORD está definida, mas já existe um administrador: nada foi criado. "
					+ "Remova a variável do ambiente.");
			return;
		}
		if (userRepository.existsByUserNameIgnoreCase(username)) {
			throw new IllegalStateException("Já existe um usuário '" + username + "' que não é administrador. "
					+ "Escolha outro nome em ADMIN_BOOTSTRAP_USERNAME ou dê a permissão ADMIN a ele manualmente.");
		}

		User admin = new User();
		admin.setUserName(username);
		admin.setFullName("Administrador");
		admin.setPassword(passwordEncoder.encode(password));
		admin.setPermissions(new ArrayList<>(List.of(permissao(Authorities.ADMIN), permissao(Authorities.USER))));
		userRepository.save(admin);

		// a senha NÃO aparece aqui, de propósito
		log.warn("Administrador inicial '{}' criado a partir de ADMIN_BOOTSTRAP_PASSWORD. REMOVA essa variável do "
				+ "ambiente agora: a senha não deve ficar configurada depois da primeira execução.", username);
	}

	private void validarConfiguracao() {
		if (username == null || !NOME_VALIDO.matcher(username).matches()) {
			throw new IllegalStateException("ADMIN_BOOTSTRAP_USERNAME inválido: use de 3 a 100 caracteres, só "
					+ "letras, números e . _ @ -");
		}
		if (!password.equals(password.strip())) {
			throw new IllegalStateException("ADMIN_BOOTSTRAP_PASSWORD tem espaço ou quebra de linha no começo ou no fim "
					+ "(um erro comum ao gravar a variável); remova-o.");
		}
		if (password.length() < TAMANHO_MINIMO_DA_SENHA) {
			throw new IllegalStateException("ADMIN_BOOTSTRAP_PASSWORD é curta demais: use pelo menos "
					+ TAMANHO_MINIMO_DA_SENHA + " caracteres.");
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > LIMITE_DA_SENHA_EM_BYTES) {
			throw new IllegalStateException("ADMIN_BOOTSTRAP_PASSWORD passa de " + LIMITE_DA_SENHA_EM_BYTES
					+ " bytes (o limite do BCrypt; letras acentuadas ocupam mais de um byte).");
		}
	}

	private Permission permissao(String descricao) {
		return permissionRepository.findByDescription(descricao)
				.orElseThrow(() -> new IllegalStateException("A permissão " + descricao + " não existe no banco"));
	}

}
