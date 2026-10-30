package br.com.vitor.apicrud_alunos.dto;

import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonPropertyOrder({ "content", "page", "size", "totalElements", "totalPages" })
public record PageResponse<T>(
		// no XML, os itens saem como <content><item>...</item></content>; JSON e YAML não mudam
		@JacksonXmlElementWrapper(localName = "content") @JacksonXmlProperty(localName = "item") List<T> content,
		int page, int size, long totalElements, int totalPages) {

	public <R> PageResponse<R> map(Function<? super T, ? extends R> mapper) {
		return new PageResponse<>(content.stream().<R>map(mapper).toList(), page, size, totalElements, totalPages);
	}

}
