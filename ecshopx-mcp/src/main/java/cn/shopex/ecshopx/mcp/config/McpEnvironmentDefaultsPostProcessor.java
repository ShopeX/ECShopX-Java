package cn.shopex.ecshopx.mcp.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

/**
 * Loads {@code application-mcp-defaults.yml} without requiring bootstrap {@code spring.config.import}.
 */
public class McpEnvironmentDefaultsPostProcessor implements EnvironmentPostProcessor {

	private static final String DEFAULTS_RESOURCE = "application-mcp-defaults.yml";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		ClassPathResource resource = new ClassPathResource(DEFAULTS_RESOURCE);
		if (!resource.exists()) {
			return;
		}
		try {
			YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
			List<PropertySource<?>> sources = loader.load("ecshopxMcpDefaults", resource);
			for (PropertySource<?> source : sources) {
				environment.getPropertySources().addLast(source);
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load " + DEFAULTS_RESOURCE, ex);
		}
	}
}