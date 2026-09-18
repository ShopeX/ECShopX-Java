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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.members.domain.MemberItemsFav;
import cn.shopex.ecshopx.members.mapper.MemberItemsFavMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import cn.shopex.ecshopx.wechat.service.WechatOfficialAccountPermanentMaterialClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemsDetailIntroArticleService {

	private final ObjectMapper objectMapper;
	private final WechatOfficialAccountPermanentMaterialClient wechatMaterialClient;
	private final ItemsRepository itemsRepository;
	private final SupplierItemsRepository supplierItemsRepository;
	private final MemberItemsFavMapper memberItemsFavMapper;

	public ItemsDetailIntroArticleService(
			ObjectMapper objectMapper,
			WechatOfficialAccountPermanentMaterialClient wechatMaterialClient,
			ItemsRepository itemsRepository,
			SupplierItemsRepository supplierItemsRepository,
			MemberItemsFavMapper memberItemsFavMapper) {
		this.objectMapper = objectMapper;
		this.wechatMaterialClient = wechatMaterialClient;
		this.itemsRepository = itemsRepository;
		this.supplierItemsRepository = supplierItemsRepository;
		this.memberItemsFavMapper = memberItemsFavMapper;
	}

	public Object pro(Object intro, String authorizerAppId) {
		return pro(intro, authorizerAppId, 0L, "", 0L);
	}

	/**
	 * @param userId 会员 id；为 0 时不查收藏（与前台匿名详情一致）。
	 * @param dataSource {@code supplier_goods} 时从供货商表补全 goods 组件，否则平台商品表。
	 * @param companyId 用于收藏查询限定公司；为 0 时仅按 user_id + item_id 查询。
	 */
	public Object pro(Object intro, String authorizerAppId, long userId, String dataSource, long companyId) {
		if (intro == null) {
			return null;
		}
		String appId = authorizerAppId == null ? "" : authorizerAppId;
		String ds = dataSource == null ? "" : dataSource;
		ArrayNode arr = parseToMutableArray(intro);
		if (arr == null) {
			return intro;
		}
		List<Long> goodsIds = new ArrayList<>();
		for (int i = 0; i < arr.size(); i++) {
			JsonNode el = arr.get(i);
			if (!el.isObject()) {
				continue;
			}
			ObjectNode block = (ObjectNode) el;
			normalizeBasePadded(block);
			collectGoodsIdsFromBlock(block, goodsIds);
			applyFilmMediaUrls(block, appId);
			normalizeConfig(block);
		}
		List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(goodsIds));
		Map<Long, IntroGoodsRow> itemData = loadItemRows(companyId, uniqueIds, ds);
		Set<Long> favSet = loadFavItemIds(userId, companyId, uniqueIds);
		for (int i = 0; i < arr.size(); i++) {
			JsonNode el = arr.get(i);
			if (!el.isObject()) {
				continue;
			}
			enrichGoodsBlock((ObjectNode) el, itemData, favSet);
		}
		return objectMapper.convertValue(arr, Object.class);
	}

	private ArrayNode parseToMutableArray(Object intro) {
		if (intro instanceof String s) {
			String t = s.trim();
			if (!t.startsWith("[")) {
				return null;
			}
			try {
				JsonNode n = objectMapper.readTree(t);
				if (!n.isArray()) {
					return null;
				}
				return (ArrayNode) n.deepCopy();
			} catch (Exception e) {
				return null;
			}
		}
		if (intro instanceof List<?>) {
			JsonNode n = objectMapper.valueToTree(intro);
			if (!n.isArray()) {
				return null;
			}
			return (ArrayNode) n.deepCopy();
		}
		return null;
	}

	private void normalizeBasePadded(ObjectNode block) {
		JsonNode base = block.path("base");
		if (!base.isObject()) {
			return;
		}
		ObjectNode b = (ObjectNode) base;
		if (b.has("padded")) {
			b.set("padded", editorTruthyToBooleanNode(b.get("padded")));
		}
	}

	private void collectGoodsIdsFromBlock(ObjectNode block, List<Long> goodsIds) {
		if (!"goods".equals(blockName(block))) {
			return;
		}
		JsonNode data = block.path("data");
		if (!data.isArray() || data.isEmpty()) {
			return;
		}
		for (JsonNode item : data) {
			if (!item.isObject()) {
				continue;
			}
			long id = parseLongId(((ObjectNode) item).get("item_id"));
			if (id > 0L) {
				goodsIds.add(id);
			}
		}
	}

	private void applyFilmMediaUrls(ObjectNode block, String authorizerAppId) {
		if (!"film".equals(blockName(block))) {
			return;
		}
		JsonNode data = block.path("data");
		if (!data.isArray()) {
			return;
		}
		ArrayNode dataArr = (ArrayNode) data;
		for (int k = 0; k < dataArr.size(); k++) {
			JsonNode val = dataArr.get(k);
			if (!val.isObject()) {
				continue;
			}
			ObjectNode valObj = (ObjectNode) val;
			if (!valObj.has("media_id")) {
				continue;
			}
			String mediaId = valObj.get("media_id").asText("");
			wechatMaterialClient.resolveMaterialDownloadUrl(authorizerAppId, mediaId).ifPresent(url -> valObj.put("url", url));
		}
	}

	private void normalizeConfig(ObjectNode block) {
		JsonNode cfg = block.path("config");
		if (!cfg.isObject()) {
			return;
		}
		ObjectNode c = (ObjectNode) cfg;
		normalizeConfigBoolean(c, "content");
		normalizeConfigBoolean(c, "dot");
		normalizeConfigBoolean(c, "dotCover");
		normalizeConfigInt(c, "height");
		normalizeConfigInt(c, "interval");
		normalizeConfigBoolean(c, "padded");
		normalizeConfigBoolean(c, "rounded");
		normalizeConfigBoolean(c, "bold");
		normalizeConfigBoolean(c, "italic");
	}

	private void normalizeConfigBoolean(ObjectNode config, String key) {
		if (!config.has(key)) {
			return;
		}
		config.set(key, editorTruthyToBooleanNode(config.get(key)));
	}

	private void normalizeConfigInt(ObjectNode config, String key) {
		if (!config.has(key)) {
			return;
		}
		JsonNode v = config.get(key);
		int iv;
		if (v.isNumber()) {
			iv = v.intValue();
		} else {
			try {
				iv = Integer.parseInt(v.asText("0").trim());
			} catch (NumberFormatException e) {
				iv = 0;
			}
		}
		config.put(key, iv);
	}

	private static JsonNode editorTruthyToBooleanNode(JsonNode v) {
		if (v == null || v.isNull() || v.isMissingNode()) {
			return BooleanNode.FALSE;
		}
		if (v.isBoolean()) {
			return v.booleanValue() ? BooleanNode.TRUE : BooleanNode.FALSE;
		}
		if (v.isTextual()) {
			String s = v.asText();
			if (s.isEmpty() || "false".equalsIgnoreCase(s) || "0".equals(s)) {
				return BooleanNode.FALSE;
			}
			return BooleanNode.TRUE;
		}
		if (v.isNumber()) {
			return v.asInt() == 0 ? BooleanNode.FALSE : BooleanNode.TRUE;
		}
		return BooleanNode.TRUE;
	}

	private void enrichGoodsBlock(ObjectNode block, Map<Long, IntroGoodsRow> itemData, Set<Long> favSet) {
		if (!"goods".equals(blockName(block))) {
			return;
		}
		JsonNode data = block.path("data");
		if (!data.isArray() || data.isEmpty() || itemData.isEmpty()) {
			return;
		}
		ArrayNode dataArr = (ArrayNode) data;
		for (int k = 0; k < dataArr.size(); k++) {
			JsonNode item = dataArr.get(k);
			if (!item.isObject()) {
				continue;
			}
			ObjectNode itemObj = (ObjectNode) item;
			long iid = parseLongId(itemObj.get("item_id"));
			IntroGoodsRow row = itemData.get(iid);
			if (row == null) {
				continue;
			}
			itemObj.put("item_name", row.itemName());
			itemObj.put("img_url", row.imgUrl());
			itemObj.put("price", row.price() != null ? row.price() : 0);
			itemObj.put("sales", row.sales());
			itemObj.put("favStatus", favSet.contains(iid));
			itemObj.put("itemStatus", "onsale".equals(row.approveStatus()));
		}
	}

	private Map<Long, IntroGoodsRow> loadItemRows(long companyId, List<Long> itemIds, String dataSource) {
		if (itemIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, IntroGoodsRow> map = new LinkedHashMap<>();
		if ("supplier_goods".equals(dataSource)) {
			List<SupplierItems> rows = supplierItemsRepository.listByCompanyAndItemIds(companyId, itemIds);
			for (SupplierItems s : rows) {
				map.put(s.getItemId(), IntroGoodsRow.fromSupplier(s, this));
			}
		} else {
			List<Items> rows = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, itemIds);
			for (Items it : rows) {
				map.put(it.getItemId(), IntroGoodsRow.fromPlatform(it, this));
			}
		}
		return map;
	}

	private Set<Long> loadFavItemIds(long userId, long companyId, List<Long> itemIds) {
		if (userId <= 0L || itemIds.isEmpty()) {
			return Set.of();
		}
		LambdaQueryWrapper<MemberItemsFav> w = new LambdaQueryWrapper<>();
		w.eq(MemberItemsFav::getUserId, userId).in(MemberItemsFav::getItemId, itemIds);
		if (companyId > 0L) {
			w.eq(MemberItemsFav::getCompanyId, companyId);
		}
		List<MemberItemsFav> rows = memberItemsFavMapper.selectList(w);
		return rows.stream().map(MemberItemsFav::getItemId).collect(Collectors.toSet());
	}

	private static String blockName(ObjectNode block) {
		JsonNode n = block.get("name");
		return n == null || n.isNull() ? "" : n.asText("");
	}

	private static long parseLongId(JsonNode n) {
		if (n == null || n.isNull()) {
			return 0L;
		}
		if (n.isNumber()) {
			return n.asLong();
		}
		String s = n.asText("").trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	String firstPicFromPicsJson(String picsJson) {
		if (!StringUtils.hasText(picsJson)) {
			return "";
		}
		try {
			JsonNode root = objectMapper.readTree(picsJson.trim());
			if (root.isArray() && root.size() > 0) {
				JsonNode first = root.get(0);
				if (first.isTextual()) {
					return first.asText("");
				}
			}
		} catch (Exception ignored) {
		}
		return "";
	}

	private record IntroGoodsRow(String itemName, String imgUrl, Integer price, int sales, String approveStatus) {
		static IntroGoodsRow fromPlatform(Items it, ItemsDetailIntroArticleService self) {
			String name = it.getItemName() != null ? it.getItemName() : "";
			String pic = self.firstPicFromPicsJson(it.getPics());
			Integer priceVal = it.getPrice() != null ? it.getPrice() : 0;
			int sales = it.getSales() != null ? it.getSales() : 0;
			String ap = it.getApproveStatus() != null ? it.getApproveStatus() : "";
			return new IntroGoodsRow(name, pic, priceVal, sales, ap);
		}

		static IntroGoodsRow fromSupplier(SupplierItems s, ItemsDetailIntroArticleService self) {
			String name = s.getItemName() != null ? s.getItemName() : "";
			String pic = self.firstPicFromPicsJson(s.getPics());
			Integer priceVal = s.getPrice() != null ? s.getPrice() : 0;
			int sales = s.getSales() != null ? s.getSales() : 0;
			String ap = s.getApproveStatus() != null ? s.getApproveStatus() : "";
			return new IntroGoodsRow(name, pic, priceVal, sales, ap);
		}
	}
}
