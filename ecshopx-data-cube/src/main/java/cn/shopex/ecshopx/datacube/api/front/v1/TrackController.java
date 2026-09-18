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

package cn.shopex.ecshopx.datacube.api.front.v1;

import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.datacube.service.TrackViewNumService;
import cn.shopex.ecshopx.datacube.web.TrackFlexibleInputMerge;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@FrontNoAuth
@RestController("datacubeFrontV1Track")
@RequestMapping("/api/v1/h5app")
public class TrackController {

	private final TrackViewNumService trackViewNumService;

	public TrackController(TrackViewNumService trackViewNumService) {
		this.trackViewNumService = trackViewNumService;
	}

	@PostMapping(value = "/wxapp/track/viewnum", name = "统计浏览人数")
	public ResponseEntity<ApiResult<Map<String, Boolean>>> addViewNum(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = TrackFlexibleInputMerge.merge(request, body);
		long companyId = resolveCompanyId(request);

		String monitorRaw = scalarString(merged.get("monitor_id"));
		String sourceRaw = scalarString(merged.get("source_id"));
		String monitorId = monitorRaw == null ? "" : StringUtils.trimWhitespace(monitorRaw);
		String sourceId = sourceRaw == null ? "" : StringUtils.trimWhitespace(sourceRaw);

		Object openObj = merged.get("open_id");
		String openIdForService;
		if (openObj == null) {
			openIdForService = "";
		} else {
			String openScalar = scalarString(openObj);
			openIdForService = openScalar == null ? "" : StringUtils.trimWhitespace(openScalar);
		}

		trackViewNumService.addViewNum(companyId, monitorId, sourceId, openIdForService);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private static long resolveCompanyId(HttpServletRequest request) {
		Object cidAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (cidAttr instanceof Number n) {
			return n.longValue();
		}
		if (cidAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException ignored) {
				return 0L;
			}
		}
		return 0L;
	}

	private static String scalarString(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof List<?> list) {
			if (list.isEmpty()) {
				return null;
			}
			return String.valueOf(list.get(0));
		}
		return String.valueOf(v);
	}
}
