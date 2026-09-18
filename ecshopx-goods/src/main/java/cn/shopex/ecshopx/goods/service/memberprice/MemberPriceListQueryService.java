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

package cn.shopex.ecshopx.goods.service.memberprice;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListFacadeService;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeQueryService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeListQueryService;
import cn.shopex.ecshopx.promotions.repository.MemberPriceExportQueryRepository;
import cn.shopex.ecshopx.promotions.support.MemberPriceColumnCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberPriceListQueryService {

	private final ItemsRepository itemsRepository;
	private final GoodsItemsListFacadeService goodsItemsListFacadeService;
	private final MemberPriceExportQueryRepository memberPriceExportQueryRepository;
	private final VipGradeListQueryService vipGradeListQueryService;
	private final MemberCardGradeQueryService memberCardGradeQueryService;
	private final ObjectMapper objectMapper;

	public MemberPriceListQueryService(ItemsRepository itemsRepository,
			GoodsItemsListFacadeService goodsItemsListFacadeService,
			MemberPriceExportQueryRepository memberPriceExportQueryRepository,
			VipGradeListQueryService vipGradeListQueryService,
			MemberCardGradeQueryService memberCardGradeQueryService,
			ObjectMapper objectMapper) {
		this.itemsRepository = itemsRepository;
		this.goodsItemsListFacadeService = goodsItemsListFacadeService;
		this.memberPriceExportQueryRepository = memberPriceExportQueryRepository;
		this.vipGradeListQueryService = vipGradeListQueryService;
		this.memberCardGradeQueryService = memberCardGradeQueryService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getMemberPriceList(long companyId, long itemId, String acceptLanguageHeader) {
		Items head = itemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (head == null) {
			throw new ResourceException("商品获取失败");
		}
		boolean multiSpec = isMultiSpecNospec(head.getNospec());
		Map<String, Object> itemList = goodsItemsListFacadeService.buildAdminPlatformSkuListForMemberPrice(companyId, itemId, multiSpec,
				acceptLanguageHeader);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> rows = (List<Map<String, Object>>) itemList.get("list");
		if (rows == null) {
			rows = new ArrayList<>();
			itemList.put("list", rows);
		}
		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Object iid = row.get("item_id");
			if (iid instanceof Number n) {
				itemIds.add(n.longValue());
			} else if (iid != null && StringUtils.hasText(iid.toString())) {
				try {
					itemIds.add(Long.parseLong(iid.toString().trim()));
				} catch (NumberFormatException ignored) {
					// skip invalid row id
				}
			}
		}
		Map<Long, String> mpriceByItem = memberPriceExportQueryRepository.mapMpriceJsonByItemId(companyId, itemIds);
		Map<String, Object> memberGradeTemplate = buildMemberGradeTemplate(companyId);
		for (Map<String, Object> row : rows) {
			Object iid = row.get("item_id");
			long skuItemId = 0L;
			if (iid instanceof Number n) {
				skuItemId = n.longValue();
			} else if (iid != null && StringUtils.hasText(iid.toString())) {
				try {
					skuItemId = Long.parseLong(iid.toString().trim());
				} catch (NumberFormatException ignored) {
					skuItemId = 0L;
				}
			}
			String raw = skuItemId > 0 ? mpriceByItem.get(skuItemId) : null;
			if (StringUtils.hasText(raw)) {
				var root = MemberPriceColumnCodec.parseRoot(objectMapper, raw);
				if (root != null && root.isObject()) {
					@SuppressWarnings("unchecked")
					Map<String, Object> parsed = objectMapper.convertValue(root, Map.class);
					applyMpriceIntoTemplate(memberGradeTemplate, parsed);
				}
			}
			row.put("memberGrade", memberGradeTemplate);
		}
		return itemList;
	}

	/**
	 * 与仓储层 nospec 语义一致：{@code false} / {@code "false"} / {@code 0} / {@code "0"} 表示多规格。
	 */
	private static boolean isMultiSpecNospec(Object nospec) {
		if (nospec == null) {
			return false;
		}
		if (nospec instanceof Boolean b) {
			return !b;
		}
		if (nospec instanceof Number n) {
			return n.intValue() == 0;
		}
		String s = nospec.toString().trim();
		return "false".equalsIgnoreCase(s) || "0".equals(s);
	}

	private Map<String, Object> buildMemberGradeTemplate(long companyId) {
		Map<String, Object> vipInner = new LinkedHashMap<>();
		for (Map<String, Object> v : vipGradeListQueryService.listDataVipGrade(companyId, true)) {
			if (v == null) {
				continue;
			}
			Object idObj = v.get("vip_grade_id");
			if (idObj == null) {
				continue;
			}
			String key = idKey(idObj);
			Map<String, Object> cell = new LinkedHashMap<>();
			cell.put("vip_grade_id", idObj);
			cell.put("grade_name", v.get("grade_name"));
			cell.put("lv_type", v.get("lv_type"));
			cell.put("mprice", "");
			vipInner.put(key, cell);
		}
		Map<String, Object> gradeInner = new LinkedHashMap<>();
		for (Map<String, Object> g : memberCardGradeQueryService.getGradeListByCompanyId(companyId, false)) {
			if (g == null) {
				continue;
			}
			Object idObj = g.get("grade_id");
			if (idObj == null) {
				continue;
			}
			String key = idKey(idObj);
			Map<String, Object> cell = new LinkedHashMap<>();
			cell.put("vip_grade_id", idObj);
			cell.put("grade_name", g.get("grade_name"));
			cell.put("mprice", "");
			gradeInner.put(key, cell);
		}
		Map<String, Object> memberGrade = new LinkedHashMap<>();
		memberGrade.put("vipGrade", vipInner);
		memberGrade.put("grade", gradeInner);
		return memberGrade;
	}

	private static String idKey(Object idObj) {
		if (idObj instanceof Number n) {
			return String.valueOf(n.longValue());
		}
		return idObj.toString().trim();
	}

	@SuppressWarnings("unchecked")
	private void applyMpriceIntoTemplate(Map<String, Object> memberGradeTemplate, Map<String, Object> parsed) {
		for (Map.Entry<String, Object> top : parsed.entrySet()) {
			String mkey = top.getKey();
			if (!"grade".equals(mkey) && !"vipGrade".equals(mkey)) {
				continue;
			}
			Object mval = top.getValue();
			if (!(mval instanceof Map)) {
				continue;
			}
			Map<String, Object> bucket = (Map<String, Object>) memberGradeTemplate.get(mkey);
			if (bucket == null) {
				continue;
			}
			Map<String, Object> inner = (Map<String, Object>) mval;
			for (Map.Entry<String, Object> ge : inner.entrySet()) {
				String gmkey = ge.getKey();
				Object gmval = ge.getValue();
				Object slotObj = bucket.get(gmkey);
				if (slotObj == null) {
					slotObj = bucket.get(idKey(gmkey));
				}
				if (!(slotObj instanceof Map)) {
					continue;
				}
				((Map<String, Object>) slotObj).put("mprice", gmval == null ? "" : gmval);
			}
		}
	}
}
