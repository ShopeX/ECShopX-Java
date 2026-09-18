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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.members.admin.MemberUnionidByUserIdLookupPort;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOpenApiSignedFormClient;
import cn.shopex.ecshopx.workwechat.service.WorkWechatConfigService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Aligns PHP {@code Distributor@getDistributorSalespersonQrcodeSingle}:
 * {@code GET /api/v1/h5app/wxapp/distributor/salesperson/qrcode}.
 */
@Service
public class DistributorH5GetSalespersonQrcodeService {

	private static final Logger log = LoggerFactory.getLogger(DistributorH5GetSalespersonQrcodeService.class);

	private static final String MC_METHOD = "members.store.salesperson.qrcode.single";

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;
	private final WorkWechatConfigService workWechatConfigService;
	private final MemberUnionidByUserIdLookupPort memberUnionidByUserIdLookupPort;

	public DistributorH5GetSalespersonQrcodeService(
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient,
			WorkWechatConfigService workWechatConfigService,
			MemberUnionidByUserIdLookupPort memberUnionidByUserIdLookupPort) {
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
		this.workWechatConfigService = workWechatConfigService;
		this.memberUnionidByUserIdLookupPort = memberUnionidByUserIdLookupPort;
	}

	public Map<String, Object> getSalespersonQrcode(
			long companyId, long distributorId, long userId, String authUnionid) {
		String bgAvatarUrl = "";
		String defaultSalespersonAvatar = "";
		try {
			Map<String, Object> workWechatConfig = workWechatConfigService.getViewConfig(companyId);
			bgAvatarUrl = stringOrEmpty(workWechatConfig.get("bg_avatar_url"));
			defaultSalespersonAvatar = stringOrEmpty(workWechatConfig.get("avatar_url"));
		} catch (Exception e) {
			log.warn(
					"获取企业微信配置失败 company_id={} error={}",
					companyId,
					e.getMessage());
		}

		Map<String, Object> distributorInfo =
				distributorRepositoryGetInfoSimpleService.getInfoSimpleByDistributorId(companyId, distributorId);
		String storeBn = stringOrEmpty(distributorInfo.get("shop_code"));
		if (!StringUtils.hasText(storeBn)) {
			return emptyResult(bgAvatarUrl);
		}

		String unionid = authUnionid == null ? "" : authUnionid.trim();
		if (userId > 0L && !StringUtils.hasText(unionid)) {
			unionid = memberUnionidByUserIdLookupPort
					.findUnionidByUserId(companyId, userId)
					.orElse("");
		}

		Map<String, Object> data = emptyResult(bgAvatarUrl);
		try {
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("store_bn", storeBn);
			payload.put("unionid", unionid);
			Map<String, Object> mcData =
					marketingCenterOpenApiSignedFormClient.postReturningParsedData(companyId, MC_METHOD, payload);
			if (mcData != null && !mcData.isEmpty()) {
				for (Map.Entry<String, Object> e : mcData.entrySet()) {
					if (e.getKey() != null) {
						data.put(e.getKey(), e.getValue());
					}
				}
				data.put("bg_avatar_url", bgAvatarUrl);
			}
		} catch (Exception e) {
			log.warn(
					"获取单门店导购二维码失败 company_id={} user_id={} distributor_id={} error={}",
					companyId,
					userId,
					distributorId,
					e.getMessage());
		}

		if (StringUtils.hasText(stringOrEmpty(data.get("work_qrcode")))
				&& !StringUtils.hasText(stringOrEmpty(data.get("salesperson_avatar")))) {
			data.put("salesperson_avatar", defaultSalespersonAvatar);
			data.put("salesperson_common_name", "客服");
		}
		return data;
	}

	private static Map<String, Object> emptyResult(String bgAvatarUrl) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("work_qrcode", "");
		data.put("work_qrcode_configid", "");
		data.put("salesperson_name", "");
		data.put("salesperson_avatar", "");
		data.put("salesperson_common_name", "");
		data.put("bg_avatar_url", bgAvatarUrl == null ? "" : bgAvatarUrl);
		return data;
	}

	private static String stringOrEmpty(Object v) {
		if (v == null) {
			return "";
		}
		String s = String.valueOf(v).trim();
		return "null".equalsIgnoreCase(s) ? "" : s;
	}
}
