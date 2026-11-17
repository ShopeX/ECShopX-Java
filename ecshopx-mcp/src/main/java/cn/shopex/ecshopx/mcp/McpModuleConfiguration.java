package cn.shopex.ecshopx.mcp;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = McpModuleConfiguration.class)
@ConfigurationPropertiesScan(basePackageClasses = McpModuleConfiguration.class)
public class McpModuleConfiguration {}