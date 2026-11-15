package br.com.vitor.apicrud_alunos.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Configuração do cache no Redis: como os objetos são serializados (JSON), por quanto tempo ficam e o que
 * acontece quando o Redis falha.
 *
 * Serializer: no Spring Boot 4 o Jackson é o 3 (pacote tools.jackson). O GenericJackson2JsonRedisSerializer
 * (Jackson 2) não entende LocalDate e falha ao gravar. O GenericJacksonJsonRedisSerializer sem tipagem devolve
 * LinkedHashMap ao ler um record/classe final (ClassCastException no @Cacheable); por isso a tipagem é ligada, com
 * lista de tipos permitidos: só as classes do projeto (model e dto), java.util e java.time podem ser recriadas
 * a partir do Redis. Cachear outro tipo exige permitir o pacote dele aqui (o teste RedisConfigTests avisa).
 *
 * Redis é OTIMIZAÇÃO: se ele cair ou estiver lento, a API consulta o banco e segue (CacheErrorHandler abaixo).
 * A propriedade spring.cache.type é lida AQUI (o Spring Boot 4 não traz a auto-configuração de cache sem o
 * spring-boot-starter-cache, que o projeto não usa): "redis" (padrão) liga o Redis; "none" usa um cache que não guarda nada.
 * Nos testes o cache fica desligado (none); o CacheRedisIntegrationTests liga com um Redis real.
 */
@Configuration
public class RedisConfig implements CachingConfigurer {

	private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

	static final Duration VALIDADE = Duration.ofMinutes(10);

	static GenericJacksonJsonRedisSerializer serializador() {
		PolymorphicTypeValidator tiposPermitidos = BasicPolymorphicTypeValidator.builder()
				.allowIfSubType("br.com.vitor.apicrud_alunos.model.")
				.allowIfSubType("br.com.vitor.apicrud_alunos.dto.")
				.allowIfSubType("java.util.")
				.allowIfSubType("java.time.")
				.build();
		return GenericJacksonJsonRedisSerializer.builder().enableDefaultTyping(tiposPermitidos).build();
	}

	@Bean
	@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
	public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(VALIDADE)
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializador()));

		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(config)
				// gravar e apagar no cache só DEPOIS do commit: sem isso, um GET concorrente poderia recolocar no cache
				// o valor antigo entre o @CacheEvict e o commit da transação
				.transactionAware()
				.build();
	}

	// cache desligado (spring.cache.type=none): os @Cacheable/@CacheEvict continuam valendo, mas não guardam nada
	@Bean
	@ConditionalOnProperty(name = "spring.cache.type", havingValue = "none")
	public CacheManager cacheManagerDesligado() {
		return new NoOpCacheManager();
	}

	// Falha do Redis (fora do ar, lento, valor ilegível): registra e segue sem cache, em vez de responder erro.
	// Limite conhecido: se o apagar (@CacheEvict) falhar, o valor antigo pode ficar até a validade (10 min).
	@Override
	public CacheErrorHandler errorHandler() {
		return new CacheErrorHandler() {
			@Override
			public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
				log.warn("Cache '{}' indisponível ao ler a chave {}: {}", cache.getName(), key, e.toString());
			}

			@Override
			public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
				log.warn("Cache '{}' indisponível ao gravar a chave {}: {}", cache.getName(), key, e.toString());
			}

			@Override
			public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
				log.warn("Cache '{}' indisponível ao apagar a chave {} (pode ficar valor antigo até {}): {}",
						cache.getName(), key, VALIDADE, e.toString());
			}

			@Override
			public void handleCacheClearError(RuntimeException e, Cache cache) {
				log.warn("Cache '{}' indisponível ao limpar: {}", cache.getName(), e.toString());
			}
		};
	}
}
