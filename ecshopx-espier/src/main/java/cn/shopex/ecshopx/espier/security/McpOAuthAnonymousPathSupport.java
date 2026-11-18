/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.espier.security;

/**
 * Servlet paths that must bypass espier operator/POS JWT gates for MCP OAuth discovery and token flows.
 * {@code /api/mcp} JSON-RPC remains protected by {@code McpBearerAuthFilter} in ecshopx-mcp.
 */
public final class McpOAuthAnonymousPathSupport {

	private static final String WELL_KNOWN_PROTECTED_RESOURCE = "/.well-known/oauth-protected-resource";
	private static final String WELL_KNOWN_AUTHORIZATION_SERVER = "/.well-known/oauth-authorization-server";

	private McpOAuthAnonymousPathSupport() {}

	public static boolean matches(String servletPath) {
		if (servletPath == null || servletPath.isEmpty()) {
			return false;
		}
		if ("/api/mcp".equals(servletPath) || servletPath.startsWith("/api/mcp/")) {
			return true;
		}
		if (WELL_KNOWN_AUTHORIZATION_SERVER.equals(servletPath)
				|| servletPath.startsWith(WELL_KNOWN_AUTHORIZATION_SERVER + "/")) {
			return true;
		}
		return servletPath.equals(WELL_KNOWN_PROTECTED_RESOURCE)
				|| servletPath.startsWith(WELL_KNOWN_PROTECTED_RESOURCE + "/");
	}
}