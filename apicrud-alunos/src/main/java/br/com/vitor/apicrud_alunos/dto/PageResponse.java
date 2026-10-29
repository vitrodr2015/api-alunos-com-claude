package br.com.vitor.apicrud_alunos.dto;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public <R> PageResponse<R> map(Function<? super T, ? extends R> mapper) {
		return new PageResponse<>(content.stream().<R>map(mapper).toList(), page, size, totalElements, totalPages);
	}

}
