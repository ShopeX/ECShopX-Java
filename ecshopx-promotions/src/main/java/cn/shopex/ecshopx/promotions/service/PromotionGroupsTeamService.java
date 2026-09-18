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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.cron.GroupRobotWechatInfo;
import cn.shopex.ecshopx.common.cron.PromotionGroupRobotWechatProfilePort;
import cn.shopex.ecshopx.orders.service.group.GroupPromotionOrderPayedService;
import cn.shopex.ecshopx.promotions.domain.PaymentOverEndTimeRow;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import cn.shopex.ecshopx.promotions.domain.ScheduleAutoDoneGroupTeamRow;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMemberMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 拼团活动团单编排：自动成团、机械人参团、团员支付成功后的订单终态与权益。
 */
@Service
@RequiredArgsConstructor
public class PromotionGroupsTeamService {

	private static final int PAGE_SIZE = 100;
	private static final int SECOND_WINDOW = 600;

	private final PromotionGroupsTeamMapper promotionGroupsTeamMapper;
	private final PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper;
	private final PromotionGroupsTeamFailService promotionGroupsTeamFailService;
	private final GroupPromotionOrderPayedService groupPromotionOrderPayedService;
	private final PromotionGroupRobotWechatProfilePort promotionGroupRobotWechatProfilePort;
	private final ObjectMapper objectMapper;

	/**
	 * 无库存机器人活动下自动成团：筛选 {@code a.store = 0}，不按结束时间窗口过滤，且不执行超时支付强制失败。
	 *
	 * @return 各轮列表查询返回行数之和（与 {@link #scheduleAutoDoneGroup()} 相同统计口径，便于观测扫描进度）
	 */
	public int scheduleNoStoreAutoDoneGroup() {
		long totalCount = promotionGroupsTeamMapper.countScheduleNoStoreAutoDoneGroup();
		if (totalCount == 0L) {
			return 0;
		}
		int totalPage = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
		int processed = 0;
		for (int i = 1; i <= totalPage; i++) {
			List<ScheduleAutoDoneGroupTeamRow> pageList =
					promotionGroupsTeamMapper.listScheduleNoStoreAutoDoneGroupPage(0, PAGE_SIZE);
			processed += pageList.size();
			for (ScheduleAutoDoneGroupTeamRow row : pageList) {
				if (!Objects.equals(row.getTeamStatus(), 1L)) {
					continue;
				}
				if (!groupRobot(row)) {
					continue;
				}
				List<PromotionGroupsTeamMember> successList =
						promotionGroupsTeamMemberMapper.selectGroupTeamSuccessMembers(row.getTeamId());
				for (PromotionGroupsTeamMember mem : successList) {
					if (mem.getMemberId() == null || mem.getMemberId() <= 0L) {
						continue;
					}
					long orderId = parseOrderId(mem.getOrderId());
					if (orderId <= 0L) {
						continue;
					}
					long companyId =
							mem.getCompanyId() != null && mem.getCompanyId() > 0L
									? mem.getCompanyId()
									: row.getCompanyId();
					if ("services".equals(safeGroupGoodsType(row.getGroupGoodsType()))) {
						groupPromotionOrderPayedService.markServiceGroupOrderPayedAndTryGrantRights(
								companyId, mem.getMemberId(), orderId);
					} else {
						groupPromotionOrderPayedService.markNormalGroupOrderPayedAndPublishErpSync(
								companyId, mem.getMemberId(), orderId);
					}
				}
			}
		}
		return processed;
	}

