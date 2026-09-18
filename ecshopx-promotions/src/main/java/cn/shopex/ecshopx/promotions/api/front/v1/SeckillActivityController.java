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
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.wxapp.WxappSeckillActivityInfoService;
import cn.shopex.ecshopx.promotions.service.wxapp.WxappSeckillActivityListService;
import cn.shopex.ecshopx.promotions.service.wxapp.WxappSeckillItemTicketService;
import cn.shopex.ecshopx.promotions.service.wxapp.WxappSeckillStoreTicketService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("promotionsFrontV1SeckillActivity")
@RequestMapping("/api/v1/h5app/wxapp/promotion/seckillactivity")
public class SeckillActivityController {

	private final WxappSeckillItemTicketService wxappSeckillItemTicketService;
	private final WxappSeckillActivityInfoService wxappSeckillActivityInfoService;
	private final WxappSeckillActivityListService wxappSeckillActivityListService;
	private final WxappSeckillStoreTicketService wxappSeckillStoreTicketService;
	private final MessageSource messageSource;
	private final LangueProperties langueProperties;

	public SeckillActivityController(
			WxappSeckillItemTicketService wxappSeckillItemTicketService,
			WxappSeckillActivityInfoService wxappSeckillActivityInfoService,
			WxappSeckillActivityListService wxappSeckillActivityListService,
			WxappSeckillStoreTicketService wxappSeckillStoreTicketService,
			MessageSource messageSource,
			LangueProperties langueProperties) {
		this.wxappSeckillItemTicketService = wxappSeckillItemTicketService;
		this.wxappSeckillActivityInfoService = wxappSeckillActivityInfoService;
		this.wxappSeckillActivityListService = wxappSeckillActivityListService;
		this.wxappSeckillStoreTicketService = wxappSeckillStoreTicketService;
		this.messageSource = messageSource;
		this.langueProperties = langueProperties;
	}

	@GetMapping(value = "/geticket", name = "秒杀资格", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getSeckillItemTicket(
			HttpServletRequest request,
			@RequestParam(value = "seckill_id", required = false) String seckillIdRaw,
			@RequestParam(value = "item_id", required = false) String itemIdRaw,
			@RequestParam(value = "num", required = false, defaultValue = "1") String numRaw) {
		Locale locale = LocaleContextHolder.getLocale();
		if (locale == null) {
			locale = Locale.SIMPLIFIED_CHINESE;
		}
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseAuthUserIdFromClaims(readH5AuthClaimsMap(request));
		if (userId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		long seckillId = parseRequiredPositiveLong(seckillIdRaw, "promotions.seckill.seckill_id_required", locale);
		long itemId = parseRequiredPositiveLong(itemIdRaw, "promotions.seckill.item_id_param_missing", locale);
		int num = parseTicketNum(numRaw, locale);
		Object ticket = wxappSeckillItemTicketService.getSeckillItemTicket(userId, companyId, seckillId, itemId, num, locale);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ticket", ticket);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	private long parseRequiredPositiveLong(String raw, String messageKey, Locale locale) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
		try {
			long v = Long.parseLong(raw.trim());
			if (v < 1L) {
				throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
	}

	private int parseTicketNum(String numRaw, Locale locale) {
		String t = numRaw == null ? "" : numRaw.trim();
		try {
			int n = Integer.parseInt(t.isEmpty() ? "1" : t);
			if (n < 1) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.seckill.ticket_num_invalid", null, locale));
			}
			return n;
		} catch (NumberFormatException e) {
			throw new BadRequestException(messageSource.getMessage("promotions.seckill.ticket_num_invalid", null, locale));
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
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

	private static long parseAuthUserIdFromClaims(Map<String, Object> claims) {
		Object v = claims == null ? null : claims.get("user_id");
		if (v == null) {
			return 0L;
		}
		try {
			long parsed = (v instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(v).trim());
			return parsed > 0L ? parsed : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@DeleteMapping(value = "/cancelTicket", name = "取消秒杀资格")
	public ResponseEntity<ApiResult<Map<String, Object>>> cancelSeckillTicket(
			HttpServletRequest request,
			@RequestParam(value = "seckill_ticket", required = false) String seckillTicket) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseAuthUserIdFromClaims(readH5AuthClaimsMap(request));
		if (userId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		wxappSeckillStoreTicketService.cancelSeckillTicket(seckillTicket, userId, companyId);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@FrontNoAuth
	@GetMapping(value = "/getlist", name = "秒杀列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getSeckillList(
			HttpServletRequest request,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "item_type", required = false, defaultValue = "normal") String itemTypeRaw,
			@RequestParam(value = "seckill_type", required = false, defaultValue = "normal") String seckillTypeRaw,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSizeRaw) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseAuthUserIdFromClaims(readH5AuthClaimsMap(request));
		int page = 1;
		if (pageRaw != null) {
			String trimmedPage = pageRaw.trim();
			if (!trimmedPage.isEmpty()) {
				try {
					int parsedPage = Integer.parseInt(trimmedPage);
					if (parsedPage >= 1) {
						page = parsedPage;
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		int pageSize = 20;
		if (pageSizeRaw != null) {
			String trimmedSize = pageSizeRaw.trim();
			if (!trimmedSize.isEmpty()) {
				try {
					int parsedSize = Integer.parseInt(trimmedSize);
					if (parsedSize >= 1) {
						pageSize = parsedSize;
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		String acceptLang = RequestLangTag.current(langueProperties);
		String langTag = resolveLangTag(request, Map.of());
		Map<String, Object> data =
				wxappSeckillActivityListService.getSeckillList(
						companyId, userId, status, itemTypeRaw, seckillTypeRaw, page, pageSize, acceptLang, langTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@GetMapping(value = "/getinfo", name = "秒杀详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getSeckillInfo(
			HttpServletRequest request,
			@RequestParam(value = "seckill_id", required = false) String seckillIdRaw,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSizeRaw) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseAuthUserIdFromClaims(readH5AuthClaimsMap(request));
		int page = 1;
		if (pageRaw != null) {
			String trimmedPage = pageRaw.trim();
			if (!trimmedPage.isEmpty()) {
				try {
					int parsedPage = Integer.parseInt(trimmedPage);
					if (parsedPage >= 1) {
						page = parsedPage;
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		int pageSize = 20;
		if (pageSizeRaw != null) {
			String trimmedSize = pageSizeRaw.trim();
			if (!trimmedSize.isEmpty()) {
				try {
					int parsedSize = Integer.parseInt(trimmedSize);
					if (parsedSize >= 1) {
						pageSize = parsedSize;
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		String acceptLang = RequestLangTag.current(langueProperties);
		String langTag = resolveLangTag(request, Map.of());
		Map<String, Object> data =
				wxappSeckillActivityInfoService.getSeckillInfo(
						companyId, userId, seckillIdRaw, page, pageSize, acceptLang, langTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private String resolveLangTag(HttpServletRequest request, Map<String, Object> merged) {
		String countryCodeTrimmed = Objects.toString(merged.get("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		return StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
	}
}
