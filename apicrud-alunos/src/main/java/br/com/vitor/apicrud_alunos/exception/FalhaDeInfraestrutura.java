package br.com.vitor.apicrud_alunos.exception;

import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;
import java.util.List;

import org.hibernate.exception.JDBCConnectionException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.CannotCreateTransactionException;

// Reconhece, olhando a cadeia de causas, que a falha foi do BANCO ESTAR FORA DO AR (sem conexão, sem transação, tempo
// esgotado) e não de um erro da aplicação nem de uma regra de dados (essas continuam 500/409/400 nos outros tratadores).
public final class FalhaDeInfraestrutura {

	private static final List<Class<? extends Throwable>> BANCO_FORA_DO_AR = List.of(
			CannotCreateTransactionException.class, CannotGetJdbcConnectionException.class,
			DataAccessResourceFailureException.class, QueryTimeoutException.class, JDBCConnectionException.class,
			SQLTransientConnectionException.class, SQLNonTransientConnectionException.class);

	private FalhaDeInfraestrutura() {
	}

	public static boolean bancoIndisponivel(Throwable erro) {
		Throwable atual = erro;
		for (int profundidade = 0; atual != null && profundidade < 12; profundidade++) {
			for (Class<? extends Throwable> tipo : BANCO_FORA_DO_AR) {
				if (tipo.isInstance(atual)) {
					return true;
				}
			}
			atual = atual.getCause() == atual ? null : atual.getCause();
		}
		return false;
	}

}
