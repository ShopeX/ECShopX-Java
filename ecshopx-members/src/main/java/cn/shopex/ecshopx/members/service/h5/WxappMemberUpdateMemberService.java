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

package cn.shopex.ecshopx.members.service.h5;

import cn.shopex.ecshopx.common.dispatch.MembersUpdateMemberSuccessDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.integration.front.FrontMemberInfoUpdateValidationPort;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmUpdateMemberInfoByMobilePort;
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformMemberModifyPort;
import cn.shopex.ecshopx.thirdparty.service.shuyun.ShuyunMemberProfileModifyPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxappMemberUpdateMemberService {

	@Value("${common.oem-shuyun:false}")
	private boolean oemShuyun;

	private final FrontMemberInfoUpdateValidationPort frontMemberInfoUpdateValidationPort;

	private final MemberAccountService memberAccountService;

	private final MembersInfoMapper membersInfoMapper;

	private final MembersUpdateMemberSuccessDispatchPublisher membersUpdateMemberSuccessDispatchPublisher;

	private final ShuyunMemberProfileModifyPort shuyunMemberProfileModifyPort;

	private final ShuyunOpenPlatformMemberModifyPort openPlatformMemberModifyPort;

	private final DmCrmSettingReadPort dmCrmSettingReadPort;

	private final DmCrmUpdateMemberInfoByMobilePort dmCrmUpdateMemberInfoByMobilePort;

	private final CompanysMapper companysMapper;

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateMember(
			long companyId,
			long userId,
			Map<String, Object> authClaims,
			Map<String, Object> postData,
			String country,
			String province,
			String city,
			String language,
			boolean isGetWxInfo,
			String acceptLanguage) {
		stripMaskedFields(postData);
		postData.remove("company_id");
		postData.remove("mobile");
		if (postData.isEmpty()) {
			throw new ResourceException("请填写数据!");
		}

		LinkedHashMap<String, Map<String, Object>> requestFields =
				frontMemberInfoUpdateValidationPort.prepareAndValidate(companyId, postData, acceptLanguage);

		LinkedHashMap<String, Object> customData = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, Object>> e : requestFields.entrySet()) {
			String key = e.getKey();
			Map<String, Object> cfg = e.getValue();
			int isDef = int01(cfg.get("is_default"));
			if (isDef != 0) {
				continue;
			}
			if (!postData.containsKey(key)) {
				continue;
			}
			customData.put(key, postData.get(key));
		}

		LinkedHashMap<String, Object> otherParams = new LinkedHashMap<>();
		Object customDataPayload = customData.isEmpty() ? new ArrayList<Object>() : customData;
		otherParams.put("custom_data", customDataPayload);
		otherParams.put("isGetWxInfo", isGetWxInfo);
		postData.put("other_params", otherParams);

		// 开放网关：远端 member.modify 成功后再写本地（A-MOD-01）
		if (openPlatformMemberModifyPort.isOpenPlatformMemberEnabled(companyId)) {
			openPlatformMemberModifyPort.modifyIfNeeded(companyId, userId, postData);
		}

		memberAccountService.memberInfoUpdate(userId, companyId, postData);

		MembersInfo fresh =
				membersInfoMapper.selectOne(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.eq(MembersInfo::getUserId, userId)
								.last("LIMIT 1"));
		if (fresh == null) {
			throw new ResourceException("未查询到更新数据");
		}
		LinkedHashMap<String, Object> dispatchSnapshot =
				new LinkedHashMap<>(memberAccountService.toMembersInfoApiMap(fresh, false));
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new BadRequestException("updateMember requires active transaction synchronization");
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				membersUpdateMemberSuccessDispatchPublisher.publish(new LinkedHashMap<>(dispatchSnapshot));
			}
		});

		if (authClaims != null
				&& StringUtils.hasText(stringVal(authClaims.get("unionid")))
				&& StringUtils.hasText(stringVal(authClaims.get("open_id")))) {
			LinkedHashMap<String, Object> wechatPatch = new LinkedHashMap<>();
			if (postData.containsKey("avatar")) {
				wechatPatch.put("headimgurl", postData.get("avatar"));
			}
			if (postData.containsKey("username")) {
				wechatPatch.put("nickname", postData.get("username"));
			}
			memberAccountService.updateWechatUserProfileByOpenUnion(
					companyId, authClaims, wechatPatch, country, province, city, language);
		}

		if (oemShuyun && !openPlatformMemberModifyPort.isOpenPlatformMemberEnabled(companyId)) {
			tryShuyunProfileModify(companyId, userId, postData);
		}

		Map<String, Object> result = memberAccountService.getMemberInfo(userId, companyId);

		if (dmCrmSettingReadPort.isPointIntegrationOpen(companyId)) {
			LinkedHashMap<String, Object> dm = new LinkedHashMap<>();
			dm.put("mobile", stringVal(authClaims != null ? authClaims.get("mobile") : null));
			dm.put("name", result.get("username"));
			dm.put("sex", result.get("sex"));
			dm.put("birthday", result.get("birthday"));
			dm.put("email", result.get("email"));
			dmCrmUpdateMemberInfoByMobilePort.updateMemberInfoByMobile(companyId, dm);
		}

		return result;
	}

	private void tryShuyunProfileModify(long companyId, long userId, Map<String, Object> postData) {
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		if (postData.containsKey("username")) {
			body.put("nick", String.valueOf(postData.get("username")));
		}
		if (postData.containsKey("birthday")) {
			Object b = postData.get("birthday");
			if (b != null && StringUtils.hasText(String.valueOf(b).trim())) {
				body.put("birthday", String.valueOf(b).trim());
			}
		}
		if (postData.containsKey("sex")) {
			String sexStr = String.valueOf(postData.get("sex")).trim();
			if ("1".equals(sexStr) || "2".equals(sexStr)) {
				body.put("gender", "1".equals(sexStr) ? "M" : "F");
			}
		}
		if (body.isEmpty()) {
			return;
		}
		Map<String, Object> assoc = memberAccountService.getMembersAssociationByUserId(companyId, "wechat", userId);
		if (assoc.isEmpty() || !StringUtils.hasText(stringVal(assoc.get("unionid")))) {
			return;
		}
		body.put("unionid", stringVal(assoc.get("unionid")).trim());
		String shopId = "";
		Companys c = companysMapper.selectById(companyId);
		if (c != null && StringUtils.hasText(c.getPassportUid())) {
			shopId = c.getPassportUid().trim();
		}
		boolean ok = shuyunMemberProfileModifyPort.modifyMemberProfile(companyId, userId, shopId, body);
		if (!ok) {
			log.debug("shuyun member profile modify returned false companyId={} userId={}", companyId, userId);
		}
	}

	private static void stripMaskedFields(Map<String, Object> postData) {
		if (postData == null || postData.isEmpty()) {
			return;
		}
		Object birthday = postData.get("birthday");
		if (birthday != null && String.valueOf(birthday).contains("****-**-*")) {
			postData.remove("birthday");
		}
		Object address = postData.get("address");
		if (address != null && String.valueOf(address).contains("******")) {
			postData.remove("address");
		}
		Object sex = postData.get("sex");
		if (sex != null) {
			String s = String.valueOf(sex);
			if (s.contains("*") || s.contains("-")) {
				postData.remove("sex");
			}
		}
	}

	private static int int01(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (v instanceof Number n) {
			return n.intValue() == 0 ? 0 : 1;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty() || "0".equals(s) || "false".equalsIgnoreCase(s)) {
			return 0;
		}
		return 1;
	}

	private static String stringVal(Object o) {
		return o == null ? "" : Objects.toString(o, "").trim();
	}
}
