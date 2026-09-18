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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.wechat.WechatDirectBindMemberConflictPort;
import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.mapper.WeappMapper;
import cn.shopex.ecshopx.wechat.mapper.WechatAuthMapper;
import cn.shopex.ecshopx.wechat.service.openplatform.WechatOpenPlatformQueryAuthClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class WechatAuthBindService {

	private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final WechatOpenPlatformQueryAuthClient openPlatformClient;
	private final WechatAuthMapper wechatAuthMapper;
	private final WeappMapper weappMapper;
	private final WechatDirectBindMemberConflictPort memberConflictPort;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate directBindMiniprogramTxn;

	public WechatAuthBindService(
			WechatOpenPlatformQueryAuthClient openPlatformClient,
			WechatAuthMapper wechatAuthMapper,
			WeappMapper weappMapper,
			WechatDirectBindMemberConflictPort memberConflictPort,
			ObjectMapper objectMapper,
			PlatformTransactionManager transactionManager) {
		this.openPlatformClient = openPlatformClient;
		this.wechatAuthMapper = wechatAuthMapper;
		this.weappMapper = weappMapper;
		this.memberConflictPort = memberConflictPort;
		this.objectMapper = objectMapper;
		this.directBindMiniprogramTxn = new TransactionTemplate(transactionManager);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> authorizedBind(long companyId, long operatorId, String authCode, String authType) {
		JsonNode queryRoot = openPlatformClient.queryAuthByAuthorizationCode(authCode);
		JsonNode authzInfo = queryRoot.path("authorization_info");
		if (authzInfo.isMissingNode() || !authzInfo.isObject() || authzInfo.size() == 0) {
			throw new ResourceException("授权数据无效");
		}
		String rawAppid = authzInfo.path("authorizer_appid").asText(null);
		String newAppid = rawAppid == null ? null : rawAppid.trim();
		if (!StringUtils.hasText(newAppid)) {
			throw new ResourceException("授权数据无效");
		}

		JsonNode authorizerRoot = openPlatformClient.getAuthorizerInfo(newAppid);
		JsonNode authorizerInfo = authorizerRoot.path("authorizer_info");
		if (!authorizerInfo.isObject()) {
			throw new ResourceException("授权数据无效");
		}

		WechatAuth existingByPk = wechatAuthMapper.selectById(newAppid);
		String refreshed = authzInfo.path("authorizer_refresh_token").asText(null);
		if (existingByPk != null && StringUtils.hasText(refreshed)) {
			LambdaUpdateWrapper<WechatAuth> w = new LambdaUpdateWrapper<>();
			w.eq(WechatAuth::getAuthorizerAppid, newAppid).set(WechatAuth::getAuthorizerRefreshToken, refreshed);
			wechatAuthMapper.update(null, w);
		}

		LambdaQueryWrapper<WechatAuth> q = new LambdaQueryWrapper<>();
		q.eq(WechatAuth::getCompanyId, companyId)
				.eq(WechatAuth::getServiceTypeInfo, 2)
				.eq(WechatAuth::getBindStatus, "bind")
				.isNull(WechatAuth::getDeletedAt)
				.last("LIMIT 1");
		WechatAuth current = wechatAuthMapper.selectOne(q);

		boolean hasMini = miniProgramSectionPresent(authorizerInfo);
		int serviceTypeId = resolveServiceTypeId(authorizerInfo);
		if (current != null
				&& !newAppid.equals(current.getAuthorizerAppid())
				&& serviceTypeId == 2) {
			LambdaUpdateWrapper<WechatAuth> u = new LambdaUpdateWrapper<>();
			u.eq(WechatAuth::getAuthorizerAppid, current.getAuthorizerAppid()).set(WechatAuth::getBindStatus, "unbind");
			wechatAuthMapper.update(null, u);
		}

		if (existingByPk != null
				&& "bind".equals(existingByPk.getBindStatus())
				&& existingByPk.getCompanyId() != null
				&& !existingByPk.getCompanyId().equals(companyId)) {
			throw new ResourceException("当前公众号已绑定其他账号，请解绑后在绑定");
		}

		boolean isSvcOrMini = (serviceTypeId == 2 || serviceTypeId == 3 || hasMini);
		if (!isSvcOrMini) {
			throw new BadRequestException("只支持服务号或小程序授权");
		}
		if ("woa".equals(authType) && hasMini) {
			throw new BadRequestException("请授权公众号账号");
		}
		if ("wxa".equals(authType) && !hasMini) {
			throw new BadRequestException("请授权小程序账号");
		}

		String principalName = authorizerInfo.path("principal_name").asText("");
		if ("个人".equals(principalName)) {
			throw new BadRequestException("不支持个人账号授权，请使用企业账号");
		}

		JsonNode verifyNode = authorizerInfo.path("verify_type_info");
		Integer verifyId = verifyNode.isObject() && verifyNode.has("id") && verifyNode.get("id").isNumber()
				? verifyNode.get("id").intValue()
				: null;
		if (verifyId != null && verifyId == -1) {
			throw new BadRequestException("当前账号未认证，不支持绑定");
		}

		boolean inserting = existingByPk == null;
		WechatAuth entity =
				toEntityForBind(authzInfo, authorizerInfo, companyId, operatorId, serviceTypeId, hasMini, verifyId, existingByPk);
		if (inserting) {
			wechatAuthMapper.insert(entity);
		} else {
			wechatAuthMapper.updateById(entity);
		}

		return toResponseMap(entity, inserting);
	}

	public Map<String, Object> directBind(long companyId, long operatorId, Map<String, Object> merged, String bindTypeRaw) {
		String bindType = bindTypeRaw == null ? "" : bindTypeRaw.trim();
		if (!StringUtils.hasText(bindType)
				|| (!"miniprogram".equals(bindType) && !"offiaccount".equals(bindType))) {
			throw new BadRequestException("您传入的绑定小程序或者公众号类型有误！");
		}

		if ("offiaccount".equals(bindType)) {
			requireText(merged, "nick_name", "请输入公众号名称");
			requireText(merged, "authorizer_appid", "请输入公众号appid");
			requireText(merged, "authorizer_appsecret", "请输入公众号appsecret");
		} else {
			requireText(merged, "template_name", "请选择小程序模板");
			requireText(merged, "nick_name", "请输入小程序名称");
			requireText(merged, "authorizer_appid", "请输入小程序appid");
			requireText(merged, "authorizer_appsecret", "请输入小程序appsecret");
		}

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("nick_name", textOrEmpty(merged, "nick_name"));
		params.put("authorizer_appid", textOrEmpty(merged, "authorizer_appid"));
		params.put("authorizer_appsecret", textOrEmpty(merged, "authorizer_appsecret"));
		Object sig = merged.get("signature");
		if (sig == null) {
			params.put("signature", null);
		} else {
			String s = String.valueOf(sig).trim();
			params.put("signature", s.isEmpty() ? "" : s);
		}
		params.put("company_id", companyId);
		params.put("operator_id", operatorId);
		if ("miniprogram".equals(bindType)) {
			params.put("template_name", textOrEmpty(merged, "template_name"));
		}

		if ("offiaccount".equals(bindType)) {
			LambdaQueryWrapper<WechatAuth> q = new LambdaQueryWrapper<>();
			q.eq(WechatAuth::getCompanyId, companyId)
					.eq(WechatAuth::getBindStatus, "bind")
					.eq(WechatAuth::getServiceTypeInfo, 2)
					.isNull(WechatAuth::getDeletedAt)
					.last("LIMIT 1");
			WechatAuth old = wechatAuthMapper.selectOne(q);
			if (old != null && StringUtils.hasText(old.getAuthorizerAppid())) {
				params.put("old_authorize_appid", old.getAuthorizerAppid().trim());
			}
			return directAuthorizedInternal(params, bindType, companyId, operatorId);
		}

		String oldAppid = null;
		String templateName = textOrEmpty(merged, "template_name");
		LambdaQueryWrapper<Weapp> wq = new LambdaQueryWrapper<>();
		wq.eq(Weapp::getCompanyId, companyId)
				.eq(Weapp::getTemplateName, templateName.trim())
				.isNull(Weapp::getDeletedAt)
				.last("LIMIT 1");
		Weapp weappinfo = weappMapper.selectOne(wq);
		if (weappinfo != null) {
			oldAppid = weappinfo.getAuthorizerAppid() == null ? null : weappinfo.getAuthorizerAppid().trim();
			WechatAuth authinfo = oldAppid == null ? null : wechatAuthMapper.selectById(oldAppid);
			if (authinfo != null && (authinfo.getIsDirect() == null || authinfo.getIsDirect() != 1)) {
				throw new ResourceException("历史绑定的小程序不是直连绑定，不能更换为直连");
			}
			if (authinfo != null) {
				params.put("old_authorize_appid", oldAppid);
				String newAppid = textOrEmpty(merged, "authorizer_appid").trim();
				String existingAppid =
						authinfo.getAuthorizerAppid() == null ? "" : authinfo.getAuthorizerAppid().trim();
				if (!newAppid.equals(existingAppid)) {
					memberConflictPort.assertNoBlockingUsers(companyId, authinfo.getAuthorizerAppid());
				}
			}
		}
		final String oldAppidForTxn = oldAppid;
		return directBindMiniprogramTxn.execute(status -> {
			try {
				Map<String, Object> result = directAuthorizedInternal(params, "miniprogram", companyId, operatorId);
				String newAppid = textOrEmpty(params, "authorizer_appid").trim();
				String tplName = textOrEmpty(params, "template_name").trim();
				Weapp weappSave = new Weapp();
				weappSave.setAuthorizerAppid(newAppid);
				weappSave.setOperatorId(operatorId);
				weappSave.setCompanyId(companyId);
				weappSave.setTemplateId("0");
				weappSave.setTemplateName(tplName);
				weappSave.setTemplateVer("1");
				weappSave.setAuditStatus(0);
				weappSave.setReleaseStatus(0);
				weappSave.setVisitStatus(1);
				weappSave.setAuditTime(String.valueOf(Instant.now().getEpochSecond()));
				LocalDateTime now = LocalDateTime.now();
				weappSave.setUpdatedAt(now);
				String wxaKeyForUpsert = StringUtils.hasText(oldAppidForTxn) ? oldAppidForTxn.trim() : newAppid;
				createWeappInternal(wxaKeyForUpsert, weappSave);
				return result;
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				throw new ResourceException(e.getMessage());
			}
		});
	}

	private Map<String, Object> directAuthorizedInternal(
			Map<String, Object> params, String bindType, long companyId, long operatorId) {
		String newAppid = textOrEmpty(params, "authorizer_appid").trim();
		String bindLabel = "offiaccount".equals(bindType) ? "公众号" : "小程序";
		WechatAuth wechatAuthData = wechatAuthMapper.selectById(newAppid);
		if (wechatAuthData != null
				&& wechatAuthData.getCompanyId() != null
				&& !wechatAuthData.getCompanyId().equals(companyId)) {
			throw new ResourceException("绑定失败，" + bindLabel + "已被占用");
		}
		if (wechatAuthData != null && (wechatAuthData.getIsDirect() == null || wechatAuthData.getIsDirect() != 1)) {
			throw new ResourceException(bindLabel + "已经绑定过第三方授权，不能改为直连");
		}
		String oldAuthorizeRaw = params.get("old_authorize_appid") == null
				? null
				: String.valueOf(params.get("old_authorize_appid")).trim();
		String oldAuthorize = StringUtils.hasText(oldAuthorizeRaw) ? oldAuthorizeRaw : null;
		if (StringUtils.hasText(oldAuthorize)
				&& wechatAuthData != null
				&& wechatAuthData.getAuthorizerAppid() != null
				&& !oldAuthorize.equals(wechatAuthData.getAuthorizerAppid().trim())) {
			throw new ResourceException("更换直连" + bindLabel + "有误，新的" + bindLabel + "已存在");
		}

		int serviceTypeInfo = "miniprogram".equals(bindType) ? 3 : 2;
		WechatAuth toSave = buildDirectWechatAuthEntity(params, serviceTypeInfo, companyId, operatorId, newAppid);
		LocalDateTime now = LocalDateTime.now();
		toSave.setUpdatedAt(now);

		boolean inserted;
		if (!StringUtils.hasText(oldAuthorize) && wechatAuthData == null) {
			toSave.setCreatedAt(now);
			wechatAuthMapper.insert(toSave);
			inserted = true;
		} else if (StringUtils.hasText(oldAuthorize) && wechatAuthData == null) {
			WechatAuth oldRow = wechatAuthMapper.selectById(oldAuthorize);
			if (oldRow == null) {
				throw new ResourceException("更换直连授权失败，原绑定不存在");
			}
			LambdaUpdateWrapper<WechatAuth> uw = new LambdaUpdateWrapper<>();
			uw.eq(WechatAuth::getAuthorizerAppid, oldAuthorize);
			applyAllWechatAuthColumns(uw, toSave);
			wechatAuthMapper.update(null, uw);
			inserted = false;
		} else {
			toSave.setCreatedAt(wechatAuthData.getCreatedAt());
			wechatAuthMapper.updateById(toSave);
			inserted = false;
		}

		return buildDirectAuthorizedResponseMap(toSave, inserted);
	}

	private void applyAllWechatAuthColumns(LambdaUpdateWrapper<WechatAuth> uw, WechatAuth e) {
		uw.set(WechatAuth::getAuthorizerAppid, e.getAuthorizerAppid())
				.set(WechatAuth::getAuthorizerRefreshToken, e.getAuthorizerRefreshToken())
				.set(WechatAuth::getNickName, e.getNickName())
				.set(WechatAuth::getUserName, e.getUserName())
				.set(WechatAuth::getAlias, e.getAlias())
				.set(WechatAuth::getQrcodeUrl, e.getQrcodeUrl())
				.set(WechatAuth::getHeadImg, e.getHeadImg())
				.set(WechatAuth::getServiceTypeInfo, e.getServiceTypeInfo())
				.set(WechatAuth::getVerifyTypeInfo, e.getVerifyTypeInfo())
				.set(WechatAuth::getBusinessInfo, e.getBusinessInfo())
				.set(WechatAuth::getPrincipalName, e.getPrincipalName())
				.set(WechatAuth::getSignature, e.getSignature())
				.set(WechatAuth::getFuncInfo, e.getFuncInfo())
				.set(WechatAuth::getMiniprograminfo, e.getMiniprograminfo())
				.set(WechatAuth::getAuthorizerAppsecret, e.getAuthorizerAppsecret())
				.set(WechatAuth::getBindStatus, e.getBindStatus())
				.set(WechatAuth::getCompanyId, e.getCompanyId())
				.set(WechatAuth::getOperatorId, e.getOperatorId())
				.set(WechatAuth::getIsDirect, e.getIsDirect())
				.set(WechatAuth::getUpdatedAt, e.getUpdatedAt())
				.set(WechatAuth::getAutoPublish, e.getAutoPublish());
	}

	private WechatAuth buildDirectWechatAuthEntity(
			Map<String, Object> params, int serviceTypeInfo, long companyId, long operatorId, String newAppid) {
		WechatAuth e = new WechatAuth();
		e.setAuthorizerAppid(newAppid);
		e.setAuthorizerRefreshToken("");
		e.setNickName(textOrEmpty(params, "nick_name"));
		e.setUserName("");
		e.setAlias("");
		e.setQrcodeUrl("");
		e.setHeadImg("");
		e.setServiceTypeInfo(serviceTypeInfo);
		e.setVerifyTypeInfo(-1);
		e.setBusinessInfo("{}");
		e.setPrincipalName("");
		Object sig = params.get("signature");
		e.setSignature(sig == null ? "" : String.valueOf(sig));
		e.setFuncInfo("");
		e.setMiniprograminfo("{}");
		e.setAuthorizerAppsecret(textOrEmpty(params, "authorizer_appsecret").trim());
		e.setBindStatus("bind");
		e.setCompanyId(companyId);
		e.setOperatorId(operatorId);
		e.setIsDirect(1);
		e.setAutoPublish(0);
		return e;
	}

	private Map<String, Object> buildDirectAuthorizedResponseMap(WechatAuth entity, boolean inserted) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("authorizer_refresh_token", entity.getAuthorizerRefreshToken());
		m.put("nick_name", entity.getNickName());
		m.put("user_name", entity.getUserName());
		m.put("alias", entity.getAlias());
		m.put("qrcode_url", entity.getQrcodeUrl());
		m.put("head_img", entity.getHeadImg());
		m.put("service_type_info", entity.getServiceTypeInfo());
		m.put("verify_type_info", entity.getVerifyTypeInfo());
		m.put("business_info", entity.getBusinessInfo());
		m.put("principal_name", entity.getPrincipalName());
		m.put("signature", entity.getSignature());
		m.put("func_info", entity.getFuncInfo());
		m.put("miniprograminfo", entity.getMiniprograminfo());
		m.put("authorizer_appsecret", entity.getAuthorizerAppsecret());
		m.put("bind_status", entity.getBindStatus());
		m.put("company_id", entity.getCompanyId());
		m.put("operator_id", entity.getOperatorId());
		if (entity.getUpdatedAt() != null) {
			m.put("updated_at", entity.getUpdatedAt().format(TS_FMT));
		}
		if (inserted && entity.getCreatedAt() != null) {
			m.put("created_at", entity.getCreatedAt().format(TS_FMT));
		}
		m.put("is_direct", 1);
		m.put("authorizer_appid", entity.getAuthorizerAppid());
		return m;
	}

	private void createWeappInternal(String wxaAppId, Weapp row) {
		String lookup = wxaAppId.trim();
		String newAppid = row.getAuthorizerAppid() == null ? "" : row.getAuthorizerAppid().trim();
		Weapp existing = weappMapper.selectById(lookup);
		LocalDateTime now = LocalDateTime.now();
		row.setAuditTime(String.valueOf(Instant.now().getEpochSecond()));
		row.setUpdatedAt(now);
		if (existing != null) {
			LambdaUpdateWrapper<Weapp> uw = new LambdaUpdateWrapper<>();
			uw.eq(Weapp::getAuthorizerAppid, lookup)
					.set(Weapp::getAuthorizerAppid, newAppid)
					.set(Weapp::getOperatorId, row.getOperatorId())
					.set(Weapp::getCompanyId, row.getCompanyId())
					.set(Weapp::getTemplateId, row.getTemplateId())
					.set(Weapp::getTemplateName, row.getTemplateName())
					.set(Weapp::getTemplateVer, row.getTemplateVer())
					.set(Weapp::getAuditStatus, row.getAuditStatus())
					.set(Weapp::getReleaseStatus, row.getReleaseStatus())
					.set(Weapp::getVisitStatus, row.getVisitStatus())
					.set(Weapp::getAuditTime, row.getAuditTime())
					.set(Weapp::getUpdatedAt, row.getUpdatedAt());
			weappMapper.update(null, uw);
		} else {
			row.setCreatedAt(now);
			weappMapper.insert(row);
		}
	}

	private static void requireText(Map<String, Object> merged, String key, String message) {
		String v = merged.get(key) == null ? null : String.valueOf(merged.get(key)).trim();
		if (!StringUtils.hasText(v)) {
			throw new BadRequestException(message);
		}
	}

	private static String textOrEmpty(Map<String, Object> merged, String key) {
		Object v = merged.get(key);
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
	}

	private static boolean miniProgramSectionPresent(JsonNode authorizerInfo) {
		return authorizerInfo.hasNonNull("MiniProgramInfo")
				&& authorizerInfo.get("MiniProgramInfo").isObject()
				&& authorizerInfo.get("MiniProgramInfo").size() > 0;
	}

	private int resolveServiceTypeId(JsonNode authorizerInfo) {
		if (miniProgramSectionPresent(authorizerInfo)) {
			return 3;
		}
		JsonNode idNode = authorizerInfo.path("service_type_info").path("id");
		if (idNode.isNumber()) {
			return idNode.asInt();
		}
		return 0;
	}

	private WechatAuth toEntityForBind(
			JsonNode authzInfo,
			JsonNode authorizerInfo,
			long companyId,
			long operatorId,
			int serviceTypeId,
			boolean hasMini,
			Integer verifyId,
			WechatAuth existingByPk) {
		WechatAuth e = new WechatAuth();
		String appid = authzInfo.path("authorizer_appid").asText("").trim();
		e.setAuthorizerAppid(appid);
		e.setAuthorizerRefreshToken(authzInfo.path("authorizer_refresh_token").asText(null));
		e.setCompanyId(companyId);
		e.setOperatorId(operatorId);
		e.setBindStatus("bind");
		e.setServiceTypeInfo(serviceTypeId);
		e.setVerifyTypeInfo(verifyId != null ? verifyId : -1);
		e.setNickName(authorizerInfo.path("nick_name").asText(null));
		e.setUserName(authorizerInfo.path("user_name").asText(null));
		e.setAlias(authorizerInfo.path("alias").asText(null));
		e.setQrcodeUrl(authorizerInfo.path("qrcode_url").asText(null));
		e.setHeadImg(authorizerInfo.path("head_img").asText(null));
		e.setSignature(authorizerInfo.path("signature").asText(null));
		e.setPrincipalName(authorizerInfo.path("principal_name").asText(null));
		e.setBusinessInfo(encodeBusinessInfo(authorizerInfo));
		e.setFuncInfo(buildFuncInfo(authzInfo.path("func_info")));
		e.setMiniprograminfo(encodeMiniprogramInfo(authorizerInfo, hasMini));
		String secret = authorizerInfo.path("authorizer_appsecret").asText(null);
		e.setAuthorizerAppsecret(StringUtils.hasText(secret) ? secret.trim() : null);
		e.setIsDirect(0);
		LocalDateTime now = LocalDateTime.now();
		e.setUpdatedAt(now);
		if (existingByPk == null) {
			e.setCreatedAt(now);
		} else {
			e.setCreatedAt(existingByPk.getCreatedAt());
		}
		return e;
	}

	private String encodeBusinessInfo(JsonNode authorizerInfo) {
		try {
			JsonNode bi = authorizerInfo.path("business_info");
			if (bi.isMissingNode() || bi.isNull()) {
				return objectMapper.writeValueAsString(objectMapper.createObjectNode());
			}
			return objectMapper.writeValueAsString(bi);
		} catch (Exception ex) {
			throw new ResourceException("授权数据序列化失败");
		}
	}

	private String encodeMiniprogramInfo(JsonNode authorizerInfo, boolean hasMini) {
		try {
			if (hasMini) {
				return objectMapper.writeValueAsString(authorizerInfo.get("MiniProgramInfo"));
			}
			return objectMapper.writeValueAsString(objectMapper.createObjectNode());
		} catch (Exception ex) {
			throw new ResourceException("授权数据序列化失败");
		}
	}

	private String buildFuncInfo(JsonNode funcInfoArr) {
		if (!funcInfoArr.isArray()) {
			return "";
		}
		List<String> ids = new ArrayList<>();
		for (JsonNode row : funcInfoArr) {
			JsonNode idNode = row.path("funcscope_category").path("id");
			if (idNode.isNumber()) {
				ids.add(String.valueOf(idNode.asInt()));
			}
		}
		return String.join(",", ids);
	}

	private Map<String, Object> toResponseMap(WechatAuth entity, boolean inserted) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("authorizer_refresh_token", entity.getAuthorizerRefreshToken());
		m.put("nick_name", entity.getNickName());
		m.put("user_name", entity.getUserName());
		m.put("alias", entity.getAlias());
		m.put("qrcode_url", entity.getQrcodeUrl());
		m.put("head_img", entity.getHeadImg());
		m.put("service_type_info", entity.getServiceTypeInfo());
		m.put("verify_type_info", entity.getVerifyTypeInfo());
		m.put("business_info", entity.getBusinessInfo());
		m.put("principal_name", entity.getPrincipalName());
		m.put("signature", entity.getSignature());
		m.put("func_info", entity.getFuncInfo());
		m.put("miniprograminfo", entity.getMiniprograminfo());
		m.put("authorizer_appsecret", entity.getAuthorizerAppsecret());
		m.put("bind_status", entity.getBindStatus());
		m.put("company_id", entity.getCompanyId());
		m.put("operator_id", entity.getOperatorId());
		if (entity.getUpdatedAt() != null) {
			m.put("updated_at", entity.getUpdatedAt().format(TS_FMT));
		}
		if (inserted && entity.getCreatedAt() != null) {
			m.put("created_at", entity.getCreatedAt().format(TS_FMT));
		}
		m.put("authorizer_appid", entity.getAuthorizerAppid());
		return m;
	}
}
