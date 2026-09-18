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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import cn.shopex.ecshopx.promotions.integration.order.PromotionGroupTeamOrderFailHandler;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PromotionGroupsTeamFailService {

	private static final Logger log = LoggerFactory.getLogger(PromotionGroupsTeamFailService.class);

	private final PromotionGroupsTeamMapper promotionGroupsTeamMapper;
	private final PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper;
	private final MessageSource messageSource;
	private final PromotionGroupTeamOrderFailHandler orderFailHandler;

	public PromotionGroupsTeamFailService(
			PromotionGroupsTeamMapper promotionGroupsTeamMapper,
			PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper,
			MessageSource messageSource,
			@Qualifier("promotionGroupTeamOrderFailHandler") PromotionGroupTeamOrderFailHandler orderFailHandler) {
		this.promotionGroupsTeamMapper = promotionGroupsTeamMapper;
		this.promotionGroupsTeamMemberMapper = promotionGroupsTeamMemberMapper;
		this.messageSource = messageSource;
		this.orderFailHandler = orderFailHandler;
	}

	public void teamFail(String teamId) {
		Locale locale = LocaleContextHolder.getLocale();
		LambdaQueryWrapper<PromotionGroupsTeam> byTeam =
				new LambdaQueryWrapper<PromotionGroupsTeam>()
						.eq(PromotionGroupsTeam::getTeamId, teamId)
						.last("LIMIT 1");
		PromotionGroupsTeam team = promotionGroupsTeamMapper.selectOne(byTeam);
		if (team == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.no_update_data_found", null, locale));
		}
		promotionGroupsTeamMapper.update(
				null,
				new LambdaUpdateWrapper<PromotionGroupsTeam>()
						.eq(PromotionGroupsTeam::getTeamId, teamId)
						.set(PromotionGroupsTeam::getTeamStatus, 3L));
		team = promotionGroupsTeamMapper.selectOne(byTeam);
		if (team == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.no_update_data_found", null, locale));
		}
		LambdaQueryWrapper<PromotionGroupsTeamMember> mw =
				new LambdaQueryWrapper<PromotionGroupsTeamMember>()
						.eq(PromotionGroupsTeamMember::getTeamId, teamId)
						.ne(PromotionGroupsTeamMember::getMemberId, 0L)
						.and(
								q ->
										q.eq(PromotionGroupsTeamMember::getDisabled, false)
												.or()
												.isNull(PromotionGroupsTeamMember::getDisabled));
		List<PromotionGroupsTeamMember> list = promotionGroupsTeamMemberMapper.selectList(mw);
		if (list == null || list.isEmpty()) {
			return;
		}
		for (PromotionGroupsTeamMember row : list) {
			try {
				orderFailHandler.onFailedTeamMember(team, row);
			} catch (Exception ex) {
				log.debug("拼团失败，退款失败参数: {}", row);
				log.debug("拼团失败，退款失败: {}", ex.getMessage());
			}
		}
	}
}