	/**
	 * @return 本次任务从库中分页扫描到的团行总数（与 PHP 侧无显式返回值时仍报告读表进度一致）
	 */
	public int scheduleAutoDoneGroup() {
		long nowSec = System.currentTimeMillis() / 1000L;
		long totalCount = promotionGroupsTeamMapper.countScheduleAutoDoneGroup(nowSec, SECOND_WINDOW);
		if (totalCount == 0L) {
			return 0;
		}
		int totalPage = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
		int processed = 0;
		for (int page = 1; page <= totalPage; page++) {
			int offset = (page - 1) * PAGE_SIZE;
			List<ScheduleAutoDoneGroupTeamRow> pageList =
					promotionGroupsTeamMapper.listScheduleAutoDoneGroupPage(
							nowSec, SECOND_WINDOW, offset, PAGE_SIZE);
			processed += pageList.size();
			List<String> teamIds =
					pageList.stream().map(ScheduleAutoDoneGroupTeamRow::getTeamId).toList();
			Set<String> failedTeamIds = forceTeamFailIfPaymentTimeOverEndTime(teamIds);
			for (ScheduleAutoDoneGroupTeamRow row : pageList) {
				if (failedTeamIds.contains(row.getTeamId())) {
					continue;
				}
				if (!Objects.equals(row.getTeamStatus(), 1L)) {
					continue;
				}
				if (!groupRobot(row)) {
					continue;
				}
				List<PromotionGroupsTeamMember> successList =
						promotionGroupsTeamMemberMapper.selectGroupTeamSuccessMembers(row.getTeamId());
				for (PromotionGroupsTeamMember mem : successList) {
					if (mem.getMemberId() == null || mem.getMemberId() <= 0L) {
						continue;
					}
					long orderId = parseOrderId(mem.getOrderId());
					if (orderId <= 0L) {
						continue;
					}
					long companyId =
							mem.getCompanyId() != null && mem.getCompanyId() > 0L
									? mem.getCompanyId()
									: row.getCompanyId();
					if ("services".equals(safeGroupGoodsType(row.getGroupGoodsType()))) {
						groupPromotionOrderPayedService.markServiceGroupOrderPayedAndTryGrantRights(
								companyId, mem.getMemberId(), orderId);
					} else {
						groupPromotionOrderPayedService.markNormalGroupOrderPayedAndPublishErpSync(
								companyId, mem.getMemberId(), orderId);
					}
				}
			}
		}
		return processed;
	}

	private Set<String> forceTeamFailIfPaymentTimeOverEndTime(List<String> teamIds) {
		if (teamIds == null || teamIds.isEmpty()) {
			return Set.of();
		}
		List<String> unique = new ArrayList<>(new LinkedHashSet<>(teamIds));
		Set<String> teamsToFail = new LinkedHashSet<>();
		for (int i = 0; i < unique.size(); i += 100) {
			int to = Math.min(i + 100, unique.size());
			List<String> chunk = unique.subList(i, to);
			List<PaymentOverEndTimeRow> rows =
					promotionGroupsTeamMemberMapper.selectPaymentOverEndTimeList(chunk);
			for (PaymentOverEndTimeRow r : rows) {
				if (StringUtils.hasText(r.getTeamId())) {
					teamsToFail.add(r.getTeamId());
				}
			}
		}
		List<String> failList = new ArrayList<>(teamsToFail);
		for (int i = 0; i < failList.size(); i += 100) {
			int to = Math.min(i + 100, failList.size());
			for (int j = i; j < to; j++) {
				promotionGroupsTeamFailService.teamFail(failList.get(j));
			}
		}
		return new HashSet<>(teamsToFail);
	}

	private boolean groupRobot(ScheduleAutoDoneGroupTeamRow info) {
		long actPerson = info.getActPersonNum() == null ? 0L : info.getActPersonNum();
		long joinNum = info.getJoinPersonNum() == null ? 0L : info.getJoinPersonNum();
		long num = actPerson - joinNum + 1L;
		int need = (int) Math.max(num, 0L);
		List<GroupRobotWechatInfo> rand = promotionGroupRobotWechatProfilePort.getRandUserInfo(need);
		String groupType = safeGroupGoodsType(info.getGroupGoodsType());
		for (int i = 1; i < num; i++) {
			GroupRobotWechatInfo prof = (i - 1 < rand.size()) ? rand.get(i - 1) : null;
			String head = prof != null && prof.getHeadimgurl() != null ? prof.getHeadimgurl() : "";
			String nick =
					prof != null && prof.getNickname() != null && !prof.getNickname().isEmpty()
							? prof.getNickname()
							: "匿名用户";
			PromotionGroupsTeamMember m = new PromotionGroupsTeamMember();
			m.setTeamId(info.getTeamId());
			m.setCompanyId(info.getCompanyId());
			m.setActId(info.getActId());
			m.setMemberId(0L);
			m.setOrderId("");
			m.setJoinTime(System.currentTimeMillis() / 1000L);
			m.setGroupGoodsType(groupType);
			m.setDisabled(false);
			m.setMemberInfo(encodeMemberInfo(head, nick));
			promotionGroupsTeamMemberMapper.insert(m);
		}
		promotionGroupsTeamMapper.update(
				null,
				new UpdateWrapper<PromotionGroupsTeam>()
						.eq("id", info.getId())
						.set("join_person_num", actPerson)
						.set("team_status", 2L));
		return true;
	}

	private String encodeMemberInfo(String headimgurl, String nickname) {
		try {
			return objectMapper.writeValueAsString(
					Map.of("headimgurl", headimgurl, "nickname", nickname));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static long parseOrderId(String orderId) {
		if (!StringUtils.hasText(orderId)) {
			return 0L;
		}
		try {
			return Long.parseLong(orderId.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String safeGroupGoodsType(String g) {
		return g == null ? "services" : g;
	}
}
