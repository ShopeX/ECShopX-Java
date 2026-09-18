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

package cn.shopex.ecshopx.goods.service.ome;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsCreateOrchestrator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OmeItemsFromOmePersistService {

	private static final Logger log = LoggerFactory.getLogger(OmeItemsFromOmePersistService.class);

	private final ItemsRepository itemsRepository;
	private final OmeItemsFromOmeRowAssembler rowAssembler;
	private final ItemsCreateOrchestrator itemsCreateOrchestrator;
	private final ObjectMapper objectMapper;
	private final OmeItemsSyncQueueLogWriter queueLogWriter;

	public OmeItemsFromOmePersistService(
			ItemsRepository itemsRepository,
			OmeItemsFromOmeRowAssembler rowAssembler,
			ItemsCreateOrchestrator itemsCreateOrchestrator,
			ObjectMapper objectMapper,
			OmeItemsSyncQueueLogWriter queueLogWriter) {
		this.itemsRepository = itemsRepository;
		this.rowAssembler = rowAssembler;
		this.itemsCreateOrchestrator = itemsCreateOrchestrator;
		this.objectMapper = objectMapper;
		this.queueLogWriter = queueLogWriter;
	}

	@Transactional(rollbackFor = Exception.class)
	public void persistOmeGoodsPage(long companyId, List<Map<String, Object>> goodsList, String apiMethod, int pageNo, long endLastmodifyUnix, String goodsBn) {
		for (Map<String, Object> goods : goodsList) {
			long t1 = System.nanoTime();
			try {
				LinkedHashMap<String, Object> goodsBase = new LinkedHashMap<>();
				for (Map.Entry<String, Object> e : goods.entrySet()) {
					if (!"products".equals(e.getKey())) {
						goodsBase.put(e.getKey(), e.getValue());
					}
				}
				List<Map<String, Object>> productRows = coerceProductRows(goods.get("products"));
				final String[] preGoodsBnHolder = new String[] { null };
				AtomicReference<Map<String, Object>> itemInfoRef = new AtomicReference<>(null);
				for (Map<String, Object> row : productRows) {
					LinkedHashMap<String, Object> merged = new LinkedHashMap<>(goodsBase);
					merged.putAll(row);
					String productBn = merged.get("product_bn") != null ? String.valueOf(merged.get("product_bn")) : "";
					Items item = itemsRepository.findByItemBnAndCompany(productBn, companyId);
					Map<String, Object> itemMap = buildItemMap(item);
					rowAssembler.handleRow(companyId, merged, itemInfoRef, itemMap, preGoodsBnHolder);
				}
				Map<String, Object> itemInfo = itemInfoRef.get();
				if (itemInfo == null) {
					continue;
				}
				itemInfo.put("spec_items", objectMapper.writeValueAsString(itemInfo.get("spec_items")));
				itemsCreateOrchestrator.createItems(itemInfo);
			} catch (cn.shopex.ecshopx.common.exception.ResourceException e) {
				queueLogWriter.writeFail(companyId, apiMethod, goods, t1, messageOf(e));
				throw e;
			} catch (Exception e) {
				queueLogWriter.writeFail(companyId, apiMethod, goods, t1, messageOf(e));
				throw new cn.shopex.ecshopx.common.exception.ResourceException(messageOf(e));
			} catch (Throwable t) {
				queueLogWriter.writeFail(companyId, apiMethod, goods, t1, messageOf(t));
				throw new cn.shopex.ecshopx.common.exception.ResourceException(messageOf(t));
			}
		}
	}

	private Map<String, Object> buildItemMap(Items item) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (item == null) {
			m.put("item_id", null);
			m.put("approve_status", "instock");
			m.put("store", 0);
			m.put("consume_type", null);
			m.put("brief", null);
			m.put("sort", null);
			m.put("templates_id", null);
			m.put("is_show_specimg", null);
			m.put("pics", Collections.emptyList());
			m.put("video_type", null);
			m.put("videos", null);
			m.put("intro", null);
			m.put("special_type", null);
			m.put("purchase_agreement", null);
			m.put("enable_agreement", null);
			m.put("item_address_city", null);
			m.put("item_address_province", null);
			m.put("date_type", null);
			m.put("begin_date", null);
			m.put("end_date", null);
			m.put("fixed_term", null);
			m.put("tax_rate", null);
			m.put("crossborder_tax_rate", null);
			m.put("origincountry_id", null);
			m.put("type", null);
			m.put("distributor_id", null);
			m.put("item_source", null);
			m.put("is_gift", null);
			m.put("is_profit", null);
			m.put("profit_type", null);
			m.put("profit_fee", null);
			return m;
		}
		m.put("item_id", item.getItemId());
		m.put("approve_status", item.getApproveStatus());
		m.put("store", item.getStore() != null ? item.getStore() : 0);
		m.put("consume_type", item.getConsumeType());
		m.put("brief", item.getBrief());
		m.put("sort", item.getSort());
		m.put("templates_id", item.getTemplatesId());
		m.put("is_show_specimg", item.getIsShowSpecimg());
		m.put("pics", parsePicsList(item.getPics()));
		m.put("video_type", item.getVideoType());
		m.put("videos", item.getVideos());
		m.put("intro", item.getIntro());
		m.put("special_type", item.getSpecialType());
		m.put("purchase_agreement", item.getPurchaseAgreement());
		m.put("enable_agreement", item.getEnableAgreement());
		m.put("item_address_city", item.getItemAddressCity());
		m.put("item_address_province", item.getItemAddressProvince());
		m.put("date_type", item.getDateType());
		m.put("begin_date", item.getBeginDate());
		m.put("end_date", item.getEndDate());
		m.put("fixed_term", item.getFixedTerm());
		m.put("tax_rate", item.getTaxRate());
		m.put("crossborder_tax_rate", item.getCrossborderTaxRate());
		m.put("origincountry_id", item.getOrigincountryId());
		m.put("type", item.getType());
		m.put("distributor_id", item.getDistributorId());
		m.put("item_source", item.getItemSource());
		m.put("is_gift", item.getIsGift());
		m.put("is_profit", item.getIsProfit());
		m.put("profit_type", item.getProfitType());
		m.put("profit_fee", item.getProfitFee());
		return m;
	}

	private List<Object> parsePicsList(String picsJson) {
		if (picsJson == null || picsJson.isBlank()) {
			return Collections.emptyList();
		}
		try {
			List<Object> list = objectMapper.readValue(picsJson, new TypeReference<List<Object>>() {
			});
			return list != null ? list : Collections.emptyList();
		} catch (Exception e) {
			log.debug("parse pics json failed: {}", e.getMessage());
			return Collections.emptyList();
		}
	}

	private static String messageOf(Throwable t) {
		String m = t.getMessage();
		return m != null && !m.isEmpty() ? m : t.getClass().getSimpleName();
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> coerceProductRows(Object productsObj) {
		if (!(productsObj instanceof List<?> raw)) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : raw) {
			if (o instanceof Map<?, ?> mp) {
				Map<String, Object> row = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : mp.entrySet()) {
					row.put(String.valueOf(e.getKey()), e.getValue());
				}
				out.add(row);
			}
		}
		return out;
	}
}
