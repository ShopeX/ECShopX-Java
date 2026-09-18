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

package cn.shopex.ecshopx.members.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.wechat.WorkWechatRelMemberBatchPort;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.domain.WechatUsers;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class WorkWechatRelMemberBatchPortImpl implements WorkWechatRelMemberBatchPort {

	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final WechatUsersMapper wechatUsersMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public WorkWechatRelMemberBatchPortImpl(MembersMapper membersMapper, MembersInfoMapper membersInfoMapper,
			MembersAssociationsMapper membersAssociationsMapper, WechatUsersMapper wechatUsersMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.wechatUsersMapper = wechatUsersMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	@Override
	public Map<Long, Map<String, Object>> loadByUserIdsForWorkWechatRel(Collection<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<Long> unique = new LinkedHashSet<>();
		for (Long uid : userIds) {
			if (uid != null) {
				unique.add(uid);
			}
		}
		if (unique.isEmpty()) {
			return Collections.emptyMap();
		}

		LambdaQueryWrapper<Members> mw = new LambdaQueryWrapper<>();
		mw.in(Members::getUserId, unique).orderByDesc(Members::getUserId).last("LIMIT 100");
		List<Members> memberRows = membersMapper.selectList(mw);
		if (memberRows.isEmpty()) {
			return Collections.emptyMap();
		}

		List<Long> loadedUserIds = new ArrayList<>();
		for (Members m : memberRows) {
			if (m.getUserId() != null) {
				loadedUserIds.add(m.getUserId());
			}
		}

		Map<Long, MembersInfo> infoByUserId = loadMembersInfo(loadedUserIds);
		Map<Long, OpenIdRow> openRows = loadOpenIdByUserIds(loadedUserIds);

		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Members m : memberRows) {
			Long uid = m.getUserId();
			if (uid == null) {
				continue;
			}
			MembersInfo info = infoByUserId.get(uid);
			OpenIdRow oid = openRows.get(uid);
			String unionid = oid == null ? "" : oid.unionid();
			String openId = oid == null ? "" : oid.openId();
			Map<String, Object> row = buildMemberRow(m, info, unionid, openId);
			out.put(uid, row);
		}
		return out;
	}

	private Map<Long, MembersInfo> loadMembersInfo(List<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		LambdaQueryWrapper<MembersInfo> iw = new LambdaQueryWrapper<>();
		iw.in(MembersInfo::getUserId, userIds);
		List<MembersInfo> infos = membersInfoMapper.selectList(iw);
		Map<Long, MembersInfo> map = new LinkedHashMap<>();
		for (MembersInfo info : infos) {
			if (info.getUserId() != null) {
				map.put(info.getUserId(), info);
			}
		}
		return map;
	}

	private record OpenIdRow(String unionid, String openId) {
	}

	/**
	 * 对齐无 company_id 条件时的关联与微信用户查询。
	 */
	private Map<Long, OpenIdRow> loadOpenIdByUserIds(List<Long> userIdList) {
		if (userIdList.isEmpty()) {
			return Collections.emptyMap();
		}
		LambdaQueryWrapper<MembersAssociations> aw = new LambdaQueryWrapper<>();
		aw.in(MembersAssociations::getUserId, userIdList);
		List<MembersAssociations> assocRows = membersAssociationsMapper.selectList(aw);
		if (assocRows.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<String> unionIds = new LinkedHashSet<>();
		for (MembersAssociations a : assocRows) {
			if (a.getUnionid() != null && !a.getUnionid().isEmpty()) {
				unionIds.add(a.getUnionid());
			}
		}
		if (unionIds.isEmpty()) {
			return Collections.emptyMap();
		}
		LambdaQueryWrapper<WechatUsers> ww = new LambdaQueryWrapper<>();
		ww.in(WechatUsers::getUnionid, unionIds);
		List<WechatUsers> wuList = wechatUsersMapper.selectList(ww);
		Map<String, String> openIdByUnionid = new LinkedHashMap<>();
		for (WechatUsers wu : wuList) {
			if (wu.getUnionid() != null) {
				String oi = wu.getOpenId() == null ? "" : wu.getOpenId();
				openIdByUnionid.put(wu.getUnionid(), oi);
			}
		}
		Map<Long, OpenIdRow> idIndex = new LinkedHashMap<>();
		for (MembersAssociations a : assocRows) {
			if (a.getUserId() == null) {
				continue;
			}
			String u = a.getUnionid() == null ? "" : a.getUnionid();
			String openId = u.isEmpty() ? "" : openIdByUnionid.getOrDefault(u, "");
			idIndex.put(a.getUserId(), new OpenIdRow(u, openId));
		}
		return idIndex;
	}

	private Map<String, Object> buildMemberRow(Members m, MembersInfo info, String unionid, String openId) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("user_id", m.getUserId());
		row.put("company_id", m.getCompanyId());
		row.put("grade_id", m.getGradeId());
		String mobileEnc = m.getMobile();
		row.put("mobile", mobileEnc == null ? "" : sensitiveFieldEncryptor.decrypt(mobileEnc));
		row.put("user_card_code", m.getUserCardCode());
		row.put("authorizer_appid", m.getAuthorizerAppid());
		row.put("wxa_appid", m.getWxaAppid());
		row.put("source_id", m.getSourceId());
		row.put("monitor_id", m.getMonitorId());
		row.put("latest_source_id", m.getLatestSourceId());
		row.put("latest_monitor_id", m.getLatestMonitorId());
		row.put("created", m.getCreated());
		row.put("updated", m.getUpdated());
		row.put("created_year", m.getCreatedYear());
		row.put("created_month", m.getCreatedMonth());
		row.put("created_day", m.getCreatedDay());
		row.put("offline_card_code", m.getOfflineCardCode());
		row.put("inviter_id", m.getInviterId());
		row.put("source_from", m.getSourceFrom());
		row.put("password", m.getPassword());
		row.put("disabled", m.getDisabled());
		row.put("use_point", m.getUsePoint());
		row.put("remarks", m.getRemarks());
		row.put("third_data", m.getThirdData());
		row.put("region_mobile", m.getRegionMobile());
		row.put("mobile_country_code", m.getMobileCountryCode());
		row.put("reg_distributor", m.getRegDistributor());
		row.put("reg_salesperson", m.getRegSalesperson());
		row.put("fp_salesperson", m.getFpSalesperson());
		row.put("has_fp", m.getHasFp());
		row.put("op_distributor", m.getOpDistributor());

		if (info != null) {
			String usernameEnc = info.getUsername();
			row.put("username", usernameEnc == null ? "" : sensitiveFieldEncryptor.decrypt(usernameEnc));
			row.put("name", info.getName());
			row.put("sex", info.getSex());
			row.put("birthday", info.getBirthday());
			row.put("address", info.getAddress());
			row.put("email", info.getEmail());
			row.put("industry", info.getIndustry());
			row.put("income", info.getIncome());
			row.put("edu_background", info.getEduBackground());
			row.put("habbit", info.getHabbit());
			row.put("avatar", info.getAvatar());
		} else {
			row.put("username", "");
			row.put("name", null);
			row.put("sex", 0);
			row.put("birthday", null);
			row.put("address", null);
			row.put("email", null);
			row.put("industry", null);
			row.put("income", null);
			row.put("edu_background", null);
			row.put("habbit", null);
			row.put("avatar", null);
		}

		row.put("unionid", unionid == null ? "" : unionid);
		row.put("open_id", openId == null ? "" : openId);
		row.put("nickname", "");
		return row;
	}
}
