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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PagesTemplateSetSetRequest;
import cn.shopex.ecshopx.theme.domain.PagesTemplateSet;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateSetMapper;
import cn.shopex.ecshopx.theme.support.PagesTemplateSetRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PagesTemplateSetSaveService {

	private final PagesTemplateSetMapper pagesTemplateSetMapper;
	private final PagesTemplateSetOutsideTabBarWriteService pagesTemplateSetOutsideTabBarWriteService;
	private final PagesTemplateSetOutsideTabBarReadService pagesTemplateSetOutsideTabBarReadService;
	private final LangueProperties langueProperties;
	private final PagesTemplateSetRowMapper pagesTemplateSetRowMapper;

	public Map<String, Object> set(long companyId, String requestLang, PagesTemplateSetSetRequest req) {
		long rid = req.getRegionauthId() == null ? 0L : req.getRegionauthId();
		long tid = req.getPagesTemplateId() == null ? 0L : req.getPagesTemplateId();

		LambdaQueryWrapper<PagesTemplateSet> q = new LambdaQueryWrapper<>();
		q.eq(PagesTemplateSet::getCompanyId, companyId)
				.eq(PagesTemplateSet::getRegionauthId, rid)
				.eq(PagesTemplateSet::getPagesTemplateId, tid);

		PagesTemplateSet existing = pagesTemplateSetMapper.selectOne(q);
		boolean nonDefault = !langueProperties.isDefaultLang(requestLang);
		boolean tabBarPayloadPresent = req.getTabBar() != null && !isLooseEmpty(req.getTabBar());

		if (existing == null) {
			PagesTemplateSet entity = new PagesTemplateSet();
			entity.setCompanyId(companyId);
			entity.setRegionauthId(rid);
			entity.setPagesTemplateId(tid);
			applyOptionalScalars(entity, req, true);
			// 非默认语种不写主表 tab_bar（对齐 PHP stripLangFields）；默认语种写主表
			if (!nonDefault && tabBarPayloadPresent) {
				entity.setTabBar(req.getTabBar().trim());
			}
			pagesTemplateSetMapper.insert(entity);
			if (entity.getId() == null) {
				throw new ResourceException("未查询到更新数据");
			}
			if (nonDefault && tabBarPayloadPresent) {
				pagesTemplateSetOutsideTabBarWriteService.upsertTabBar(
						(int) companyId, entity.getId().longValue(), req.getTabBar().trim(), requestLang);
			}
		} else {
			PagesTemplateSet row = pagesTemplateSetMapper.selectOne(q);
			if (row == null) {
				throw new ResourceException("未查询到更新数据");
			}
			applyOptionalScalars(row, req, false);
			// 非默认语种：主表 tab_bar 保持原值，不置 null
			if (!nonDefault && tabBarPayloadPresent) {
				row.setTabBar(req.getTabBar().trim());
			}
			pagesTemplateSetMapper.updateById(row);
			if (nonDefault && tabBarPayloadPresent) {
				pagesTemplateSetOutsideTabBarWriteService.upsertTabBar(
						(int) companyId, row.getId().longValue(), req.getTabBar().trim(), requestLang);
			}
		}

		PagesTemplateSet finalRow = pagesTemplateSetMapper.selectOne(q);
		if (finalRow == null) {
			throw new ResourceException("未查询到更新数据");
		}
		String displayTabBar;
		if (langueProperties.isDefaultLang(requestLang)) {
			displayTabBar = finalRow.getTabBar();
		} else {
			String fromMod = pagesTemplateSetOutsideTabBarReadService.findTabBarOverlay(
					(int) companyId, finalRow.getId().longValue(), requestLang);
			displayTabBar = fromMod != null ? fromMod : finalRow.getTabBar();
		}
		PagesTemplateSet merged = new PagesTemplateSet();
		merged.setId(finalRow.getId());
		merged.setCompanyId(finalRow.getCompanyId());
		merged.setRegionauthId(finalRow.getRegionauthId());
		merged.setIndexType(finalRow.getIndexType());
		merged.setPagesTemplateId(finalRow.getPagesTemplateId());
		merged.setIsEnforceSync(finalRow.getIsEnforceSync());
		merged.setIsOpenRecommend(finalRow.getIsOpenRecommend());
		merged.setIsOpenWechatappLocation(finalRow.getIsOpenWechatappLocation());
		merged.setIsOpenScanQrcode(finalRow.getIsOpenScanQrcode());
		merged.setIsOpenOfficialAccount(finalRow.getIsOpenOfficialAccount());
		merged.setTabBar(displayTabBar);
		return pagesTemplateSetRowMapper.toRowMap(merged);
	}

	private void applyOptionalScalars(PagesTemplateSet target, PagesTemplateSetSetRequest req, boolean create) {
		if (req.getIndexType() != null && !isLooseEmpty(req.getIndexType())) {
			target.setIndexType(req.getIndexType());
		}
		if (req.getIsEnforceSync() != null && !isLooseEmpty(req.getIsEnforceSync())) {
			target.setIsEnforceSync(req.getIsEnforceSync());
		}
		if (req.getIsOpenRecommend() != null && !isLooseEmpty(req.getIsOpenRecommend())) {
			target.setIsOpenRecommend(req.getIsOpenRecommend());
		}
		if (req.getIsOpenWechatappLocation() != null && !isLooseEmpty(req.getIsOpenWechatappLocation())) {
			target.setIsOpenWechatappLocation(req.getIsOpenWechatappLocation());
		}
		if (req.getIsOpenScanQrcode() != null && !isLooseEmpty(req.getIsOpenScanQrcode())) {
			target.setIsOpenScanQrcode(req.getIsOpenScanQrcode());
		}
		if (req.getIsOpenOfficialAccount() != null && !isLooseEmpty(req.getIsOpenOfficialAccount())) {
			target.setIsOpenOfficialAccount(req.getIsOpenOfficialAccount());
		}
		if (!create) {
			if (req.getRegionauthId() != null && req.getRegionauthId() != 0L) {
				target.setRegionauthId(req.getRegionauthId());
			}
			if (req.getPagesTemplateId() != null && req.getPagesTemplateId() != 0L) {
				target.setPagesTemplateId(req.getPagesTemplateId());
			}
		}
	}

	private static boolean isLooseEmpty(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (v instanceof CharSequence cs) {
			String t = cs.toString().trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		return false;
	}
}
