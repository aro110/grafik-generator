package pl.grafik.grafik_generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import pl.grafik.grafik_generator.infrastructure.repository.EmployeeRepository;
import pl.grafik.grafik_generator.infrastructure.repository.GenerationRunGroupRepository;
import pl.grafik.grafik_generator.infrastructure.repository.GenerationRunRepository;
import pl.grafik.grafik_generator.infrastructure.repository.ScheduleAiEditRepository;
import pl.grafik.grafik_generator.infrastructure.repository.ScheduleConfigRepository;
import pl.grafik.grafik_generator.infrastructure.repository.ScheduleRepository;
import pl.grafik.grafik_generator.infrastructure.repository.SectionRepository;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=" +
				"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
				"org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
				"org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration," +
				"org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
@Import(GrafikGeneratorApplicationTests.TestRepositories.class)
class GrafikGeneratorApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration
	static class TestRepositories {

		@Bean
		EmployeeRepository employeeRepository() {
			return repository(EmployeeRepository.class);
		}

		@Bean
		GenerationRunRepository generationRunRepository() {
			return repository(GenerationRunRepository.class);
		}

		@Bean
		GenerationRunGroupRepository generationRunGroupRepository() {
			return repository(GenerationRunGroupRepository.class);
		}

		@Bean
		ScheduleConfigRepository scheduleConfigRepository() {
			return repository(ScheduleConfigRepository.class);
		}

		@Bean
		ScheduleRepository scheduleRepository() {
			return repository(ScheduleRepository.class);
		}

		@Bean
		ScheduleAiEditRepository scheduleAiEditRepository() {
			return repository(ScheduleAiEditRepository.class);
		}

		@Bean
		SectionRepository sectionRepository() {
			return repository(SectionRepository.class);
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		private static <T> T repository(Class<T> repositoryType) {
			Object proxy = Proxy.newProxyInstance(
					repositoryType.getClassLoader(),
					new Class<?>[]{repositoryType},
					(instance, method, args) -> {
						if (method.getDeclaringClass() == Object.class) {
							return switch (method.getName()) {
								case "toString" -> repositoryType.getSimpleName() + "TestProxy";
								case "hashCode" -> System.identityHashCode(instance);
								case "equals" -> instance == args[0];
								default -> null;
							};
						}
						Class<?> returnType = method.getReturnType();
						if (returnType == Optional.class) {
							return Optional.empty();
						}
						if (returnType == List.class || returnType == Iterable.class) {
							return List.of();
						}
						if (returnType == boolean.class) {
							return false;
						}
						if (returnType.isPrimitive()) {
							return 0;
						}
						return null;
					});
			return repositoryType.cast(proxy);
		}
	}

}
