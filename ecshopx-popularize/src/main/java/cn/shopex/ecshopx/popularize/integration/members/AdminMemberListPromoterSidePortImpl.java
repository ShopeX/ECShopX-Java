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

package cn.shopex.ecshopx.popularize.integration.members;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.members.integration.popularize.AdminMemberListPromoterSidePort;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.admin.MembersContactByUserIdsLookupService;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service("adminMemberListPromoterSidePortImpl")
public class AdminMemberListPromoterSidePortImpl implements AdminMemberListPromoterSidePort {

	private final PromoterMapper promoterMapper;
	private final MembersMapper membersMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public AdminMemberListPromoterSidePortImpl(
			PromoterMapper promoterMapper,
			MembersMapper membersMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.promoterMapper = promoterMapper;
		this.membersMapper = membersMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	@Override
	public List<Long> listDownstreamUserIdsByPromoterMobileEnc(long companyId, String mobileEnc) {
		List<Long> ids = promoterMapper.selectPromoterDownstreamUserIdsForAdminMemberList(companyId, mobileEnc);
		return ids == null ? List.of() : ids;
	}

	@Override
	public void applyOemShuyunExtensions(long companyId, List<Map<String, Object>> memberRows, List<Long> pageUserIds) {
		if (memberRows == null || memberRows.isEmpty() || pageUserIds == null || pageUserIds.isEmpty()) {
			return;
		}
		Map<Long, Boolean> canChange = loadCanChangePid(companyId, pageUserIds);
		for (Map<String, Object> row : memberRows) {
			long uid = userIdLong(row.get("user_id"));
			row.put("is_can_changepid", Boolean.TRUE.equals(canChange.get(uid)));
			Map<String, Object> info = resolvePromoterParentInfo(companyId, uid);
			if (info != null && !info.isEmpty()) {
				row.put("promoter_info", info);
			}
		}
	}

	private Map<Long, Boolean> loadCanChangePid(long companyId, List<Long> userIds) {
		List<Promoter> rows =
				promoterMapper.selectList(
						new LambdaQueryWrapper<Promoter>()
								.eq(Promoter::getCompanyId, companyId)
								.in(Promoter::getUserId, userIds)
								.eq(Promoter::getIsPromoter, 0)
								.gt(Promoter::getPid, 0L)
								.eq(Promoter::getDisabled, 0));
		Map<Long, Boolean> m = new LinkedHashMap<>();
		for (Promoter p : rows) {
			if (p.getUserId() != null) {
				m.put(p.getUserId(), Boolean.TRUE);
			}
		}
		return m;
	}

	private Map<String, Object> resolvePromoterParentInfo(long companyId, long memberUserId) {
		Promoter self =
				promoterMapper.selectOne(
						new LambdaQueryWrapper<Promoter>()
								.eq(Promoter::getCompanyId, companyId)
								.eq(Promoter::getUserId, memberUserId)
								.eq(Promoter::getDisabled, 0)
								.last("LIMIT 1"));
		if (self == null || self.getPid() == null || self.getPid() <= 0L) {
			return null;
		}
		Promoter parent = promoterMapper.selectById(self.getPid());
		if (parent == null || parent.getUserId() == null) {
			return null;
		}
		MembersContactByUserIdsLookupService.MemberContactRow mr =
				membersMapper
						.selectInviterMobileRowsByUserIds(companyId, List.of(parent.getUserId()))
						.stream()
						.findFirst()
						.orElse(null);
		String pmobile = "";
		if (mr != null && mr.getMobileEnc() != null) {
			pmobile = sensitiveFieldEncryptor.decrypt(mr.getMobileEnc());
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("promoter_id", parent.getId());
		out.put("promoter_name", parent.getPromoterName() == null ? "" : parent.getPromoterName());
		out.put("promoter_mobile", pmobile);
		out.put("promoter_identity", "");
		out.put("is_subordinates", parent.getIsSubordinates());
		return out;
	}

	private static long userIdLong(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
