package br.com.vitor.apicrud_alunos.exception;

public class InvalidRequestException extends RuntimeException {

	public InvalidRequestException(String message) {
		super(message);
	}

}
