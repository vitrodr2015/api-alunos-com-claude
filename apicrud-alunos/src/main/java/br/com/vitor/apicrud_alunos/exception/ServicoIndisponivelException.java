package br.com.vitor.apicrud_alunos.exception;

// O banco (ou outra dependência) está fora do ar: o cliente deve tentar de novo daqui a pouco. Vira 503, não 401 nem 500.
public class ServicoIndisponivelException extends RuntimeException {

	public ServicoIndisponivelException(Throwable causa) {
		super("Serviço temporariamente indisponível. Tente novamente em instantes.", causa);
	}

}
