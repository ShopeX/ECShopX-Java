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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PromoterChildrenListService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter BIND_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

	private final PromoterMapper promoterMapper;
	private final PromoterGradeService promoterGradeService;
	private final MemberAccountService memberAccountService;
	private final PopularizeBrokerageCountReadService popularizeBrokerageCountReadService;
	private final PromoterIdentityMapper promoterIdentityMapper;
	private final boolean oemShuyun;

	public PromoterChildrenListService(
			PromoterMapper promoterMapper,
			PromoterGradeService promoterGradeService,
			MemberAccountService memberAccountService,
			PopularizeBrokerageCountReadService popularizeBrokerageCountReadService,
			PromoterIdentityMapper promoterIdentityMapper,
			@Value("${ecshopx.request-field.oem-shuyun:false}") boolean oemShuyun) {
		this.promoterMapper = promoterMapper;
		this.promoterGradeService = promoterGradeService;
		this.memberAccountService = memberAccountService;
		this.popularizeBrokerageCountReadService = popularizeBrokerageCountReadService;
		this.promoterIdentityMapper = promoterIdentityMapper;
		this.oemShuyun = oemShuyun;
	}

	public Map<String, Object> getPromoterchildrenList(
			long companyId, long parentPromoterId, int page, int pageSize) {
		if (parentPromoterId <= 0L) {
			throw new BadRequestException("参数错误");
		}

		int offset = (page - 1) * pageSize;

		LambdaQueryWrapper<Promoter> w = new LambdaQueryWrapper<Promoter>()
				.eq(Promoter::getCompanyId, companyId)
				.eq(Promoter::getPid, parentPromoterId)
				.orderByDesc(Promoter::getCreated);
		List<Promoter> all = promoterMapper.selectList(w);
		List<Promoter> filtered =
				all.stream()
						.filter(
								p ->
										p.getCompanyId() != null
												&& p.getCompanyId().longValue() == companyId)
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

		Map<Long, Map<String, Object>> brokerageByUser = new LinkedHashMap<>();
		if (total > 0) {
			brokerageByUser.putAll(
					popularizeBrokerageCountReadService.batchPromoterBrokerageCountsByUserIds(
							companyId, userIds));
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

			Map<String, Object> stats =
					uid > 0L ? brokerageByUser.getOrDefault(uid, Map.of()) : Map.of();
			Map<String, Object> merged = new LinkedHashMap<>(row);
			Map<String, Object> statCopy = defaultBrokerageStats();
			statCopy.putAll(stats);
			merged.putAll(statCopy);
			listMaps.add(merged);
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", (long) total);
		data.put("list", listMaps);
		return data;
	}

	private static Map<String, Object> defaultBrokerageStats() {
		LinkedHashMap<String, Object> z = new LinkedHashMap<>();
		z.put("itemTotalPrice", 0L);
		z.put("rebateTotal", 0L);
		z.put("noCloseRebate", 0L);
		z.put("cashWithdrawalRebate", 0L);
		z.put("freezeCashWithdrawalRebate", 0L);
		z.put("rechargeRebate", 0L);
		z.put("payedRebate", 0L);
		z.put("noClosePoint", 0L);
		z.put("pointTotal", 0L);
		return z;
	}
}
