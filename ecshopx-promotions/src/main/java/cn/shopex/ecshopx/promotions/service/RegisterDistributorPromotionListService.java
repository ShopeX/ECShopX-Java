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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.DistributorListRowFormatService;
import cn.shopex.ecshopx.distribution.service.SelfDeliverySettingReadService;
import cn.shopex.ecshopx.promotions.domain.DistributorPromotions;
import cn.shopex.ecshopx.promotions.domain.RegisterPromotions;
import cn.shopex.ecshopx.promotions.mapper.DistributorPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.RegisterPromotionsMapper;
import cn.shopex.ecshopx.promotions.service.multilang.RegisterPromotionMultiLangReadService;
import cn.shopex.ecshopx.promotions.service.support.RegisterPromotionApiRowBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RegisterDistributorPromotionListService {

	private static final int DISTRIBUTOR_SUBQUERY_PAGE = 1;
	private static final int DISTRIBUTOR_SUBQUERY_PAGE_SIZE = 100;

	private final RegisterPromotionsMapper registerPromotionsMapper;
	private final RegisterPromotionApiRowBuilder registerPromotionApiRowBuilder;
	private final RegisterPromotionMultiLangReadService registerPromotionMultiLangReadService;
	private final DistributorPromotionsMapper distributorPromotionsMapper;
	private final DistributorMapper distributorMapper;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final ObjectMapper objectMapper;

	public RegisterDistributorPromotionListService(
			RegisterPromotionsMapper registerPromotionsMapper,
			RegisterPromotionApiRowBuilder registerPromotionApiRowBuilder,
			RegisterPromotionMultiLangReadService registerPromotionMultiLangReadService,
			DistributorPromotionsMapper distributorPromotionsMapper,
			DistributorMapper distributorMapper,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			ObjectMapper objectMapper) {
		this.registerPromotionsMapper = registerPromotionsMapper;
		this.registerPromotionApiRowBuilder = registerPromotionApiRowBuilder;
		this.registerPromotionMultiLangReadService = registerPromotionMultiLangReadService;
		this.distributorPromotionsMapper = distributorPromotionsMapper;
		this.distributorMapper = distributorMapper;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getRegisterList(long companyId, int page, int pageSize, String requestLangTag) {
		LambdaQueryWrapper<RegisterPromotions> w =
				new LambdaQueryWrapper<RegisterPromotions>()
						.eq(RegisterPromotions::getCompanyId, companyId)
						.eq(RegisterPromotions::getRegisterType, "distributor")
						.orderByAsc(RegisterPromotions::getId);
		Page<RegisterPromotions> p = new Page<>(page, pageSize);
		registerPromotionsMapper.selectPage(p, w);
		long totalCount = p.getTotal();

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (RegisterPromotions entity : p.getRecords()) {
			listMaps.add(registerPromotionApiRowBuilder.toRowMap(entity));
		}
		if (!listMaps.isEmpty()) {
			registerPromotionMultiLangReadService.applyAdPicAndTitle(companyId, listMaps, requestLangTag, false);
		}

		for (int i = 0; i < listMaps.size(); i++) {
			Map<String, Object> row = listMaps.get(i);
			attachDistributorPromotionRelations(companyId, row);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", listMaps);
		return out;
	}

	public Object getRegisterInfo(long companyId, String idPath) {
		if (idPath == null || idPath.trim().isEmpty()) {
			return Collections.emptyList();
		}
		String t = idPath.trim();
		long promotionId;
		try {
			promotionId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			return Collections.emptyList();
		}

		LambdaQueryWrapper<RegisterPromotions> w =
				new LambdaQueryWrapper<RegisterPromotions>()
						.eq(RegisterPromotions::getCompanyId, companyId)
						.eq(RegisterPromotions::getRegisterType, "distributor")
						.eq(RegisterPromotions::getId, promotionId);
		RegisterPromotions entity = registerPromotionsMapper.selectOne(w);
		if (entity == null) {
			return Collections.emptyList();
		}

		Map<String, Object> row = registerPromotionApiRowBuilder.toRowMap(entity);
		attachDistributorPromotionRelations(companyId, row);
		return row;
	}

	private void attachDistributorPromotionRelations(long companyId, Map<String, Object> row) {
		long promotionId = promotionIdFromRow(row);
		LambdaQueryWrapper<DistributorPromotions> relW =
				new LambdaQueryWrapper<DistributorPromotions>()
						.eq(DistributorPromotions::getCompanyId, companyId)
						.eq(DistributorPromotions::getPromotionId, promotionId)
						.eq(DistributorPromotions::getPromotionType, "register");
		List<DistributorPromotions> rels = distributorPromotionsMapper.selectList(relW);
		if (rels == null || rels.isEmpty()) {
			return;
		}
		LinkedHashSet<Long> idSet = new LinkedHashSet<>();
		for (DistributorPromotions rel : rels) {
			Long did = rel.getDistributorId();
			if (did != null) {
				idSet.add(did);
			}
		}
		List<Long> distIds = new ArrayList<>(idSet);
		if (distIds.isEmpty()) {
			return;
		}

		LambdaQueryWrapper<Distributor> base =
				new LambdaQueryWrapper<Distributor>()
						.eq(Distributor::getCompanyId, companyId)
						.in(Distributor::getDistributorId, distIds)
						.orderByDesc(Distributor::getCreated);
		int offset = (DISTRIBUTOR_SUBQUERY_PAGE - 1) * DISTRIBUTOR_SUBQUERY_PAGE_SIZE;
		LambdaQueryWrapper<Distributor> limited = base.clone();
		limited.last("LIMIT " + offset + ", " + DISTRIBUTOR_SUBQUERY_PAGE_SIZE);
		List<Distributor> entities = distributorMapper.selectList(limited);

		List<Map<String, Object>> keyLabelList = new ArrayList<>();
		for (Distributor d : entities) {
			long did = d.getDistributorId() == null ? 0L : d.getDistributorId();
			int ds = d.getDistributorSelf() == null ? 0 : d.getDistributorSelf();
			Map<String, Object> setting = selfDeliverySettingReadService.getSetting(companyId, did, ds);
			Map<String, Object> formatted =
					distributorListRowFormatService.formatStoreRow(d, setting, objectMapper);
			LinkedHashMap<String, Object> kl = new LinkedHashMap<>();
			kl.put("key", formatted.get("distributor_id"));
			Object nameVal = formatted.get("name");
			kl.put("label", nameVal != null ? nameVal : "");
			keyLabelList.add(kl);
		}

		LinkedHashMap<String, Object> distWrap = new LinkedHashMap<>();
		distWrap.put("ids", distIds);
		distWrap.put("list", keyLabelList);
		row.put("distributor_id", distWrap);
	}

	private static long promotionIdFromRow(Map<String, Object> row) {
		Object idObj = row.get("id");
		if (idObj instanceof Number n) {
			return n.longValue();
		}
		if (idObj != null) {
			try {
				return Long.parseLong(idObj.toString().trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}
}
