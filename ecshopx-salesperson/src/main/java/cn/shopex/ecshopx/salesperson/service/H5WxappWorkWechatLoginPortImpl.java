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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.companys.wxapp.H5WxappWorkWechatLoginPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOpenApiSignedFormClient;
import cn.shopex.ecshopx.workwechat.service.WorkWechatConfigService;
import cn.shopex.ecshopx.workwechat.service.WorkWechatGuideMiniProgramApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class H5WxappWorkWechatLoginPortImpl implements H5WxappWorkWechatLoginPort {

	private static final long SESSION_TTL_SECONDS = 604800L;

	private final WorkWechatConfigService workWechatConfigService;
	private final WorkWechatGuideMiniProgramApiService workWechatGuideMiniProgramApiService;
	private final ShopSalespersonWorkWechatWxappLoginMutationService mutationService;
	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;
	private final SalespersonGetInfoService salespersonGetInfoService;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;

	public H5WxappWorkWechatLoginPortImpl(
			WorkWechatConfigService workWechatConfigService,
			WorkWechatGuideMiniProgramApiService workWechatGuideMiniProgramApiService,
			ShopSalespersonWorkWechatWxappLoginMutationService mutationService,
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient,
			SalespersonGetInfoService salespersonGetInfoService,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.workWechatConfigService = workWechatConfigService;
		this.workWechatGuideMiniProgramApiService = workWechatGuideMiniProgramApiService;
		this.mutationService = mutationService;
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
		this.salespersonGetInfoService = salespersonGetInfoService;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public Map<String, Object> workwechatlogin(long companyId, String appname, String code) {
		if (!StringUtils.hasText(appname)) {
			throw new BadRequestException("缺少name参数，登录失败!");
		}
		workWechatConfigService.assertShowAllowsOrdinaryLogin(companyId);
		Map<String, Object> wx = workWechatGuideMiniProgramApiService.miniprogramJscode2session(companyId, code);
		String userid = String.valueOf(wx.get("userid")).trim();
		if (!StringUtils.hasText(userid)) {
			throw new BadRequestException("导购登陆解析失败！");
		}
		String sessionKey = String.valueOf(wx.get("session_key")).trim();
		if (!StringUtils.hasText(sessionKey)) {
			throw new BadRequestException("导购登陆解析失败！");
		}

		Optional<ShopSalesperson> firstOpt = mutationService.findFirstByWorkUseridOrWorkClearUserid(userid);
		if (firstOpt.isPresent()) {
			var row = firstOpt.get();
			if (StringUtils.hasText(row.getWorkUserid()) && !StringUtils.hasText(row.getWorkClearUserid())) {
				mutationService.updateWorkClearUseridBySalespersonId(row.getSalespersonId(), userid);
			}
		} else {
			Map<String, Object> ba = marketingCenterOpenApiSignedFormClient.basicsSalespersonUseridToOpenUserid(companyId,
					userid);
			if (ba.isEmpty()) {
				throw new BadRequestException("转换导购工号错误!");
			}
			String openUserid = ba.get("open_userid") == null ? "" : String.valueOf(ba.get("open_userid")).trim();
			if (!StringUtils.hasText(openUserid)) {
				throw new BadRequestException("转换导购工号错误!");
			}
			Optional<Long> spIdOpt = mutationService.findSalespersonIdByWorkUseridEqOpenUserid(openUserid);
			if (spIdOpt.isEmpty()) {
				throw new BadRequestException("没有您的导购信息!");
			}
			mutationService.updateWorkClearUseridBySalespersonId(spIdOpt.get(), userid);
		}

		Map<String, Object> detail = salespersonGetInfoService.getShoppingGuideDetailForWorkWechatLogin(companyId, userid);
		if (detail.isEmpty()) {
			throw new BadRequestException("当前账号无权限");
		}
		if (!detail.containsKey("store_name")) {
			throw new BadRequestException("请在后台为当前导购添加店铺");
		}
		Object storeNameObj = detail.get("store_name");
		String storeNameStr = storeNameObj == null ? "" : String.valueOf(storeNameObj).trim();
		if (!StringUtils.hasText(storeNameStr)) {
			throw new BadRequestException("请在后台为当前导购添加店铺");
		}

		if (wx.get("userid") != null && StringUtils.hasText(String.valueOf(wx.get("userid")).trim())) {
			String cfgId = trimToNull(detail.get("work_configid"));
			if (cfgId == null) {
				cfgId = workWechatGuideMiniProgramApiService.addContactWay(companyId, 1, 1, userid);
			}
			String qrId = trimToNull(detail.get("work_qrcode_configid"));
			if (qrId == null) {
				qrId = workWechatGuideMiniProgramApiService.addContactWay(companyId, 1, 2, userid);
			}
			long salespersonId = Long.parseLong(String.valueOf(detail.get("salesperson_id")).trim());
			mutationService.updateWorkConfigIdsBySalespersonId(salespersonId, cfgId, qrId);
		}

		LinkedHashMap<String, Object> sessionPayload = new LinkedHashMap<>();
		sessionPayload.put("open_id", userid);
		sessionPayload.put("session_key", sessionKey);
		sessionPayload.put("appname", appname);
		sessionPayload.put("phoneNumber", nullToEmpty(detail.get("mobile")));
		sessionPayload.put("company_id", String.valueOf(companyId));
		sessionPayload.put("salesperson_id", nullToEmpty(detail.get("salesperson_id")));
		sessionPayload.put("salesperson_name", nullToEmpty(detail.get("name")));
		sessionPayload.put("salesperson_type", nullToEmpty(detail.get("salesperson_type")));
		sessionPayload.put("work_userid", userid);
		try {
			String json = objectMapper.writeValueAsString(sessionPayload);
			sharedStringRedisTemplate.opsForValue()
					.set("frontSession3rd:" + sessionKey, json, Duration.ofSeconds(SESSION_TTL_SECONDS));
		} catch (Exception e) {
			throw new BadRequestException("导购登陆失败！");
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("session3rd", sessionKey);
		out.put("company_id", String.valueOf(companyId));
		out.put("phoneNumber", nullToEmpty(detail.get("mobile")));
		out.put("distributor_id", distributorIdForResponse(detail.get("distributor_id")));
		out.put("avatar", nullToEmpty(detail.get("avatar")));
		out.put("salesperson_name", nullToEmpty(detail.get("name")));
		out.put("salesperson_type", nullToEmpty(detail.get("salesperson_type")));
		out.put("salesperson_id", nullToEmpty(detail.get("salesperson_id")));
		out.put("store_name", storeNameStr);
		out.put("work_userid", userid);
		out.put("salesperson_job", nullToEmpty(detail.get("salesperson_job")));
		out.put("employee_status", String.valueOf(detail.get("employee_status")));
		out.put("shop_code", detail.get("shop_code") == null ? "" : String.valueOf(detail.get("shop_code")));
		return out;
	}

	private static String nullToEmpty(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static String trimToNull(Object v) {
		if (v == null) {
			return null;
		}
		String t = String.valueOf(v).trim();
		return t.isEmpty() || "0".equals(t) ? null : t;
	}

	private static String distributorIdForResponse(Object v) {
		if (v == null || Boolean.FALSE.equals(v)) {
			return String.valueOf(0L);
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return String.valueOf(0L);
		}
		return String.valueOf(v);
	}
}
