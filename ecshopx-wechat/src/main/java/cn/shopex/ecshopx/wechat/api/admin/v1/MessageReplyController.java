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
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.wechat.service.DefaultReplyQueryService;
import cn.shopex.ecshopx.wechat.service.DefaultReplyRedisService;
import cn.shopex.ecshopx.wechat.service.KeywordAutoreplyQueryService;
import cn.shopex.ecshopx.wechat.service.KeywordAutoreplyRedisService;
import cn.shopex.ecshopx.wechat.service.OpenKfReplyService;
import cn.shopex.ecshopx.wechat.service.SubscribeReplyQueryService;
import cn.shopex.ecshopx.wechat.service.SubscribeReplyRedisService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("wechatAdminV1MessageReply")
@RequestMapping("/api/v1/wechat")
public class MessageReplyController {

	private final SubscribeReplyRedisService subscribeReplyRedisService;
	private final DefaultReplyRedisService defaultReplyRedisService;
	private final DefaultReplyQueryService defaultReplyQueryService;
	private final KeywordAutoreplyRedisService keywordAutoreplyRedisService;
	private final KeywordAutoreplyQueryService keywordAutoreplyQueryService;
	private final OpenKfReplyService openKfReplyService;
	private final SubscribeReplyQueryService subscribeReplyQueryService;

	public MessageReplyController(
			SubscribeReplyRedisService subscribeReplyRedisService,
			DefaultReplyRedisService defaultReplyRedisService,
			DefaultReplyQueryService defaultReplyQueryService,
			KeywordAutoreplyRedisService keywordAutoreplyRedisService,
			KeywordAutoreplyQueryService keywordAutoreplyQueryService,
			OpenKfReplyService openKfReplyService,
			SubscribeReplyQueryService subscribeReplyQueryService) {
		this.subscribeReplyRedisService = subscribeReplyRedisService;
		this.defaultReplyRedisService = defaultReplyRedisService;
		this.defaultReplyQueryService = defaultReplyQueryService;
		this.keywordAutoreplyRedisService = keywordAutoreplyRedisService;
		this.keywordAutoreplyQueryService = keywordAutoreplyQueryService;
		this.openKfReplyService = openKfReplyService;
		this.subscribeReplyQueryService = subscribeReplyQueryService;
	}

