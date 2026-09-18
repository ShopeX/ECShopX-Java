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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.domain.PromoterIdentity;
import cn.shopex.ecshopx.popularize.mapper.PromoterIdentityMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PromoterFrontChildrenPromoterListService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter BIND_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

	private final PromoterMapper promoterMapper;
	private final PromoterGradeService promoterGradeService;
	private final MemberAccountService memberAccountService;
	private final PromoterIdentityMapper promoterIdentityMapper;
	private final boolean oemShuyun;

	public PromoterFrontChildrenPromoterListService(
			PromoterMapper promoterMapper,
			PromoterGradeService promoterGradeService,
			MemberAccountService memberAccountService,
			PromoterIdentityMapper promoterIdentityMapper,
			@Value("${ecshopx.request-field.oem-shuyun:false}") boolean oemShuyun) {
		this.promoterMapper = promoterMapper;
		this.promoterGradeService = promoterGradeService;
		this.memberAccountService = memberAccountService;
		this.promoterIdentityMapper = promoterIdentityMapper;
		this.oemShuyun = oemShuyun;
	}

	public Map<String, Object> getChildrenpromoterList(
			long companyId,
			long authUserId,
			Optional<Long> promoterUserIdFilter,
			int page,
			int pageSize) {
		if (companyId <= 0L || authUserId <= 0L) {
			throw new BadRequestException("参数错误");
		}

		if (promoterUserIdFilter.isPresent() && Objects.equals(promoterUserIdFilter.get(), -1L)) {
			return Map.of("total_count", 0L, "list", List.of());
		}

		int offset = (page - 1) * pageSize;

		Promoter root =
				promoterMapper.selectOne(
						new LambdaQueryWrapper<Promoter>()
								.eq(Promoter::getCompanyId, companyId)
								.eq(Promoter::getUserId, authUserId)
								.last("LIMIT 1"));
		if (root == null || root.getId() == null) {
			return Map.of("total_count", 0L, "list", List.of());
		}
		long rootId = root.getId();

		LambdaQueryWrapper<Promoter> childW =
				new LambdaQueryWrapper<Promoter>()
						.eq(Promoter::getCompanyId, companyId)
						.eq(Promoter::getPid, rootId)
						.eq(Promoter::getIsPromoter, 1)
						.eq(Promoter::getDisabled, 0)
						.orderByDesc(Promoter::getCreated);
		if (promoterUserIdFilter.isPresent()) {
			long uidF = promoterUserIdFilter.get();
			if (uidF > 0L) {
				childW = childW.eq(Promoter::getUserId, uidF);
			}
		}

		List<Promoter> fromDb = promoterMapper.selectList(childW);
		List<Promoter> filtered =
				fromDb.stream()
						.filter(p -> Objects.equals(Long.valueOf(companyId), p.getCompanyId()))
						.filter(p -> Objects.equals(Integer.valueOf(1), p.getIsPromoter()))
						.filter(p -> Objects.equals(Integer.valueOf(0), p.getDisabled()))
						.toList();

		int total = filtered.size();
		int from = Math.min(offset, total);
		int to = Math.min(offset + pageSize, total);
		List<Promoter> pageRows = filtered.subList(from, to);

		if (pageRows.isEmpty()) {
			return Map.of("total_count", (long) total, "list", List.of());
		}

		List<Long> childPromoterIds =
				pageRows.stream().map(Promoter::getId).filter(Objects::nonNull).toList();

		Map<Long, Long> pidToCount = new HashMap<>();
		if (!childPromoterIds.isEmpty()) {
			List<Map<String, Object>> cntRows =
					promoterMapper.countDirectChildrenByPidList(companyId, childPromoterIds);
			for (Map<String, Object> r : cntRows) {
				if (r == null) {
					continue;
				}
				Object pidO = r.get("pid");
				Object cntO = r.get("cnt");
				if (pidO == null) {
					continue;
				}
				long pid =
						pidO instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(pidO).trim());
				long cnt =
						cntO instanceof Number n
								? n.longValue()
								: Long.parseLong(String.valueOf(cntO).trim());
				pidToCount.put(pid, cnt);
			}
		}

		boolean isOpen = promoterGradeService.readIsOpenPromoterGrade(companyId);
		String isOpenStr = isOpen ? "true" : "false";

		List<Long> userIds =
				pageRows.stream()
						.map(Promoter::getUserId)
						.filter(Objects::nonNull)
						.filter(uid -> uid > 0L)
						.distinct()
						.toList();

		Map<Long, String> usernameByUser = new HashMap<>();
		Map<Long, String> mobileByUser = new HashMap<>();
		for (Map<String, Object> sum : memberAccountService.listMemberSummariesByUserIds(companyId, userIds)) {
			Object uidObj = sum.get("user_id");
			if (uidObj == null) {
				continue;
			}
			long uid = uidObj instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(uidObj).trim());
			usernameByUser.put(
					uid, sum.get("username") != null ? String.valueOf(sum.get("username")) : "");
			mobileByUser.put(uid, sum.get("mobile") != null ? String.valueOf(sum.get("mobile")) : "");
		}

		Map<Long, Map<String, String>> wechatByUser =
				memberAccountService.batchWechatNicknameHeadByUserIds(companyId, userIds);

		Map<Long, String> identityNameById = new HashMap<>();
		if (oemShuyun) {
			List<Long> identityIds =
					pageRows.stream()
							.map(Promoter::getIdentityId)
							.filter(Objects::nonNull)
							.filter(id -> id > 0L)
							.distinct()
							.toList();
			if (!identityIds.isEmpty()) {
				List<PromoterIdentity> identities = promoterIdentityMapper.selectBatchIds(identityIds);
				if (identities != null) {
					for (PromoterIdentity pi : identities) {
						if (pi != null && pi.getId() != null && pi.getName() != null) {
							identityNameById.put(pi.getId(), pi.getName());
						}
					}
				}
			}
		}

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (Promoter p : pageRows) {
			long pid = p.getId() != null ? p.getId() : 0L;
			long uid = p.getUserId() != null ? p.getUserId() : 0L;

			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("relationship_depth", 1);
			row.put("promoter_id", pid);
			row.put("id", pid);
			row.put("company_id", p.getCompanyId());
			row.put("user_id", p.getUserId());
			row.put("pid", p.getPid());
			row.put("grade_level", p.getGradeLevel());
			row.put("shop_status", p.getShopStatus());
			row.put("is_promoter", p.getIsPromoter());
			row.put("disabled", p.getDisabled());
			row.put("is_buy", p.getIsBuy());
			row.put("created", p.getCreated());
			row.put("pmobile", p.getPmobile() != null ? p.getPmobile() : "");
			row.put("children_count", pidToCount.getOrDefault(pid, 0L));

			Integer createdInt = p.getCreated();
			if (createdInt != null) {
				row.put(
						"bind_date",
						Instant.ofEpochSecond(createdInt.longValue())
								.atZone(SHANGHAI)
								.toLocalDate()
								.format(BIND_DATE_FMT));
			} else {
				row.put("bind_date", "");
			}

			row.put("mobile", uid > 0L ? mobileByUser.getOrDefault(uid, "") : "");
			row.put("username", uid > 0L ? usernameByUser.getOrDefault(uid, "") : "");

			String regionMobile = "";
			if (uid > 0L) {
				Map<String, Object> info = memberAccountService.getMemberInfo(uid, companyId);
				Object v = info.get("region_mobile");
				if (v != null) {
					String s = String.valueOf(v).trim();
					if (!s.isEmpty()) {
						regionMobile = s;
					}
				}
			}
			row.put("region_mobile", regionMobile);

			Map<String, String> wx = uid > 0L ? wechatByUser.get(uid) : null;
			if (wx != null) {
				row.put("nickname", wx.getOrDefault("nickname", ""));
				row.put("headimgurl", wx.getOrDefault("headimgurl", ""));
			} else {
				row.put("nickname", "");
				row.put("headimgurl", "");
			}

			row.put(
					"promoter_grade_name",
					promoterGradeService.readPromoterGradeDisplayName(companyId, p.getGradeLevel()));
			row.put("is_open_promoter_grade", isOpenStr);

			if (oemShuyun && p.getIdentityId() != null && p.getIdentityId() > 0L) {
				String iname = identityNameById.get(p.getIdentityId());
				if (iname != null) {
					row.put("identity_name", iname);
				}
			}

			listMaps.add(row);
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", (long) total);
		data.put("list", listMaps);
		return data;
	}
}
