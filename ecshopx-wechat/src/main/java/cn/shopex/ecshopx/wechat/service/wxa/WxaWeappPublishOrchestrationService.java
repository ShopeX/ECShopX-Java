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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.wechat.port.CompanyLiveRoomEnabledPort;
import cn.shopex.ecshopx.common.youshu.port.YoushuSettingInfoPort;
import cn.shopex.ecshopx.wechat.config.WechatLivePlayerPluginProperties;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class WxaWeappPublishOrchestrationService {

	private static final Logger log = LoggerFactory.getLogger(WxaWeappPublishOrchestrationService.class);

	private static final List<String> REQUIRED_PRIVATE_INFOS = List.of(
			"getLocation", "chooseAddress", "chooseInvoiceTitle", "getPhoneNumber");

	private final WeappRowQueryService weappRowQueryService;
	private final WeappCreateOrUpdateService weappCreateOrUpdateService;
	private final WxaSubmitAuditCheckService wxaSubmitAuditCheckService;
	private final SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService;
	private final WxaOpenPlatformCodeApiClient codeClient;
	private final YoushuSettingInfoPort youshuSettingInfoPort;
	private final CompanyLiveRoomEnabledPort companyLiveRoomEnabledPort;
	private final WechatLivePlayerPluginProperties livePlayerPluginProperties;
	private final WechatOpenPlatformAuthorizerTokenService authorizerTokenService;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;

	public WxaWeappPublishOrchestrationService(
			WeappRowQueryService weappRowQueryService,
			WeappCreateOrUpdateService weappCreateOrUpdateService,
			WxaSubmitAuditCheckService wxaSubmitAuditCheckService,
			SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService,
			WxaOpenPlatformCodeApiClient codeClient,
			YoushuSettingInfoPort youshuSettingInfoPort,
			CompanyLiveRoomEnabledPort companyLiveRoomEnabledPort,
			WechatLivePlayerPluginProperties livePlayerPluginProperties,
			WechatOpenPlatformAuthorizerTokenService authorizerTokenService,
			ObjectMapper objectMapper,
			PlatformTransactionManager transactionManager) {
		this.weappRowQueryService = weappRowQueryService;
		this.weappCreateOrUpdateService = weappCreateOrUpdateService;
		this.wxaSubmitAuditCheckService = wxaSubmitAuditCheckService;
		this.superadminWxappTemplateMetadataService = superadminWxappTemplateMetadataService;
		this.codeClient = codeClient;
		this.youshuSettingInfoPort = youshuSettingInfoPort;
		this.companyLiveRoomEnabledPort = companyLiveRoomEnabledPort;
		this.livePlayerPluginProperties = livePlayerPluginProperties;
		this.authorizerTokenService = authorizerTokenService;
		this.objectMapper = objectMapper;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@SuppressWarnings("unused")
	public void submitAudit(
			long companyId,
			String authorizerAppid,
			long operatorId,
			String templateName,
			String wxaName,
			Map<String, Object> options,
			String wxaAppId,
			boolean isOnlyCommitCode) {
		Optional<Map<String, Object>> weappInfo = weappRowQueryService.getWeappInfo(companyId, wxaAppId);
		if (weappInfo.isEmpty()) {
			Map<String, Object> ph = new LinkedHashMap<>();
			ph.put("company_id", companyId);
			ph.put("operator_id", operatorId);
			ph.put("template_id", "-1");
			ph.put("template_name", "");
			ph.put("template_ver", "1");
			ph.put("audit_status", 3);
			ph.put("release_status", 0);
			ph.put("visitstatus", 1);
			ph.put("reason", "");
			ph.put("audit_time", String.valueOf(Instant.now().getEpochSecond()));
			weappCreateOrUpdateService.saveWeapp(wxaAppId, ph);
			return;
		}
		authorizerTokenService.getAuthorizerAccessToken(wxaAppId);
		String trimmedTemplateKey = templateName == null ? "" : templateName.trim();
		wxaSubmitAuditCheckService.submitAuditCheck(
				companyId, authorizerAppid, wxaAppId, trimmedTemplateKey, wxaName);
		Map<String, Object> templateData =
				superadminWxappTemplateMetadataService.resolveTemplateRow(trimmedTemplateKey);
		Map<String, Object> mergedOptions = mergeYykWindowIfNeeded(trimmedTemplateKey, options);
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> domainMap = (Map<String, Object>) templateData.get("domain");
			codeClient.modifyDomain(wxaAppId, domainMap == null ? Map.of() : domainMap);
		} catch (Exception e) {
			log.error("modify_domain 调用失败，继续后续流程: wxaAppId={}", wxaAppId, e);
		}
		commitTemplate(companyId, wxaAppId, templateData, wxaName, mergedOptions);
		try {
			Thread.sleep(2000L);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new BadRequestException("提交审核被中断");
		}
		JsonNode cat = codeClient.getCategory(wxaAppId);
		JsonNode list = cat.path("category_list");
		if (list == null || !list.isArray() || list.isEmpty()) {
			throw new BadRequestException("请在微信小程序中设置服务类目");
		}
		JsonNode c0 = list.get(0);
		Map<String, Object> submitAuditData = buildSubmitAuditPayload(templateData, wxaName, c0);
		Map<String, Object> preSave = new LinkedHashMap<>();
		preSave.put("company_id", companyId);
		preSave.put("operator_id", operatorId);
		preSave.put("template_id", String.valueOf(templateData.get("template_id")));
		preSave.put("template_name", trimmedTemplateKey);
		preSave.put("template_ver", String.valueOf(templateData.get("version")));
		preSave.put("audit_status", 2);
		preSave.put("release_status", 0);
		preSave.put("visitstatus", 1);
		preSave.put("reason", "");
		preSave.put("audit_time", String.valueOf(Instant.now().getEpochSecond()));
		transactionTemplate.execute(status -> {
			weappCreateOrUpdateService.saveWeapp(wxaAppId, preSave);
			JsonNode res = codeClient.submitAudit(wxaAppId, submitAuditData);
			int ec = res.path("errcode").asInt(0);
			if (ec > 0 && ec != 85009) {
				status.setRollbackOnly();
				throw new BadRequestException(res.path("errmsg").asText("微信审核提交失败"));
			}
			return null;
		});
	}

	public void submitReviewWithoutCodeUpload(
			long companyId,
			String authorizerAppid,
			long operatorId,
			String templateName,
			String wxaName,
			Map<String, Object> templateOptions,
			String wxaAppId) {
		String trimmedWxa = wxaAppId == null ? "" : wxaAppId.trim();
		if (WxaUploadWxaService.wxaAppIdHasText(trimmedWxa)) {
			authorizerTokenService.getAuthorizerAccessToken(trimmedWxa);
		}
		String trimmedTemplateKey = templateName == null ? "" : templateName.trim();
		wxaSubmitAuditCheckService.submitAuditCheck(
				companyId, authorizerAppid, trimmedWxa, trimmedTemplateKey, wxaName);
		Map<String, Object> templateData =
				superadminWxappTemplateMetadataService.resolveTemplateRow(trimmedTemplateKey);
		@SuppressWarnings("unused")
		Map<String, Object> mergedOptions = mergeYykWindowIfNeeded(trimmedTemplateKey, templateOptions);
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> domainMap = (Map<String, Object>) templateData.get("domain");
			codeClient.modifyDomain(trimmedWxa, domainMap == null ? Map.of() : domainMap);
		} catch (Exception e) {
			log.error("modify_domain 调用失败: wxaAppId={}", trimmedWxa, e);
			String m = e.getMessage();
			throw new ResourceException((m != null && !m.isBlank()) ? m.trim() : "域名配置失败");
		}
		JsonNode cat = codeClient.getCategory(trimmedWxa);
		JsonNode list = cat.path("category_list");
		if (list == null || !list.isArray() || list.isEmpty()) {
			throw new BadRequestException("请在微信小程序中设置服务类目");
		}
		JsonNode c0 = list.get(0);
		Map<String, Object> submitAuditData = buildSubmitAuditPayload(templateData, wxaName, c0);
		Map<String, Object> preSave = new LinkedHashMap<>();
		preSave.put("company_id", companyId);
		preSave.put("operator_id", operatorId);
		preSave.put("template_id", String.valueOf(templateData.get("template_id")));
		preSave.put("template_name", trimmedTemplateKey);
		preSave.put("template_ver", String.valueOf(templateData.get("version")));
		preSave.put("audit_status", 2);
		preSave.put("release_status", 0);
		preSave.put("visitstatus", 1);
		preSave.put("reason", "");
		preSave.put("audit_time", String.valueOf(Instant.now().getEpochSecond()));
		weappCreateOrUpdateService.saveWeapp(trimmedWxa, preSave);
		JsonNode res = codeClient.submitAudit(trimmedWxa, submitAuditData);
		int ec = res.path("errcode").asInt(0);
		if (ec > 0 && ec != 85009) {
			throw new BadRequestException(res.path("errmsg").asText("微信审核提交失败"), 400);
		}
	}

	public void onlyCommitTempCode(
			long companyId,
			String authorizerAppid,
			long operatorId,
			String templateName,
			String wxaName,
			Map<String, Object> options,
			String wxaAppId) {
		if (StringUtils.hasText(wxaAppId)) {
			authorizerTokenService.getAuthorizerAccessToken(wxaAppId);
		}
		String trimmedTemplateKey = templateName == null ? "" : templateName.trim();
		wxaSubmitAuditCheckService.submitAuditCheck(
				companyId, authorizerAppid, wxaAppId, trimmedTemplateKey, wxaName);
		Map<String, Object> templateData =
				superadminWxappTemplateMetadataService.resolveTemplateRow(trimmedTemplateKey);
		Map<String, Object> mergedOptions = mergeYykWindowIfNeeded(trimmedTemplateKey, options);
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> domainMap = (Map<String, Object>) templateData.get("domain");
			codeClient.modifyDomain(wxaAppId, domainMap == null ? Map.of() : domainMap);
		} catch (Exception e) {
			log.error("modify_domain 调用失败: wxaAppId={}", wxaAppId, e);
			String m = e.getMessage();
			String msg = (m != null && !m.isBlank()) ? m.trim() : "域名配置失败";
			throw new BadRequestException(msg, 400);
		}
		commitTemplate(companyId, wxaAppId, templateData, wxaName, mergedOptions);
		Map<String, Object> save = new LinkedHashMap<>();
		save.put("company_id", companyId);
		save.put("operator_id", operatorId);
		save.put("template_id", String.valueOf(templateData.get("template_id")));
		save.put("template_name", trimmedTemplateKey);
		save.put("template_ver", String.valueOf(templateData.get("version")));
		save.put("audit_status", 3);
		save.put("release_status", 0);
		save.put("visitstatus", 1);
		save.put("reason", "");
		save.put("audit_time", String.valueOf(Instant.now().getEpochSecond()));
		weappCreateOrUpdateService.saveWeapp(wxaAppId, save);
	}

	private Map<String, Object> mergeYykWindowIfNeeded(String normalizedTemplateKey, Map<String, Object> options) {
		Map<String, Object> copy = new LinkedHashMap<>();
		if (options != null) {
			copy.putAll(options);
		}
		if ("yykweishop".equals(normalizedTemplateKey)) {
			if (!(copy.get("window") instanceof Map<?, ?>)) {
				Map<String, Object> window = new LinkedHashMap<>();
				window.put("navigationBarTitleText", "商城");
				window.put("navigationBarTextStyle", "black");
				window.put("navigationBarBackgroundColor", "#ffffff");
				copy.put("window", window);
			}
		}
		return copy;
	}

	private void commitTemplate(
			long companyId,
			String wxaAppId,
			Map<String, Object> templateData,
			String wxaName,
			Map<String, Object> options) {
		Map<String, Object> ext = new LinkedHashMap<>();
		if (options != null) {
			ext.putAll(options);
		}
		Map<String, Object> extRoot = new LinkedHashMap<>();
		extRoot.put("ext", ext);
		extRoot.put("extAppid", wxaAppId);
		extRoot.put("extEnable", true);
		extRoot.put("directCommit", false);
		Map<String, Object> ys = youshuSettingInfoPort.getInfo(companyId);
		String youshuTok = "";
		if (ys != null) {
			String a = ys.get("app_id") == null ? "" : String.valueOf(ys.get("app_id")).trim();
			if (StringUtils.hasText(a)) {
				youshuTok = a;
			} else {
				String s = ys.get("sandbox_app_id") == null ? "" : String.valueOf(ys.get("sandbox_app_id")).trim();
				youshuTok = s;
			}
		}
		if (StringUtils.hasText(youshuTok)) {
			extRoot.put("youshutoken", youshuTok);
		}
		extRoot.put("requiredPrivateInfos", REQUIRED_PRIVATE_INFOS);
		if (companyLiveRoomEnabledPort.isLiveRoomEnabled(companyId)) {
			Map<String, Object> plugins = new LinkedHashMap<>();
			Map<String, Object> live = new LinkedHashMap<>();
			if (StringUtils.hasText(livePlayerPluginProperties.getVersion())) {
				live.put("version", livePlayerPluginProperties.getVersion());
			}
			if (StringUtils.hasText(livePlayerPluginProperties.getProvider())) {
				live.put("provider", livePlayerPluginProperties.getProvider());
			}
			if (!live.isEmpty()) {
				plugins.put("live-player-plugin", live);
				extRoot.put("plugins", plugins);
			}
		}
		String extJson;
		try {
			extJson = objectMapper.writeValueAsString(extRoot);
		} catch (Exception e) {
			throw new BadRequestException("参数序列化失败");
		}
		String tplId = String.valueOf(templateData.get("template_id"));
		String userVer = String.valueOf(templateData.get("version"));
		String userDesc = String.valueOf(templateData.get("desc"));
		codeClient.commit(wxaAppId, tplId, extJson, userVer, userDesc);
	}

	private static Map<String, Object> buildSubmitAuditPayload(
			Map<String, Object> templateData, String wxaName, JsonNode categoryFirst) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("address", "pages/index");
		item.put("tag", String.valueOf(templateData.getOrDefault("tag", "")));
		String title = wxaName == null || !StringUtils.hasText(wxaName.trim()) ? "小程序" : wxaName.trim();
		item.put("title", title);
		copyCategoryFields(item, categoryFirst);
		List<Map<String, Object>> itemList = new ArrayList<>();
		itemList.add(item);
		Map<String, Object> submitAuditData = new LinkedHashMap<>();
		submitAuditData.put("item_list", itemList);
		submitAuditData.put("order_path", "/subpage/pages/trade/list");
		return submitAuditData;
	}

	private static void copyCategoryFields(Map<String, Object> item, JsonNode cat) {
		if (cat == null || cat.isMissingNode()) {
			return;
		}
		String[] textKeys = {"first_class", "second_class", "third_class"};
		for (String k : textKeys) {
			if (cat.hasNonNull(k)) {
				item.put(k, cat.get(k).asText());
			}
		}
		String[] idKeys = {"first_id", "second_id", "third_id"};
		for (String k : idKeys) {
			if (cat.has(k) && !cat.get(k).isNull()) {
				JsonNode n = cat.get(k);
				if (n.isIntegralNumber()) {
					item.put(k, n.asInt());
				} else if (n.isNumber()) {
					item.put(k, n.asInt());
				} else {
					item.put(k, n.asText());
				}
			}
		}
	}
}
