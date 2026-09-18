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
import cn.shopex.ecshopx.common.promotions.PromotionGroupsActivityAdminGoodsDetailPort;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.service.multilang.PromotionGroupsActivityItemMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PromotionGroupsActivityDetailService {

	private static final Logger log = LoggerFactory.getLogger(PromotionGroupsActivityDetailService.class);

	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final PromotionGroupsActivityAdminRowAssembler promotionGroupsActivityAdminRowAssembler;
	private final PromotionGroupsActivityItemMultiLangReadService promotionGroupsActivityItemMultiLangReadService;
	private final PromotionGroupsActivityAdminGoodsDetailPort promotionGroupsActivityAdminGoodsDetailPort;

	public PromotionGroupsActivityDetailService(
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			PromotionGroupsActivityAdminRowAssembler promotionGroupsActivityAdminRowAssembler,
			PromotionGroupsActivityItemMultiLangReadService promotionGroupsActivityItemMultiLangReadService,
			PromotionGroupsActivityAdminGoodsDetailPort promotionGroupsActivityAdminGoodsDetailPort) {
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.promotionGroupsActivityAdminRowAssembler = promotionGroupsActivityAdminRowAssembler;
		this.promotionGroupsActivityItemMultiLangReadService = promotionGroupsActivityItemMultiLangReadService;
		this.promotionGroupsActivityAdminGoodsDetailPort = promotionGroupsActivityAdminGoodsDetailPort;
	}

	public Map<String, Object> getPromotionGroupsActivityDetail(
			long companyId,
			long groupsActivityId,
			String requestLangTag,
			HttpServletRequest request,
			Map<String, Object> operatorJwt) {
		LambdaQueryWrapper<PromotionGroupsActivity> w = new LambdaQueryWrapper<>();
		w.eq(PromotionGroupsActivity::getCompanyId, companyId)
				.eq(PromotionGroupsActivity::getGroupsActivityId, groupsActivityId)
				.eq(PromotionGroupsActivity::getDisabled, Boolean.FALSE);
		PromotionGroupsActivity entity = promotionGroupsActivityMapper.selectOne(w);
		if (entity == null) {
			throw new ResourceException("拼团活动不存在");
		}

		int now = (int) java.time.Instant.now().getEpochSecond();
		Map<String, Object> row = promotionGroupsActivityAdminRowAssembler.toRow(entity, now);

		Long aid = entity.getGroupsActivityId();
		if (aid != null) {
			Map<Long, Map<String, Object>> rowByActivityId = new LinkedHashMap<>();
			rowByActivityId.put(aid, row);
			promotionGroupsActivityItemMultiLangReadService.applyBatch(
					companyId, List.of(aid), rowByActivityId, requestLangTag);
		}

		Long goodsId = entity.getGoodsId();
		if (goodsId == null || goodsId < 1L) {
			row.put("goods", List.of());
		} else {
			String authorizerAppId = null;
			Object rawAid = operatorJwt.get("authorizer_appid");
			if (rawAid != null) {
				authorizerAppId = rawAid.toString();
			}
			try {
				Map<String, Object> detail =
						promotionGroupsActivityAdminGoodsDetailPort.getAdminItemsDetail(
								request, operatorJwt, goodsId.longValue(), authorizerAppId);
				if (detail != null
						&& detail.containsKey("status_code")
						&& detail.get("item_id") == null) {
					row.put("goods", List.of());
				} else {
					row.put("goods", detail);
				}
			} catch (Exception e) {
				log.warn("promotion groups activity goods detail failed: companyId={} goodsId={}", companyId, goodsId, e);
				row.put("goods", List.of());
			}
		}

		return row;
	}
}