	@Activated(routeAlias = "wechat.keyword.messagereply.add")
	@PostMapping(value = "/keyword/reply", name = "新增关键字回复", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> addKeywordReply(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object rawAppid = map.get("authorizer_appid");
		String authorizerAppId = (rawAppid == null) ? null : String.valueOf(rawAppid).trim();
		Object ruleNameRaw = resolveKeywordReplyField(request, body, "rule_name");
		String ruleNameParam =
				ruleNameRaw == null ? null : (ruleNameRaw instanceof String s ? s : String.valueOf(ruleNameRaw));
		Object keywordsRaw = resolveKeywordReplyRaw(request, body);
		Object replyType = resolveKeywordReplyField(request, body, "reply_type");
		Object replyContent = resolveKeywordReplyField(request, body, "reply_content");
		keywordAutoreplyRedisService.addKeywordReply(
				authorizerAppId, ruleNameParam, keywordsRaw, replyType, replyContent);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.keyword.messagereply.get")
	@GetMapping(value = "/keyword/reply", name = "关键字回复列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getKeywordReplyList(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object rawAppid = map.get("authorizer_appid");
		String authorizerAppId = rawAppid == null ? null : String.valueOf(rawAppid).trim();
		List<Map<String, Object>> list = keywordAutoreplyQueryService.getKeywordReplyList(authorizerAppId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.keyword.messagereply.put")
	@PutMapping(value = "/keyword/reply", name = "更新关键字回复", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateKeywordReply(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object rawAppid = map.get("authorizer_appid");
		String authorizerAppId = (rawAppid == null) ? null : String.valueOf(rawAppid).trim();
		Object ruleNameRaw = resolveKeywordReplyField(request, body, "rule_name");
		String ruleNameParam =
				ruleNameRaw == null ? null : (ruleNameRaw instanceof String s ? s : String.valueOf(ruleNameRaw));
		Object keywordsRaw = resolveKeywordReplyRaw(request, body);
		Object replyType = resolveKeywordReplyField(request, body, "reply_type");
		Object replyContent = resolveKeywordReplyField(request, body, "reply_content");
		keywordAutoreplyRedisService.updateKeywordReply(
				authorizerAppId, ruleNameParam, keywordsRaw, replyType, replyContent);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.keyword.messagereply.delete")
	@DeleteMapping(value = "/keyword/reply", name = "删除关键字回复", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteKeywordReply(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object rawAppid = map.get("authorizer_appid");
		String authorizerAppId = (rawAppid == null) ? null : String.valueOf(rawAppid).trim();
		Object ruleNameRaw = resolveKeywordReplyField(request, body, "rule_name");
		String ruleNameParam =
				ruleNameRaw == null ? null : (ruleNameRaw instanceof String s ? s : String.valueOf(ruleNameRaw));
		keywordAutoreplyRedisService.deleteKeywordReply(authorizerAppId, ruleNameParam);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.default.messagereply.add")
	@PostMapping(value = "/default/reply", name = "设置默认回复", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> setDefaultReply(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object rawAppid = map.get("authorizer_appid");
		String authorizerAppId = (rawAppid == null) ? null : String.valueOf(rawAppid).trim();
		Object replyType = resolveSubscribeReplyField(request, body, "reply_type");
		Object replyContent = resolveSubscribeReplyField(request, body, "reply_content");
		defaultReplyRedisService.setDefaultReply(authorizerAppId, replyType, replyContent);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.default.messagereply.get")
	@GetMapping(value = "/default/reply", name = "获取默认回复", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getDefaultReply(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object rawAppid = map.get("authorizer_appid");
		String authorizerAppId = rawAppid == null ? null : String.valueOf(rawAppid).trim();
		Map<String, Object> body = defaultReplyQueryService.getDefaultReply(authorizerAppId);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "wechat.openkf.messagereply.get")
	@GetMapping(value = "/openkf/reply", name = "多客服回复查询", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getOpenKfReply() {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object rawAppid = map.get("authorizer_appid");
		String authorizerAppId = (rawAppid == null) ? null : String.valueOf(rawAppid);
		boolean status = openKfReplyService.getOpenKfReply(authorizerAppId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("isOpenKfReply", status);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.openkf.messagereply.set")
	@PostMapping(value = "/openkf/reply", name = "多客服回复设置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> setOpenKfReply(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object rawAppid = map.get("authorizer_appid");
		String authorizerAppId = (rawAppid == null) ? null : String.valueOf(rawAppid);
		Object raw = resolveOpenKfReplyField(request, body);
		Object echo = (raw == null) ? "false" : raw;
		openKfReplyService.setOpenKfReply(authorizerAppId, echo);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("isOpenKfReply", echo);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.subscribe.messagereply.get")
	@GetMapping(value = "/subscribe/reply", name = "关注回复查询", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getSubscribeReply() {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object rawAppid = map.get("authorizer_appid");
		String authorizerAppId = (rawAppid == null) ? null : String.valueOf(rawAppid);
		Map<String, Object> body = subscribeReplyQueryService.getSubscribeReply(authorizerAppId);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "wechat.subscribe.messagereply.set")
	@PostMapping(value = "/subscribe/reply", name = "关注回复设置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> setSubscribeReply(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object rawAppid = map.get("authorizer_appid");
		String authorizerAppId = (rawAppid == null) ? null : String.valueOf(rawAppid);
		Object replyType = resolveSubscribeReplyField(request, body, "reply_type");
		Object replyContent = resolveSubscribeReplyField(request, body, "reply_content");
		subscribeReplyRedisService.setSubscribeReply(authorizerAppId, replyType, replyContent);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Object resolveSubscribeReplyField(HttpServletRequest request, Map<String, Object> body, String name) {
		if (body != null && body.containsKey(name)) {
			return body.get(name);
		}
		if (request.getParameterMap().containsKey(name)) {
			String raw = request.getParameter(name);
			return (raw == null) ? "" : raw.trim();
		}
		return null;
	}

	private static Object resolveOpenKfReplyField(HttpServletRequest request, Map<String, Object> body) {
		if (body != null && body.containsKey("isOpenKfReply")) {
			return body.get("isOpenKfReply");
		}
		if (request.getParameterMap().containsKey("isOpenKfReply")) {
			String raw = request.getParameter("isOpenKfReply");
			return (raw == null) ? "" : raw.trim();
		}
		return null;
	}

	private static Object resolveKeywordReplyField(HttpServletRequest request, Map<String, Object> body, String name) {
		if (body != null && body.containsKey(name)) {
			return body.get(name);
		}
		if (request.getParameterMap().containsKey(name)) {
			String raw = request.getParameter(name);
			return (raw == null) ? "" : raw.trim();
		}
		return null;
	}

	private static Object resolveKeywordReplyRaw(HttpServletRequest request, Map<String, Object> body) {
		if (body != null && body.containsKey("keywords_rule")) {
			return body.get("keywords_rule");
		}
		if (request.getParameterMap().containsKey("keywords_rule")) {
			return request.getParameter("keywords_rule");
		}
		return null;
	}
}
