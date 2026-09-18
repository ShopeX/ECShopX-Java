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

package cn.shopex.ecshopx.youshu.service;

import cn.shopex.ecshopx.common.cron.youshu.YoushuDataSourceApiPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuOpenApiCredentials;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.youshu.domain.YoushuSetting;
import cn.shopex.ecshopx.youshu.integration.YoushuMemberPushPort;
import cn.shopex.ecshopx.youshu.mapper.YoushuSettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class YoushuMemberCreateSuccessSyncService {

	private static final int DATA_SOURCE_TYPE_MEMBER = 0;

	private final YoushuSettingMapper youshuSettingMapper;
	private final YoushuDataSourceApiPort youshuDataSourceApiPort;
	private final YoushuMemberPushPort youshuMemberPushPort;
	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;

	public YoushuMemberCreateSuccessSyncService(
			YoushuSettingMapper youshuSettingMapper,
			YoushuDataSourceApiPort youshuDataSourceApiPort,
			YoushuMemberPushPort youshuMemberPushPort,
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper) {
		this.youshuSettingMapper = youshuSettingMapper;
		this.youshuDataSourceApiPort = youshuDataSourceApiPort;
		this.youshuMemberPushPort = youshuMemberPushPort;
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
	}

	public void syncMemberAfterCreate(long companyId, long userId) {
		YoushuSetting setting =
				youshuSettingMapper.selectOne(
						new LambdaQueryWrapper<YoushuSetting>()
								.eq(YoushuSetting::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (setting == null) {
			return;
		}
		String merchantId = setting.getMerchantId();
		if (!StringUtils.hasText(merchantId)) {
			return;
		}

		Members member =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, userId)
								.last("LIMIT 1"));
		if (member == null) {
			return;
		}

		MembersInfo info =
				membersInfoMapper.selectOne(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.eq(MembersInfo::getUserId, userId)
								.last("LIMIT 1"));

		YoushuOpenApiCredentials credentials = toCredentials(setting);
		String dataSourceId =
				youshuDataSourceApiPort.getOrCreateDataSourceId(
						merchantId.trim(), DATA_SOURCE_TYPE_MEMBER, credentials);

		Map<String, Object> row = buildMemberRow(member, info);
		youshuMemberPushPort.pushMemberRow(dataSourceId, row, credentials);
	}

	private static Map<String, Object> buildMemberRow(Members m, MembersInfo info) {
		Map<String, Object> row = new LinkedHashMap<>();
		long uid = m.getUserId() == null ? 0L : m.getUserId();
		row.put("company_id", m.getCompanyId());
		row.put("user_id", uid);
		row.put("object_id", uid);
		row.put("grade_id", m.getGradeId() == null ? 0L : m.getGradeId());
		String rawMobile = m.getMobile();
		String mobileOut = "";
		if (StringUtils.hasText(rawMobile)) {
			mobileOut = LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(rawMobile.trim());
		}
		row.put("mobile", mobileOut);
		row.put("wxa_appid", m.getWxaAppid() != null ? m.getWxaAppid() : "");
		row.put("created", m.getCreated() != null ? m.getCreated() : 0L);
		row.put("disabled", Boolean.TRUE.equals(m.getDisabled()) ? 1 : 0);
		if (info != null) {
			row.put("username", info.getUsername() != null ? info.getUsername() : "");
			row.put("avatar", info.getAvatar() != null ? info.getAvatar() : "");
			row.put("sex", info.getSex() != null ? info.getSex() : 0);
		} else {
			row.put("username", "");
			row.put("avatar", "");
			row.put("sex", 0);
		}
		return row;
	}

	private static YoushuOpenApiCredentials toCredentials(YoushuSetting v) {
		String base = firstNonBlank(v.getApiUrl(), v.getSandboxApiUrl());
		String appId = firstNonBlank(v.getAppId(), v.getSandboxAppId());
		String appSecret = firstNonBlank(v.getAppSecret(), v.getSandboxAppSecret());
		return new YoushuOpenApiCredentials(base, appId, appSecret);
	}

	private static String firstNonBlank(String primary, String fallback) {
		if (primary != null && !primary.isBlank()) {
			return primary;
		}
		if (fallback != null && !fallback.isBlank()) {
			return fallback;
		}
		return "";
	}
}
