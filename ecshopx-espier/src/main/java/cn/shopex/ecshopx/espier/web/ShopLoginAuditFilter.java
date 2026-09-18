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

package cn.shopex.ecshopx.espier.web;

import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class ShopLoginAuditFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(ShopLoginAuditFilter.class);

	private final ObjectMapper objectMapper;
	private final OperatorLogsWriteService operatorLogsWriteService;

	public ShopLoginAuditFilter(ObjectMapper objectMapper, OperatorLogsWriteService operatorLogsWriteService) {
		this.objectMapper = objectMapper;
		this.operatorLogsWriteService = operatorLogsWriteService;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		return !(path != null
				&& path.equals("/api/v1/operator/login")
				&& "POST".equalsIgnoreCase(request.getMethod()));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
		try {
			filterChain.doFilter(request, wrapped);
		} finally {
			try {
				byte[] buf = wrapped.getContentAsByteArray();
				if (buf.length > 0 && wrapped.getStatus() == 200) {
					JsonNode root = objectMapper.readTree(buf);
					if (root.has("data") && root.get("data").has("token")) {
						Map<String, Object> ctx = new HashMap<>();
						ctx.put("request_uri", request.getRequestURI());
						ctx.put("ip", request.getRemoteAddr());
						ctx.put("params", objectMapper.writeValueAsString(root));
						ctx.put("operator_name", "login");
						operatorLogsWriteService.addLogs(ctx);
					}
				}
			} catch (Exception e) {
				log.debug("login audit skip: {}", e.getMessage());
			}
			wrapped.copyBodyToResponse();
		}
	}
}
