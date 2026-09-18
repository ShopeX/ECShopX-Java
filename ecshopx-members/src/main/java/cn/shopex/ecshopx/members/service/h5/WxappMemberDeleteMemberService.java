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

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.h5.H5WxappMemberDeleteDisablePromoterPort;
import cn.shopex.ecshopx.common.members.h5.H5WxappMemberDeleteIncompleteOrdersPort;
import cn.shopex.ecshopx.common.members.h5.H5WxappMemberDeleteVipGradeRelPort;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAddress;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.MembersDeleteRecord;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.domain.WechatUsers;
import cn.shopex.ecshopx.members.mapper.MembersAddressMapper;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformMemberUnbindPort;
import cn.shopex.ecshopx.thirdparty.service.shuyun.ShuyunMemberUnbindPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxappMemberDeleteMemberService {

	private static final String LOGOUT_BLOCKED_DEFAULT =
			"订单完成之前，无法注销会员。如有疑问，请联系客服";

	@Value("${common.oem-shuyun:false}")
	private boolean oemShuyun;

	private final ShopProtocolSetService shopProtocolSetService;

	private final MembersMapper membersMapper;

	private final MembersAssociationsMapper membersAssociationsMapper;

	private final WechatUsersMapper wechatUsersMapper;

	private final MembersAddressMapper membersAddressMapper;

	private final MembersInfoMapper membersInfoMapper;

	private final MembersDeleteRecordMapper membersDeleteRecordMapper;

	private final CompanysMapper companysMapper;

	private final H5WxappMemberDeleteIncompleteOrdersPort incompleteOrdersPort;

	private final H5WxappMemberDeleteDisablePromoterPort disablePromoterPort;

	private final H5WxappMemberDeleteVipGradeRelPort deleteVipGradeRelPort;

	private final ShuyunMemberUnbindPort shuyunMemberUnbindPort;

	private final ShuyunOpenPlatformMemberUnbindPort openPlatformMemberUnbindPort;

	private final ObjectProvider<WxappMemberDeleteMemberService> selfProvider;

	public Map<String, Object> deleteMember(
			long companyId,
			long userId,
			String mobilePlain,
			boolean isDeleteConfirmed,
			String protocolLang) {
		boolean checkOk = checkDeleteMembers(companyId, userId);
		if (!checkOk) {
			LinkedHashMap<String, Object> data = new LinkedHashMap<>();
			data.put("status", Boolean.FALSE);
			data.put("msg", buildLogoutBlockedMessage(companyId, protocolLang));
			return data;
		}
		if (!isDeleteConfirmed) {
			LinkedHashMap<String, Object> data = new LinkedHashMap<>();
			data.put("status", Boolean.TRUE);
			data.put("msg", DataMasking.maskUname(mobilePlain));
			return data;
		}
		selfProvider.getObject().deleteMembersInTx(companyId, userId, mobilePlain);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		data.put("msg", "注销成功");
		return data;
	}

	public boolean checkDeleteMembers(long companyId, long userId) {
		Members m =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, userId)
								.last("LIMIT 1"));
		if (m == null) {
			return false;
		}
		if (incompleteOrdersPort.hasIncompleteNormalOrders(companyId, userId)) {
			return false;
		}
		return true;
	}

	private String buildLogoutBlockedMessage(long companyId, String protocolLang) {
		Map<String, Object> proto = shopProtocolSetService.get(companyId, "member_logout_config", protocolLang);
		Object block = proto.get("member_logout_config");
		String title = "";
		if (block instanceof Map<?, ?> m) {
			Object t = m.get("title");
			title = t == null ? "" : String.valueOf(t).trim();
		}
		if (StringUtils.hasText(title)) {
			return title;
		}
		return LOGOUT_BLOCKED_DEFAULT;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteMembersInTx(long companyId, long userId, String mobilePlain) {
		try {
			if (openPlatformMemberUnbindPort.isOpenPlatformMemberEnabled(companyId)) {
				openPlatformMemberUnbindPort.unbindIfNeeded(companyId, userId, false);
			} else if (oemShuyun) {
				String shopId = "";
				Companys c = companysMapper.selectById(companyId);
				if (c != null && StringUtils.hasText(c.getPassportUid())) {
					shopId = c.getPassportUid().trim();
				}
				shuyunMemberUnbindPort.tryUnbind(companyId, userId, shopId);
			}

			List<MembersAssociations> assocs =
					membersAssociationsMapper.selectList(
							new LambdaQueryWrapper<MembersAssociations>()
									.eq(MembersAssociations::getCompanyId, companyId)
									.eq(MembersAssociations::getUserId, userId));
			for (MembersAssociations a : assocs) {
				String unionid = a.getUnionid();
				if (StringUtils.hasText(unionid)) {
					wechatUsersMapper.delete(
							new LambdaQueryWrapper<WechatUsers>()
									.eq(WechatUsers::getCompanyId, companyId)
									.eq(WechatUsers::getUnionid, unionid.trim()));
				}
			}
			membersAssociationsMapper.delete(
					new LambdaQueryWrapper<MembersAssociations>()
							.eq(MembersAssociations::getCompanyId, companyId)
							.eq(MembersAssociations::getUserId, userId));

			membersAddressMapper.delete(
					new LambdaQueryWrapper<MembersAddress>()
							.eq(MembersAddress::getCompanyId, companyId)
							.eq(MembersAddress::getUserId, userId));

			membersInfoMapper.delete(
					new LambdaQueryWrapper<MembersInfo>()
							.eq(MembersInfo::getCompanyId, companyId)
							.eq(MembersInfo::getUserId, userId));

			membersMapper.delete(
					new LambdaQueryWrapper<Members>()
							.eq(Members::getCompanyId, companyId)
							.eq(Members::getUserId, userId));

			disablePromoterPort.disablePromoter(companyId, userId);
			deleteVipGradeRelPort.deleteVipGradeRelForMember(companyId, userId);

			int now = (int) (System.currentTimeMillis() / 1000L);
			MembersDeleteRecord rec = new MembersDeleteRecord();
			rec.setCompanyId(companyId);
			rec.setUserId(userId);
			rec.setMobile(LegacyFixedMobileEncrypt.fixedEncryptMobile(mobilePlain));
			rec.setCreated(now);
			rec.setUpdated(now);
			membersDeleteRecordMapper.insert(rec);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.error("member delete tx failed companyId={} userId={}", companyId, userId, e);
			throw new ResourceException("注销失败");
		}
	}
}
