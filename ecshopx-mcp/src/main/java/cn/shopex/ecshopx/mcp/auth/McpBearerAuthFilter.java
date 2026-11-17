package cn.shopex.ecshopx.mcp.auth;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import cn.shopex.ecshopx.mcp.config.McpPublicUrls;
import cn.shopex.ecshopx.mcp.tool.ToolAuthContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class McpBearerAuthFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final McpProperties mcpProperties;

	public McpBearerAuthFilter(McpProperties mcpProperties) {
		this.mcpProperties = mcpProperties;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = normalizePath(request);
		String mcpPath = mcpProperties.getMcpPath();
		if (!mcpPath.startsWith("/")) {
			mcpPath = "/" + mcpPath;
		}
		return !mcpPath.equals(path);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (!hasBearerCredential(authorization)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setHeader(
					HttpHeaders.WWW_AUTHENTICATE,
					"Bearer realm=\"mcp\", resource_metadata=\"" + McpPublicUrls.resourceMetadataUrl(mcpProperties)
							+ "\"");
			return;
		}
		try {
			ToolAuthContext.setBearer(authorization.trim());
			filterChain.doFilter(request, response);
		}
		finally {
			ToolAuthContext.clear();
		}
	}

	private static boolean hasBearerCredential(String authorization) {
		if (!StringUtils.hasText(authorization) || authorization.length() < BEARER_PREFIX.length()) {
			return false;
		}
		if (!authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			return false;
		}
		return StringUtils.hasText(authorization.substring(BEARER_PREFIX.length()).trim());
	}

	private static String normalizePath(HttpServletRequest request) {
		String path = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
			path = path.substring(contextPath.length());
		}
		if (path.isEmpty()) {
			return "/";
		}
		return path;
	}
}