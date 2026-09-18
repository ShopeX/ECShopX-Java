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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.openapi.thirdapi.v2.OpenapiThirdApiV2MemberTagListService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberPointOrderListService {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final OpenapiMemberPointOrderV2ListFilterBuilder filterBuilder;
	private final NormalOrdersMapper normalOrdersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public OpenapiThirdApiV2MemberPointOrderListService(
			OpenapiMemberPointOrderV2ListFilterBuilder filterBuilder,
			NormalOrdersMapper normalOrdersMapper,
			MembersInfoMapper membersInfoMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.filterBuilder = filterBuilder;
		this.normalOrdersMapper = normalOrdersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> executeOpenapiList(
			long companyId,
			boolean mobilePresent,
			String mobileRaw,
			boolean orderIdPresent,
			String orderIdRaw,
			int page,
			int pageSize) {
		OpenapiMemberPointOrderV2ListFilterBuilder.FilterSpec spec =
				filterBuilder.build(
						companyId, mobilePresent, mobileRaw, orderIdPresent, orderIdRaw);

		LambdaQueryWrapper<NormalOrders> base =
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, spec.companyId())
						.ge(NormalOrders::getCreateTime, spec.createTimeGte());
		if (spec.userId() != null) {
			base.eq(NormalOrders::getUserId, spec.userId());
		}
		if (spec.orderId() != null) {
			base.eq(NormalOrders::getOrderId, spec.orderId());
		}

		long totalCount = normalOrdersMapper.selectCount(base);

		LambdaQueryWrapper<NormalOrders> listWrapper =
				base.clone()
						.select(
								NormalOrders::getOrderId,
								NormalOrders::getUserId,
								NormalOrders::getMobile,
								NormalOrders::getCreateTime,
								NormalOrders::getGetPoints,
								NormalOrders::getBonusPoints,
								NormalOrders::getExtraPoints,
								NormalOrders::getPointUse,
								NormalOrders::getPointFee)
						.orderByDesc(NormalOrders::getOrderId);

		Page<NormalOrders> pageReq = new Page<>(page, pageSize, false);
		normalOrdersMapper.selectPage(pageReq, listWrapper);
		List<NormalOrders> entities = pageReq.getRecords();

		List<Map<String, Object>> list = List.of();
		if (entities != null && !entities.isEmpty()) {
			list = formatPointOrderRows(entities);
			appendUsernameToList(companyId, list);
		}

		return OpenapiThirdApiV2MemberTagListService.formatListStruct(
				totalCount, list, page, pageSize);
	}

	private List<Map<String, Object>> formatPointOrderRows(List<NormalOrders> entities) {
		List<Map<String, Object>> rows = new ArrayList<>(entities.size());
		for (NormalOrders entity : entities) {
			int getPoints = nzInt(entity.getGetPoints());
			int bonusPoints = nzInt(entity.getBonusPoints());
			int extraPoints = nzInt(entity.getExtraPoints());

			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("order_id", String.valueOf(entity.getOrderId()));
			row.put("user_id", entity.getUserId() != null ? entity.getUserId() : 0L);
			row.put("mobile", decryptMaybe(entity.getMobile()));
			row.put("create_time", formatEpochSeconds(entity.getCreateTime()));
			row.put("get_points", getPoints);
			row.put("bonus_points", bonusPoints);
			row.put("extra_points", extraPoints);
			row.put("point_use", nzInt(entity.getPointUse()));
			row.put("point_fee", pointFeeToYuanString(entity.getPointFee()));
			row.put("total_points", getPoints + bonusPoints + extraPoints);
			rows.add(row);
		}
		return rows;
	}

	private void appendUsernameToList(long companyId, List<Map<String, Object>> list) {
		List<Long> userIds =
				list.stream()
						.map(row -> longOrZero(row.get("user_id")))
						.distinct()
						.toList();
		Map<Long, String> usernameByUserId = Map.of();
		if (!userIds.isEmpty()) {
			List<MembersInfo> infos =
					membersInfoMapper.selectList(
							new LambdaQueryWrapper<MembersInfo>()
									.eq(MembersInfo::getCompanyId, companyId)
									.in(MembersInfo::getUserId, userIds)
									.select(
											MembersInfo::getUserId,
											MembersInfo::getUsername));
			usernameByUserId = new HashMap<>();
			for (MembersInfo mi : infos) {
				usernameByUserId.put(mi.getUserId(), decryptMaybe(mi.getUsername()));
			}
		}
		for (Map<String, Object> row : list) {
			long uid = longOrZero(row.get("user_id"));
			row.put("username", usernameByUserId.getOrDefault(uid, ""));
			row.remove("user_id");
		}
	}

	private String decryptMaybe(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		try {
			return sensitiveFieldEncryptor.decrypt(raw);
		} catch (Exception e) {
			return raw;
		}
	}

	private static String formatEpochSeconds(Integer epochSeconds) {
		if (epochSeconds == null || epochSeconds <= 0) {
			return "";
		}
		return DATETIME_FMT.format(Instant.ofEpochSecond(epochSeconds.longValue()));
	}

	private static int nzInt(Integer value) {
		return value != null ? value : 0;
	}

	private static String pointFeeToYuanString(Integer pointFee) {
		if (pointFee == null) {
			return "";
		}
		return BigDecimal.valueOf(pointFee)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static long longOrZero(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
