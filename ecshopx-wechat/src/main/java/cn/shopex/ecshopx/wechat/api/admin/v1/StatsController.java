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

package cn.shopex.ecshopx.wechat.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wechat.service.WechatStatsUserWeekSummaryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("wechatAdminV1Stats")
@RequestMapping("/api/v1/wechat/stats")
public class StatsController {

	private final WechatStatsUserWeekSummaryService wechatStatsUserWeekSummaryService;

	public StatsController(WechatStatsUserWeekSummaryService wechatStatsUserWeekSummaryService) {
		this.wechatStatsUserWeekSummaryService = wechatStatsUserWeekSummaryService;
	}

	@Activated(routeAlias = "wechat.stats.userWeekSummary")
	@GetMapping(value = "/userweeksummary", name = "最近七天用户数据")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> userWeekSummary() {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object raw = map.get("authorizer_appid");
		String authorizerAppid = raw == null ? "" : String.valueOf(raw).trim();
		if (!StringUtils.hasText(authorizerAppid)) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		}
		List<Map<String, Object>> list = wechatStatsUserWeekSummaryService.userWeekSummary(authorizerAppid);
		return ResponseEntity.ok(ApiResult.ok(list));
	}
}
