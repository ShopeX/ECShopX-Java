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

package cn.shopex.ecshopx.kaquan.service.vipgrade;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.VipGradeOrder;
import cn.shopex.ecshopx.kaquan.domain.VipGradeRelUser;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeOrderMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeRelUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Applies VIP member relations from an existing {@link VipGradeOrder} row; shared with receive and paid-completion
 * flows.
 */
@Service
public class VipGradeOrderMemberRelationApplyService {

	private final VipGradeOrderMapper vipGradeOrderMapper;
	private final VipGradeRelUserMapper vipGradeRelUserMapper;
	private final ObjectMapper objectMapper;

	public VipGradeOrderMemberRelationApplyService(
			VipGradeOrderMapper vipGradeOrderMapper,
			VipGradeRelUserMapper vipGradeRelUserMapper,
			ObjectMapper objectMapper) {
		this.vipGradeOrderMapper = vipGradeOrderMapper;
		this.vipGradeRelUserMapper = vipGradeRelUserMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> addMemberVipGrade(long companyId, long userId, long orderId, boolean isVerify) {
		VipGradeOrder order = vipGradeOrderMapper.selectOne(new LambdaQueryWrapper<VipGradeOrder>()
				.eq(VipGradeOrder::getUserId, userId)
				.eq(VipGradeOrder::getCompanyId, (int) companyId)
				.eq(VipGradeOrder::getOrderId, orderId));
		if (order == null) {
			throw new ResourceException("没有该会员卡");
		}
		if (order.getVipGradeId() == null) {
			throw new ResourceException("没有该会员卡");
		}

		JsonNode cardTypeNode;
		try {
			cardTypeNode = objectMapper.readTree(order.getCardType());
		} catch (JsonProcessingException e) {
			throw new ResourceException("没有该会员卡");
		}
		int day = cardTypeNode.path("day").asInt(0);
		long daySeconds = day * 86400L;

		String lvType = order.getLvType();
		if (lvType == null) {
			throw new ResourceException("没有该会员卡");
		}

		List<VipGradeRelUser> relList = vipGradeRelUserMapper.selectList(new LambdaQueryWrapper<VipGradeRelUser>()
				.eq(VipGradeRelUser::getUserId, userId)
				.eq(VipGradeRelUser::getCompanyId, (int) companyId));
		Map<String, VipGradeRelUser> relByType = new HashMap<>();
		for (VipGradeRelUser r : relList) {
			if (r.getVipType() != null) {
				relByType.put(r.getVipType(), r);
			}
		}

		if (isVerify && relByType.containsKey(lvType)) {
			throw new ResourceException("已领取不可重复");
		}

		long nowSec = Instant.now().getEpochSecond();

		long orderVipGradeId = order.getVipGradeId().longValue();

		if (relByType.isEmpty()) {
			long endDateSec = nowSec + daySeconds;
			insertRelUserRow(companyId, userId, lvType, orderVipGradeId, endDateSec);
			return buildRedisPayload(companyId, userId, lvType);
		}

		VipGradeRelUser svipRel = relByType.get("svip");
		VipGradeRelUser vipRel = relByType.get("vip");

		switch (lvType) {
			case "svip" -> {
				long newSvipEnd;
				if (svipRel != null) {
					long svipEnd = parseEndDateSeconds(svipRel.getEndDate());
					long svipSurplusSec = svipEnd - nowSec;
					long nowTime = (svipSurplusSec < 0) ? nowSec : svipEnd;
					newSvipEnd = nowTime + daySeconds;
					int rows = vipGradeRelUserMapper.update(
							null,
							new LambdaUpdateWrapper<VipGradeRelUser>()
									.eq(VipGradeRelUser::getUserId, userId)
									.eq(VipGradeRelUser::getCompanyId, (int) companyId)
									.eq(VipGradeRelUser::getVipType, "svip")
									.set(VipGradeRelUser::getEndDate, String.valueOf(newSvipEnd)));
					if (rows == 0) {
						throw new ResourceException("无更新数据");
					}
				} else {
					newSvipEnd = nowSec + daySeconds;
					insertRelUserRow(companyId, userId, "svip", orderVipGradeId, newSvipEnd);
				}
				if (vipRel != null) {
					long vipEnd = parseEndDateSeconds(vipRel.getEndDate());
					long vipSurplusSec = vipEnd - nowSec;
					long vipNewEnd = (vipSurplusSec < 0) ? newSvipEnd : (vipEnd + daySeconds);
					int rowsV = vipGradeRelUserMapper.update(
							null,
							new LambdaUpdateWrapper<VipGradeRelUser>()
									.eq(VipGradeRelUser::getUserId, userId)
									.eq(VipGradeRelUser::getCompanyId, (int) companyId)
									.eq(VipGradeRelUser::getVipType, "vip")
									.set(VipGradeRelUser::getEndDate, String.valueOf(vipNewEnd)));
					if (rowsV == 0) {
						throw new ResourceException("无更新数据");
					}
				}
			}
			case "vip" -> {
				if (vipRel != null) {
					long vipEnd = parseEndDateSeconds(vipRel.getEndDate());
					long vipSurplusSec = vipEnd - nowSec;
					long endDate = (vipSurplusSec < 0) ? nowSec : vipEnd;
					long vipNewEnd = endDate + daySeconds;
					int rows = vipGradeRelUserMapper.update(
							null,
							new LambdaUpdateWrapper<VipGradeRelUser>()
									.eq(VipGradeRelUser::getUserId, userId)
									.eq(VipGradeRelUser::getCompanyId, (int) companyId)
									.eq(VipGradeRelUser::getVipType, "vip")
									.set(VipGradeRelUser::getEndDate, String.valueOf(vipNewEnd)));
					if (rows == 0) {
						throw new ResourceException("无更新数据");
					}
				} else {
					long endDate = 0L;
					if (svipRel != null) {
						long svipEnd = parseEndDateSeconds(svipRel.getEndDate());
						long svipSurplusSec = svipEnd - nowSec;
						endDate = (svipSurplusSec > 0) ? svipEnd : 0L;
					}
					long nowTime = endDate > 0 ? endDate : nowSec;
					long newVipEnd = nowTime + daySeconds;
					insertRelUserRow(companyId, userId, "vip", orderVipGradeId, newVipEnd);
				}
			}
			default -> throw new ResourceException("没有该会员卡");
		}

		return buildRedisPayload(companyId, userId, lvType);
	}

	private void insertRelUserRow(long companyId, long userId, String vipType, long vipGradeId, long endDateSec) {
		VipGradeRelUser row = new VipGradeRelUser();
		row.setCompanyId((int) companyId);
		row.setUserId(userId);
		row.setVipType(vipType);
		row.setVipGradeId(vipGradeId);
		row.setEndDate(String.valueOf(endDateSec));
		vipGradeRelUserMapper.insert(row);
	}

	private static Map<String, Object> buildRedisPayload(long companyId, long userId, String vipType) {
		Map<String, Object> m = new HashMap<>(3);
		m.put("company_id", (int) companyId);
		m.put("user_id", userId);
		m.put("vip_type", vipType);
		return m;
	}

	private static long parseEndDateSeconds(String endDateStr) {
		if (endDateStr == null || endDateStr.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(endDateStr.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
