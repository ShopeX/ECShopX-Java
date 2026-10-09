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

package cn.shopex.ecshopx.promotions.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskSkeletonService;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端会员任务（SSOT G24 / §11.6 · ECX-10434 切片 0 骨架）。
 *
 * <p>前缀：{@code /api/v1/h5app/wxapp/promotion/member-tasks}
 */
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("promotionsFrontV1MemberTasks")
@RequestMapping("/api/v1/h5app/wxapp/promotion/member-tasks")
public class MemberTasksFrontController {

	private final MemberTaskSkeletonService memberTaskSkeletonService;
	private final LangueProperties langueProperties;

	public MemberTasksFrontController(
			MemberTaskSkeletonService memberTaskSkeletonService, LangueProperties langueProperties) {
		this.memberTaskSkeletonService = memberTaskSkeletonService;
		this.langueProperties = langueProperties;
	}

	@GetMapping(name = "C端会员任务列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> list() {
		Locale locale = LocaleContextHolder.getLocale();
		RequestLangTag.current(langueProperties);
		memberTaskSkeletonService.throwNotImplemented(locale);
		return ResponseEntity.ok(ApiResult.ok(Map.of()));
	}
}
