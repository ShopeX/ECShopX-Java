package cn.shopex.ecshopx.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "ecx.mcp")
public class McpProperties {

	private String apiBaseUrl = "http://127.0.0.1:8080";

	private String mcpPath = "/api/mcp";

	private String publicBaseUrl = "";

	private Duration connectTimeout = Duration.ofSeconds(5);

	private Duration readTimeout = Duration.ofSeconds(30);
}