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

package cn.shopex.ecshopx.kaquan.service.cardpackage;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.kaquan.domain.CardPackageReceive;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageReceiveMapper;
import cn.shopex.ecshopx.kaquan.service.discount.KaquanDiscountCardMessages;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PackageReceivesLogQueryService {

	private static final int RECEIVE_STATUS_SUCCESS = 2;

	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final CardPackageReceiveMapper cardPackageReceiveMapper;
	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public PackageReceivesLogQueryService(
			CardPackageReceiveMapper cardPackageReceiveMapper,
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.cardPackageReceiveMapper = cardPackageReceiveMapper;
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> getReceivesLog(
			long companyId, long packageId, long page, long pageSize, boolean needEncode) {
		LambdaQueryWrapper<CardPackageReceive> wrapper = new LambdaQueryWrapper<>();
		wrapper
				.eq(CardPackageReceive::getCompanyId, companyId)
				.eq(CardPackageReceive::getPackageId, packageId)
				.eq(CardPackageReceive::getReceiveStatus, RECEIVE_STATUS_SUCCESS)
				.orderByDesc(CardPackageReceive::getReceiveTime);

		Page<CardPackageReceive> mpPage = new Page<>(page, pageSize);
		cardPackageReceiveMapper.selectPage(mpPage, wrapper);
		long total = mpPage.getTotal();
		List<CardPackageReceive> records = mpPage.getRecords();

		List<Map<String, Object>> rows = new ArrayList<>();
		if (records.isEmpty()) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("list", rows);
			out.put("count", total);
			return out;
		}

		Set<Long> userIds = new LinkedHashSet<>();
		for (CardPackageReceive r : records) {
			if (r.getUserId() != null) {
				userIds.add(r.getUserId());
			}
		}

		Map<Long, String> mobileByUserId = loadMobileByUserId(companyId, userIds);
		Map<Long, String> usernameByUserId = loadUsernameByUserId(companyId, userIds);

		for (CardPackageReceive r : records) {
			Long uid = r.getUserId();
			String username = uid != null ? usernameByUserId.getOrDefault(uid, "") : "";
			String mobile = uid != null ? mobileByUserId.getOrDefault(uid, "") : "";

			if (needEncode) {
				username = DataMasking.maskTruename(username);
				mobile = DataMasking.maskMobile(mobile);
			}

			Map<String, Object> row = new LinkedHashMap<>();
			row.put("user_id", uid);
			row.put("receive_type", translateReceiveType(r.getReceiveType()));
			row.put("receive_time", formatReceiveTime(r.getReceiveTime()));
			row.put("username", username);
			row.put("mobile", mobile);
			rows.add(row);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", rows);
		out.put("count", total);
		return out;
	}

	private static String translateReceiveType(String raw) {
		if (!StringUtils.hasText(raw)) {
			return KaquanDiscountCardMessages.RECEIVE_TYPE_UNKNOWN;
		}
		return switch (raw) {
			case "template" -> KaquanDiscountCardMessages.RECEIVE_TYPE_TEMPLATE;
			case "grade" -> KaquanDiscountCardMessages.RECEIVE_TYPE_GRADE;
			case "vip_grade" -> KaquanDiscountCardMessages.RECEIVE_TYPE_VIP_GRADE;
			default -> KaquanDiscountCardMessages.RECEIVE_TYPE_UNKNOWN;
		};
	}

	private String formatReceiveTime(Integer epochSec) {
		if (epochSec == null || epochSec <= 0) {
			return "";
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(epochSec.longValue()));
	}

	private Map<Long, String> loadMobileByUserId(long companyId, Set<Long> userIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		if (userIds.isEmpty()) {
			return out;
		}
		List<Members> members =
				membersMapper.selectList(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.in(Members::getUserId, userIds));
		Map<Long, Members> byUid = new LinkedHashMap<>();
		for (Members m : members) {
			byUid.put(m.getUserId(), m);
		}
		for (Long uid : userIds) {
			Members m = byUid.get(uid);
			String mobile = "";
			if (m != null) {
				String decrypted = sensitiveFieldEncryptor.decrypt(m.getMobile());
				if (StringUtils.hasText(decrypted)) {
					mobile = decrypted;
				} else if (StringUtils.hasText(m.getRegionMobile())) {
					mobile = m.getRegionMobile();
				}
			}
			out.put(uid, mobile);
		}
		return out;
	}

	private Map<Long, String> loadUsernameByUserId(long companyId, Set<Long> userIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		if (userIds.isEmpty()) {
			return out;
		}
		List<MembersInfo> infos =
				membersInfoMapper.selectList(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.in(MembersInfo::getUserId, userIds));
		Map<Long, String> usernameByUid = new LinkedHashMap<>();
		for (MembersInfo info : infos) {
			if (info.getUsername() != null) {
				usernameByUid.put(info.getUserId(), info.getUsername());
			}
		}
		for (Long uid : userIds) {
			out.put(uid, usernameByUid.getOrDefault(uid, ""));
		}
		return out;
	}
}
