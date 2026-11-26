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

package cn.shopex.ecshopx.goods.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.goods.service.hotword.SearchHotwordQueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 热门搜索前台接口（ECX-11390 F-005；Teambition ECX-11415）。
 *
 * <p>vshop 传 {@code channel=mobile}（缺省），web 传 {@code channel=pc}；接口返回全部当前有效热词且不截断，
 * 最多展示 8 个/两行由前端按容器取舍。
 */
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("goodsFrontV1SearchHotword")
@RequestMapping("/api/v1/h5app/wxapp/goods")
public class SearchHotwordController {

	private final SearchHotwordQueryService queryService;

	public SearchHotwordController(SearchHotwordQueryService queryService) {
		this.queryService = queryService;
	}

	@GetMapping(value = "/hotwords", name = "热门搜索")
	public ResponseEntity<ApiResult<Map<String, Object>>> list(
			HttpServletRequest request,
			@RequestParam(value = "channel", required = false) String channel) {
		long companyId = parseCompanyIdFromRequest(request);
		return ResponseEntity.ok(
				ApiResult.ok(queryService.listEffective(companyId, channel, Instant.now().getEpochSecond())));
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number number) {
			companyId = number.longValue();
		} else if (companyAttr instanceof String text && StringUtils.hasText(text)) {
			try {
				companyId = Long.parseLong(text.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return companyId;
	}
}
