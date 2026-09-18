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

package cn.shopex.ecshopx.companys.service.operatorpending;

import cn.shopex.ecshopx.companys.domain.OperatorCart;
import cn.shopex.ecshopx.companys.domain.OperatorPendingOrder;
import cn.shopex.ecshopx.companys.mapper.OperatorCartMapper;
import cn.shopex.ecshopx.companys.mapper.OperatorPendingOrderMapper;
import cn.shopex.ecshopx.common.companys.operatorpending.OperatorPendingListItemsEnrichmentPort;
import cn.shopex.ecshopx.common.companys.operatorpending.OperatorPendingListMemberLookupPort;
import cn.shopex.ecshopx.common.companys.operatorpending.PendingListItemsBundle;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperatorPendingOrderService {

	private static final String SHOP_TYPE_OFFLINE = "shop_offline";

	private final OperatorCartMapper operatorCartMapper;
	private final OperatorPendingOrderMapper operatorPendingOrderMapper;
	private final ObjectMapper objectMapper;
	private final OperatorPendingListMemberLookupPort operatorPendingListMemberLookupPort;
	private final OperatorPendingListItemsEnrichmentPort operatorPendingListItemsEnrichmentPort;

	public OperatorPendingOrderService(
			OperatorCartMapper operatorCartMapper,
			OperatorPendingOrderMapper operatorPendingOrderMapper,
			ObjectMapper objectMapper,
			OperatorPendingListMemberLookupPort operatorPendingListMemberLookupPort,
			OperatorPendingListItemsEnrichmentPort operatorPendingListItemsEnrichmentPort) {
		this.operatorCartMapper = operatorCartMapper;
		this.operatorPendingOrderMapper = operatorPendingOrderMapper;
		this.objectMapper = objectMapper;
		this.operatorPendingListMemberLookupPort = operatorPendingListMemberLookupPort;
		this.operatorPendingListItemsEnrichmentPort = operatorPendingListItemsEnrichmentPort;
	}

	public Map<String, Object> listPendingData(
			long companyId, long operatorId, long distributorId, int page, int pageSize) {
		LambdaQueryWrapper<OperatorPendingOrder> w = new LambdaQueryWrapper<>();
		w.eq(OperatorPendingOrder::getCompanyId, companyId)
				.eq(OperatorPendingOrder::getOperatorId, operatorId)
				.eq(OperatorPendingOrder::getDistributorId, distributorId)
				.orderByDesc(OperatorPendingOrder::getCreated);
		Page<OperatorPendingOrder> p = new Page<>(page, pageSize);
		operatorPendingOrderMapper.selectPage(p, w);
		long total = p.getTotal();
		List<OperatorPendingOrder> rows = p.getRecords();

		List<Long> userIdList = new ArrayList<>();
		LinkedHashSet<Long> uidDedup = new LinkedHashSet<>();
		for (OperatorPendingOrder r : rows) {
			Long uid = r.getUserId();
			if (uid != null && uid > 0L && uidDedup.add(uid)) {
				userIdList.add(uid);
			}
		}
		Map<Long, Map<String, Object>> memberByUserId = Collections.emptyMap();
		if (!userIdList.isEmpty()) {
			memberByUserId =
					operatorPendingListMemberLookupPort.listPendingDataMemberRows(companyId, userIdList, pageSize);
		}

		List<Map<String, Object>> listMaps = new ArrayList<>();
		List<Long> globalItemIdsAppendOrder = new ArrayList<>();
		for (OperatorPendingOrder entity : rows) {
			LinkedHashMap<String, Object> line = new LinkedHashMap<>();
			line.put("pending_id", entity.getPendingId());
			line.put("company_id", entity.getCompanyId());
			line.put("distributor_id", entity.getDistributorId());
			line.put("operator_id", entity.getOperatorId());
			line.put("user_id", entity.getUserId());
			line.put("pending_type", entity.getPendingType());
			line.put("created", entity.getCreated());
			line.put("updated", entity.getUpdated());

			String raw = entity.getPendingData();
			if (raw == null || raw.isBlank()) {
				line.put("pending_data", List.of());
				line.put("total_num", Integer.valueOf(0));
			} else {
				try {
					JsonNode root = objectMapper.readTree(raw);
					if (root.isArray()) {
						List<Map<String, Object>> pendingLines =
								objectMapper.convertValue(root, new TypeReference<List<Map<String, Object>>>() {});
						line.put("pending_data", pendingLines);
						for (Map<String, Object> v : pendingLines) {
							long itemId = parsePositiveItemIdLoose(v.get("item_id"));
							if (itemId > 0L) {
								globalItemIdsAppendOrder.add(Long.valueOf(itemId));
							}
						}
					} else if (root.isObject()) {
						Map<String, Object> orderMap =
								objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
						line.put("pending_data", orderMap);
						line.put("total_num", Integer.valueOf(0));
					} else {
						throw new BadRequestException("挂单数据格式错误");
					}
				} catch (JsonProcessingException e) {
					throw new BadRequestException("挂单数据格式错误");
				}
			}

			long userId = entity.getUserId() == null ? 0L : entity.getUserId().longValue();
			if (userId > 0L && memberByUserId.containsKey(Long.valueOf(userId))) {
				line.put("memberInfo", memberByUserId.get(Long.valueOf(userId)));
			}
			listMaps.add(line);
		}

		List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(globalItemIdsAppendOrder));
		Map<Long, Map<String, Object>> itemByItemId = Collections.emptyMap();
		Map<Long, List<Map<String, Object>>> specLinesByItemId = Collections.emptyMap();
		if (!distinctIds.isEmpty()) {
			PendingListItemsBundle bundle =
					operatorPendingListItemsEnrichmentPort.loadForPendingList(companyId, distinctIds);
			itemByItemId = bundle.itemByItemId();
			specLinesByItemId = bundle.specLinesByItemId();
		}

		for (Map<String, Object> line : listMaps) {
			Object pd = line.get("pending_data");
			if (!(pd instanceof List<?>)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> pendingLines = (List<Map<String, Object>>) pd;
			int totalNum = 0;
			for (int i = 0; i < pendingLines.size(); i++) {
				Map<String, Object> v = pendingLines.get(i);
				long rawNum = parseLooseLong(v.get("num"));
				totalNum += rawNum;
				long itemId = parsePositiveItemIdLoose(v.get("item_id"));
				if (itemId > 0L && itemByItemId.containsKey(Long.valueOf(itemId))) {
					LinkedHashMap<String, Object> merged = new LinkedHashMap<>(v);
					merged.putAll(itemByItemId.get(Long.valueOf(itemId)));
					List<Map<String, Object>> specLines = specLinesByItemId.get(Long.valueOf(itemId));
					if (specLines != null && !specLines.isEmpty()) {
						merged.put("item_spec_desc", joinItemSpecDesc(specLines));
					}
					pendingLines.set(i, merged);
				}
			}
			line.put("total_num", Integer.valueOf(totalNum));
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", Long.valueOf(total));
		out.put("list", listMaps);
		return out;
	}

	private static long parseLooseLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parsePositiveItemIdLoose(Object o) {
		long v = parseLooseLong(o);
		return v > 0L ? v : 0L;
	}

	private static String joinItemSpecDesc(List<Map<String, Object>> specLines) {
		if (specLines == null || specLines.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Map<String, Object> m : specLines) {
			Object sn = m.get("spec_name");
			Object svn = m.get("spec_value_name");
			String specName = sn == null ? "" : sn.toString();
			String specValueName = svn == null ? "" : svn.toString();
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(specName).append(':').append(specValueName);
		}
		return sb.toString();
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> pendingCartData(long companyId, long operatorId, long distributorId, long userId) {
		OperatorPendingOrder inserted =
				snapshotCurrentCartAsPendingIfNonEmpty(companyId, operatorId, distributorId, userId, false);
		return toPendingResultMap(inserted);
	}

	/**
	 * @return inserted pending row when a non-empty cart was snapshotted; {@code null} when {@code ifFetch} and cart
	 *     was empty (no DB writes)
	 */
	private OperatorPendingOrder snapshotCurrentCartAsPendingIfNonEmpty(
			long companyId, long operatorId, long distributorId, long userId, boolean ifFetch) {
		LambdaQueryWrapper<OperatorCart> cartWrapper = new LambdaQueryWrapper<>();
		cartWrapper
				.eq(OperatorCart::getOperatorId, operatorId)
				.eq(OperatorCart::getCompanyId, companyId)
				.eq(OperatorCart::getDistributorId, distributorId);

		List<OperatorCart> list = operatorCartMapper.selectList(cartWrapper);
		if (list == null || list.isEmpty()) {
			if (ifFetch) {
				return null;
			}
			throw new ResourceException("购物车为空");
		}

		List<Map<String, Object>> cartRowsAsListOfMaps = new ArrayList<>(list.size());
		for (OperatorCart row : list) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("cart_id", row.getCartId());
			m.put("company_id", row.getCompanyId());
			m.put("distributor_id", row.getDistributorId());
			m.put("operator_id", row.getOperatorId());
			m.put("item_id", row.getItemId());
			m.put("num", row.getNum());
			m.put("is_checked", Boolean.TRUE.equals(row.getIsChecked()) ? Integer.valueOf(1) : Integer.valueOf(0));
			m.put("special_type", row.getSpecialType());
			Long shopId = row.getDistributorId();
			m.put("shop_id", shopId);
			m.put("shop_type", SHOP_TYPE_OFFLINE);
			cartRowsAsListOfMaps.add(m);
		}

		if (!ifFetch) {
			LambdaQueryWrapper<OperatorPendingOrder> pendingWrapper = new LambdaQueryWrapper<>();
			pendingWrapper
					.eq(OperatorPendingOrder::getOperatorId, operatorId)
					.eq(OperatorPendingOrder::getCompanyId, companyId)
					.eq(OperatorPendingOrder::getDistributorId, distributorId);

			long cnt = operatorPendingOrderMapper.selectCount(pendingWrapper);
			if (cnt >= 10L) {
				throw new ResourceException("挂单已达到上限，请清理挂单", 422, 42201);
			}
		}

		OperatorPendingOrder entity = new OperatorPendingOrder();
		entity.setCompanyId(companyId);
		entity.setDistributorId(distributorId);
		entity.setUserId(userId);
		entity.setOperatorId(operatorId);
		entity.setPendingType("cart");
		try {
			entity.setPendingData(objectMapper.writeValueAsString(cartRowsAsListOfMaps));
		} catch (JsonProcessingException e) {
			throw new ResourceException("挂单数据序列化失败");
		}
		Integer nowSec = Integer.valueOf((int) Instant.now().getEpochSecond());
		entity.setCreated(nowSec);
		entity.setUpdated(nowSec);

		operatorPendingOrderMapper.insert(entity);

		operatorCartMapper.delete(cartWrapper);

		return entity;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> pendingOrderData(
			long companyId, long operatorId, long distributorId, long userId, String orderId) {
		OperatorPendingOrder entity = new OperatorPendingOrder();
		entity.setCompanyId(companyId);
		entity.setDistributorId(distributorId);
		entity.setUserId(userId);
		entity.setOperatorId(operatorId);
		entity.setPendingType("order");
		LinkedHashMap<String, Object> pendingPayload = new LinkedHashMap<>();
		pendingPayload.put("order_id", orderId);
		try {
			entity.setPendingData(objectMapper.writeValueAsString(pendingPayload));
		} catch (JsonProcessingException e) {
			throw new ResourceException("挂单数据序列化失败");
		}
		Integer nowSec = Integer.valueOf((int) Instant.now().getEpochSecond());
		entity.setCreated(nowSec);
		entity.setUpdated(nowSec);
		operatorPendingOrderMapper.insert(entity);
		return toPendingResultMap(entity);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> fetchPendingData(
			long companyId, long operatorId, long distributorId, long userId, long pendingId) {
		LambdaQueryWrapper<OperatorPendingOrder> wrapper = new LambdaQueryWrapper<>();
		wrapper
				.eq(OperatorPendingOrder::getCompanyId, companyId)
				.eq(OperatorPendingOrder::getDistributorId, distributorId)
				.eq(OperatorPendingOrder::getOperatorId, operatorId)
				.eq(OperatorPendingOrder::getPendingId, pendingId);

		OperatorPendingOrder row = operatorPendingOrderMapper.selectOne(wrapper);
		if (row == null) {
			throw new ResourceException("挂单数据为空");
		}

		String rawPendingData = row.getPendingData();
		if (rawPendingData == null || rawPendingData.isBlank()) {
			throw new ResourceException("挂单数据异常");
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(rawPendingData);
		} catch (JsonProcessingException e) {
			throw new ResourceException("挂单数据异常");
		}

		String pendingType = row.getPendingType();
		String typeNorm = pendingType == null ? "" : pendingType.trim();
		if (Objects.equals("order", typeNorm)) {
			if (!root.isObject()) {
				throw new ResourceException("挂单数据异常");
			}
			Map<String, Object> orderMap =
					objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
			return buildPendingFetchResultMap(row, orderMap);
		}

		snapshotCurrentCartAsPendingIfNonEmpty(companyId, operatorId, distributorId, userId, true);

		if (!root.isArray()) {
			throw new ResourceException("挂单数据异常");
		}

		for (JsonNode item : root) {
			if (!item.isObject()) {
				throw new ResourceException("挂单数据异常");
			}
			OperatorCart cart = operatorCartFromPendingJsonRow(item, companyId, distributorId, operatorId);
			operatorCartMapper.insert(cart);
		}

		operatorPendingOrderMapper.delete(wrapper);

		Object decodedList = objectMapper.convertValue(root, Object.class);
		return buildPendingFetchResultMap(row, decodedList);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> deletePendingByFilter(long companyId, long operatorId, long pendingId) {
		LambdaQueryWrapper<OperatorPendingOrder> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(OperatorPendingOrder::getCompanyId, companyId)
				.eq(OperatorPendingOrder::getOperatorId, operatorId)
				.eq(OperatorPendingOrder::getPendingId, pendingId);
		operatorPendingOrderMapper.delete(wrapper);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("status", Boolean.TRUE);
		return out;
	}

	private static Map<String, Object> buildPendingFetchResultMap(OperatorPendingOrder row, Object decodedPendingData) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("pending_id", row.getPendingId());
		out.put("company_id", row.getCompanyId());
		out.put("distributor_id", row.getDistributorId());
		out.put("operator_id", row.getOperatorId());
		out.put("user_id", row.getUserId());
		out.put("pending_type", row.getPendingType());
		out.put("pending_data", decodedPendingData);
		out.put("created", row.getCreated());
		out.put("updated", row.getUpdated());
		return out;
	}

	private OperatorCart operatorCartFromPendingJsonRow(
			JsonNode row, long companyId, long distributorId, long operatorId) {
		OperatorCart cart = new OperatorCart();
		cart.setCartId(parseOptionalPositiveCartId(row));
		applyTriStateLong(row, "company_id", companyId, cart::setCompanyId);
		applyTriStateLong(row, "distributor_id", distributorId, cart::setDistributorId);
		applyTriStateLong(row, "operator_id", operatorId, cart::setOperatorId);
		cart.setItemId(requirePositiveLongField(row, "item_id"));
		cart.setNum(requireNonNegativeLongField(row, "num"));
		cart.setIsChecked(parseIsChecked(row));
		cart.setSpecialType(parseSpecialType(row));
		return cart;
	}

	private Long parseOptionalPositiveCartId(JsonNode row) {
		if (!row.has("cart_id")) {
			return null;
		}
		JsonNode n = row.get("cart_id");
		if (n == null || n.isNull()) {
			return null;
		}
		long v = parseLongNode(n);
		if (v <= 0L) {
			return null;
		}
		return Long.valueOf(v);
	}

	private void applyTriStateLong(JsonNode row, String key, long fallback, Consumer<Long> setter) {
		if (!row.has(key)) {
			setter.accept(Long.valueOf(fallback));
			return;
		}
		JsonNode n = row.get(key);
		if (n.isNull()) {
			setter.accept(null);
			return;
		}
		setter.accept(Long.valueOf(parseLongNode(n)));
	}

	private long parseLongNode(JsonNode n) {
		if (n.isIntegralNumber()) {
			return n.longValue();
		}
		if (n.isNumber()) {
			return n.longValue();
		}
		if (n.isTextual()) {
			String s = n.asText().trim();
			try {
				return Long.parseLong(s);
			} catch (NumberFormatException e) {
				throw new ResourceException("挂单数据异常");
			}
		}
		throw new ResourceException("挂单数据异常");
	}

	private long requirePositiveLongField(JsonNode row, String key) {
		if (!row.has(key) || row.get(key).isNull()) {
			throw new ResourceException("挂单数据异常");
		}
		long v = parseLongNode(row.get(key));
		if (v <= 0L) {
			throw new ResourceException("挂单数据异常");
		}
		return v;
	}

	private long requireNonNegativeLongField(JsonNode row, String key) {
		if (!row.has(key) || row.get(key).isNull()) {
			throw new ResourceException("挂单数据异常");
		}
		long v = parseLongNode(row.get(key));
		if (v < 0L) {
			throw new ResourceException("挂单数据异常");
		}
		return v;
	}

	private static Boolean parseIsChecked(JsonNode row) {
		if (!row.has("is_checked") || row.get("is_checked").isNull()) {
			return Boolean.TRUE;
		}
		JsonNode n = row.get("is_checked");
		if (n.isBoolean()) {
			return Boolean.valueOf(n.booleanValue());
		}
		if (n.isIntegralNumber()) {
			return Boolean.valueOf(n.intValue() != 0);
		}
		if (n.isTextual()) {
			String s = n.asText().trim();
			if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
				return Boolean.TRUE;
			}
			if ("0".equals(s) || "false".equalsIgnoreCase(s)) {
				return Boolean.FALSE;
			}
			try {
				return Boolean.valueOf(Long.parseLong(s) != 0L);
			} catch (NumberFormatException e) {
				throw new ResourceException("挂单数据异常");
			}
		}
		throw new ResourceException("挂单数据异常");
	}

	private static String parseSpecialType(JsonNode row) {
		if (!row.has("special_type") || row.get("special_type").isNull()) {
			return "normal";
		}
		JsonNode n = row.get("special_type");
		String s = n.isTextual() ? n.asText().trim() : n.asText();
		if (s.isEmpty()) {
			return "normal";
		}
		return s;
	}

	private static Map<String, Object> toPendingResultMap(OperatorPendingOrder entity) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("pending_id", entity.getPendingId());
		out.put("company_id", entity.getCompanyId());
		out.put("distributor_id", entity.getDistributorId());
		out.put("operator_id", entity.getOperatorId());
		out.put("user_id", entity.getUserId());
		out.put("pending_type", entity.getPendingType());
		out.put("pending_data", entity.getPendingData());
		out.put("created", entity.getCreated());
		out.put("updated", entity.getUpdated());
		return out;
	}
}
