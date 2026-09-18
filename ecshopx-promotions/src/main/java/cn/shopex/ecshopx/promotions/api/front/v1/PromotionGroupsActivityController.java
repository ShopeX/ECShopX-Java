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
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsActivityListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("promotionsFrontV1PromotionGroupsActivity")
@RequestMapping("/api/v1/h5app/wxapp/promotions")
public class PromotionGroupsActivityController {

	private final PromotionGroupsActivityListService promotionGroupsActivityListService;
	private final MessageSource messageSource;
	private final LangueProperties langueProperties;

	public PromotionGroupsActivityController(
			PromotionGroupsActivityListService promotionGroupsActivityListService,
			MessageSource messageSource,
			LangueProperties langueProperties) {
		this.promotionGroupsActivityListService = promotionGroupsActivityListService;
		this.messageSource = messageSource;
		this.langueProperties = langueProperties;
	}

	@GetMapping(value = "/groups", name = "拼团列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getPromotionGroupsActivityList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSizeRaw,
			@RequestParam(value = "keywords", required = false) String keywords,
			@RequestParam(value = "view", required = false) String viewRaw,
			@RequestParam(value = "group_goods_type", required = false) String groupGoodsTypeRaw) {
		int viewCheck;
		if (viewRaw == null) {
			viewCheck = 1;
		} else if (!StringUtils.hasText(viewRaw.trim())) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.groups.activity_list_failed", null, LocaleContextHolder.getLocale()));
		} else {
			try {
				viewCheck = Integer.parseInt(LeadingNumberParser.parseAsString(viewRaw.trim()));
			} catch (NumberFormatException e) {
				throw new ResourceException(
						messageSource.getMessage(
								"promotions.groups.activity_list_failed", null, LocaleContextHolder.getLocale()));
			}
			if (viewCheck != 1 && viewCheck != 2) {
				throw new ResourceException(
						messageSource.getMessage(
								"promotions.groups.activity_list_failed", null, LocaleContextHolder.getLocale()));
			}
		}
		Integer viewForTimeFilter = viewRaw == null ? null : Integer.valueOf(viewCheck);
		String effectiveType =
				StringUtils.hasText(groupGoodsTypeRaw) ? groupGoodsTypeRaw.trim() : "services";
		long companyId = parseCompanyIdFromRequest(request);
		String requestLangTag = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				promotionGroupsActivityListService.getPromotionGroupsActivityList(
						companyId, pageRaw, pageSizeRaw, keywords, viewForTimeFilter, effectiveType, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
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
