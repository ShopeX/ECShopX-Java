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

package cn.shopex.ecshopx.orders.service.admin.orderlist;

import cn.shopex.ecshopx.orders.mapper.AdminOrderListSourcesLookupMapper;
import cn.shopex.ecshopx.orders.mapper.AdminOrderListSourcesLookupMapper.SourceNameRow;
import cn.shopex.ecshopx.members.service.admin.MembersContactByUserIdsLookupService;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import cn.shopex.ecshopx.common.orders.port.AdminOrderListSalespersonMirrorPort;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminOrderListPostProcessor {

	private final MembersContactByUserIdsLookupService membersContactByUserIdsLookupService;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final SupplierMapper supplierMapper;
	private final AdminOrderListSourcesLookupMapper sourcesLookupMapper;
	private final AdminOrderListSalespersonMirrorPort salespersonMirrorPort;
	private final AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort;

	public AdminOrderListPostProcessor(
			MembersContactByUserIdsLookupService membersContactByUserIdsLookupService,
			OrderAssociationsMapper orderAssociationsMapper,
			SupplierMapper supplierMapper,
			AdminOrderListSourcesLookupMapper sourcesLookupMapper,
			AdminOrderListSalespersonMirrorPort salespersonMirrorPort,
			AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort) {
		this.membersContactByUserIdsLookupService = membersContactByUserIdsLookupService;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.supplierMapper = supplierMapper;
		this.sourcesLookupMapper = sourcesLookupMapper;
		this.salespersonMirrorPort = salespersonMirrorPort;
		this.adminOrderDetailDistributionSupportPort = adminOrderDetailDistributionSupportPort;
	}

	@SuppressWarnings("unchecked")
	public void afterQuery(long companyId, String orderTypeRaw, Map<String, Object> data) {
		Object listObj = data.get("list");
		if (!(listObj instanceof List<?> rawList) || rawList.isEmpty()) {
			return;
		}
		List<Map<String, Object>> list = (List<Map<String, Object>>) rawList;
		int datapassBlock = parseDatapassInt(data.get("datapass_block"));

		List<Long> userIds =
				list.stream().map(r -> longVal(r.get("user_id"))).filter(id -> id > 0).distinct().toList();
		Map<Long, Map<String, String>> contacts =
				membersContactByUserIdsLookupService.loadDecryptedContactsByUserIds(userIds, 5000);

		if (StringUtils.hasText(orderTypeRaw)) {
			enrichItemsAndSuppliers(companyId, list);
		}

		fillPromoterUserIdsFromAssociations(companyId, list);

		if (StringUtils.hasText(orderTypeRaw)) {
			applySalespersonFirstPass(companyId, list, datapassBlock);
			applySalespersonBatchOverride(companyId, list, datapassBlock);
			attachDistributorFields(companyId, list);
		}

		applySourceNames(list);

		for (Map<String, Object> row : list) {
			applyMemberUsername(row, contacts, datapassBlock);
			applyDatapassRowMasking(row, datapassBlock);
			touchAppInfo(row);
		}
	}

	private static int parseDatapassInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v == null) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private void enrichItemsAndSuppliers(long companyId, List<Map<String, Object>> list) {
		List<Long> supplierPkIds = new ArrayList<>();
		for (Map<String, Object> row : list) {
			Object itemsObj = row.get("items");
			if (!(itemsObj instanceof List<?> il)) {
				continue;
			}
			for (Object it : il) {
				if (it instanceof Map<?, ?> im) {
					Object sid = im.get("supplier_id");
					long id = longVal(sid);
					if (id > 0) {
						supplierPkIds.add(id);
					}
				}
			}
		}
		Map<Long, String> supplierNameById = loadSupplierNames(supplierPkIds);
		for (Map<String, Object> row : list) {
			Object itemsObj = row.get("items");
			if (!(itemsObj instanceof List<?> il)) {
				continue;
			}
			StringBuilder holder = new StringBuilder();
			StringBuilder supNames = new StringBuilder();
			for (Object it : il) {
				if (it instanceof Map<?, ?> im) {
					Object name = im.get("item_name");
					if (name != null && StringUtils.hasText(String.valueOf(name))) {
						if (holder.length() > 0) {
							holder.append(",");
						}
						holder.append(String.valueOf(name).trim());
					}
					long supId = longVal(im.get("supplier_id"));
					if (supId > 0) {
						String sn = supplierNameById.get(supId);
						if (StringUtils.hasText(sn)) {
							if (supNames.length() > 0) {
								supNames.append(",");
							}
							supNames.append(sn);
						}
					}
				}
			}
			if (holder.length() > 0) {
				row.put("item_holder", holder.toString());
			}
			if (supNames.length() > 0) {
				row.put("supplier_name", supNames.toString());
			}
		}
	}

	private Map<Long, String> loadSupplierNames(List<Long> supplierPkIds) {
		if (supplierPkIds.isEmpty()) {
			return Map.of();
		}
		List<Long> distinct = supplierPkIds.stream().distinct().toList();
		Map<Long, String> out = new LinkedHashMap<>();
		for (Long id : distinct) {
			Supplier s = supplierMapper.selectById(id);
			if (s != null && StringUtils.hasText(s.getSupplierName())) {
				out.put(id, s.getSupplierName());
			}
		}
		return out;
	}

	private void fillPromoterUserIdsFromAssociations(long companyId, List<Map<String, Object>> list) {
		List<Long> orderIds =
				list.stream().map(r -> longVal(r.get("order_id"))).filter(id -> id > 0).distinct().toList();
		if (orderIds.isEmpty()) {
			return;
		}
		List<OrderAssociations> asc =
				orderAssociationsMapper.selectList(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.in(OrderAssociations::getOrderId, orderIds));
		Map<Long, Long> promoterByOrder =
				asc.stream()
						.filter(a -> a.getPromoterUserId() != null && a.getPromoterUserId() > 0)
						.collect(
								Collectors.toMap(OrderAssociations::getOrderId, OrderAssociations::getPromoterUserId, (a, b) -> a));
		for (Map<String, Object> row : list) {
			long oid = longVal(row.get("order_id"));
			Long p = promoterByOrder.get(oid);
			if (p != null) {
				row.put("promoter_user_id", p);
			}
		}
	}

	private void applySalespersonFirstPass(long companyId, List<Map<String, Object>> list, int datapassBlock) {
		for (Map<String, Object> row : list) {
			long promoterUid = longVal(row.get("promoter_user_id"));
			if (promoterUid <= 0L) {
				row.put("salesman_info", List.of());
				continue;
			}
			String distKey = String.valueOf(row.getOrDefault("distributor_id", "0"));
			Map<String, Object> spRow =
					salespersonMirrorPort.getSalespersonListForOrderRow(companyId, promoterUid, distKey);
			if (spRow.isEmpty()) {
				row.put("salesman_info", List.of());
				continue;
			}
			Map<String, Object> copy = new LinkedHashMap<>(spRow);
			if (datapassBlock != 0) {
				maskSalespersonMap(copy);
			}
			row.put("salesman_info", List.of(copy));
			row.put("salesman_mobile", spRow.getOrDefault("mobile", ""));
			row.put("salesman_name", spRow.getOrDefault("salesman_name", spRow.get("name")));
			row.put("salesman_user_id", spRow.getOrDefault("user_id", 0));
		}
	}

	private void applySalespersonBatchOverride(long companyId, List<Map<String, Object>> list, int datapassBlock) {
		List<Long> smIds =
				list.stream().map(r -> longVal(r.get("salesman_id"))).filter(id -> id > 0).distinct().toList();
		if (smIds.isEmpty()) {
			return;
		}
		Map<Long, Map<String, Object>> bySp = salespersonMirrorPort.mapBySalespersonId(companyId, smIds);
		for (Map<String, Object> row : list) {
			long sid = longVal(row.get("salesman_id"));
			if (sid <= 0) {
				continue;
			}
			Map<String, Object> sp = bySp.get(sid);
			if (sp == null) {
				continue;
			}
			Map<String, Object> copy = new LinkedHashMap<>(sp);
			if (datapassBlock != 0) {
				maskSalespersonMap(copy);
			}
			row.put("salesman_info", List.of(copy));
		}
	}

	private static void maskSalespersonMap(Map<String, Object> spRow) {
		Object n = spRow.get("name");
		String nameStr = n == null ? null : String.valueOf(n);
		spRow.put("name", DataMasking.maskTruename(nameStr));
		Object mo = spRow.get("mobile");
		String mobileStr = mo == null ? null : String.valueOf(mo);
		spRow.put("mobile", DataMasking.maskMobile(mobileStr));
		Object sn = spRow.get("salesman_name");
		if (sn != null) {
			spRow.put("salesman_name", DataMasking.maskTruename(String.valueOf(sn)));
		}
	}

	private void attachDistributorFields(long companyId, List<Map<String, Object>> list) {
		boolean hasStoreIds = false;
		for (Map<String, Object> row : list) {
			Object raw = row.get("distributor_id");
			if (raw instanceof Number n) {
				if (n.longValue() >= 0L) {
					hasStoreIds = true;
					break;
				}
			} else if (raw != null) {
				try {
					if (Long.parseLong(String.valueOf(raw).trim()) >= 0L) {
						hasStoreIds = true;
						break;
					}
				} catch (NumberFormatException ignored) {
					// skip non-numeric distributor_id
				}
			}
		}
		if (!hasStoreIds) {
			for (Map<String, Object> row : list) {
				row.put("distributor_info", new ArrayList<>());
				row.put("distributor_name", "");
			}
			return;
		}

		Map<String, Object> selfRow =
				new LinkedHashMap<>(
						adminOrderDetailDistributionSupportPort.getDistributorSelfSimpleInfo(companyId));
		Set<Long> positiveIds = new LinkedHashSet<>();
		for (Map<String, Object> row : list) {
			long did = longVal(row.get("distributor_id"));
			if (did > 0L) {
				positiveIds.add(did);
			}
		}
		Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
		for (Long id : positiveIds) {
			Map<String, Object> one =
					adminOrderDetailDistributionSupportPort.getDistributorInfoSimple(
							companyId, String.valueOf(id));
			byId.put(id, one.isEmpty() ? new LinkedHashMap<>() : new LinkedHashMap<>(one));
		}

		for (Map<String, Object> row : list) {
			long did = longVal(row.get("distributor_id"));
			Map<String, Object> info;
			if (did > 0L) {
				info = new LinkedHashMap<>(byId.getOrDefault(did, new LinkedHashMap<>()));
			} else {
				info = new LinkedHashMap<>(selfRow);
			}
			if (info.isEmpty()) {
				row.put("distributor_info", new ArrayList<>());
				row.put("distributor_name", "");
			} else {
				row.put("distributor_info", info);
				Object nm = info.get("name");
				row.put("distributor_name", nm == null ? "" : String.valueOf(nm));
			}
		}
	}

	private void applySourceNames(List<Map<String, Object>> list) {
		List<Long> sids =
				list.stream().map(r -> longVal(r.get("source_id"))).filter(id -> id > 0).distinct().toList();
		if (sids.isEmpty()) {
			return;
		}
		List<SourceNameRow> srcs = sourcesLookupMapper.selectSourceNamesByIds(sids);
		Map<Long, String> names =
				srcs.stream()
						.filter(s -> s.sourceId() != null)
						.collect(
								Collectors.toMap(
										SourceNameRow::sourceId,
										s -> s.sourceName() == null ? "" : s.sourceName(),
										(a, b) -> a));
		for (Map<String, Object> row : list) {
			long sid = longVal(row.get("source_id"));
			if (sid > 0 && names.containsKey(sid)) {
				row.put("source_name", names.get(sid));
			}
		}
	}

	private static void applyMemberUsername(
			Map<String, Object> row, Map<Long, Map<String, String>> contacts, int datapassBlock) {
		long uid = longVal(row.get("user_id"));
		if (uid <= 0) {
			return;
		}
		Map<String, String> c = contacts.get(uid);
		if (c == null) {
			return;
		}
		String username = c.getOrDefault("username", "");
		if (datapassBlock != 0) {
			username = DataMasking.maskTruename(username);
		}
		row.put("username", username);
	}

	/** Splits {@code mobile / name} segments when present, otherwise masks the whole string. */
	private static void maskOperatorDescForRow(Map<String, Object> row) {
		Object raw = row.get("operator_desc");
		if (raw == null) {
			return;
		}
		String s = String.valueOf(raw);
		String sep = " : ";
		int idx = s.indexOf(sep);
		if (idx < 0) {
			sep = ":";
			idx = s.indexOf(sep);
		}
		if (idx > 0 && idx + sep.length() < s.length()) {
			String mobilePart = s.substring(0, idx).trim();
			String namePart = s.substring(idx + sep.length()).trim();
			row.put(
					"operator_desc",
					DataMasking.maskMobile(mobilePart)
							+ " : "
							+ DataMasking.maskTruename(namePart));
		} else {
			row.put("operator_desc", DataMasking.maskTruename(s));
		}
	}

	private static void applyDatapassRowMasking(Map<String, Object> row, int datapassBlock) {
		if (datapassBlock == 0) {
			return;
		}
		Object rn = row.get("receiver_name");
		if (rn != null) {
			row.put("receiver_name", DataMasking.maskTruename(String.valueOf(rn)));
		}
		Object mob = row.get("mobile");
		if (mob != null) {
			row.put("mobile", DataMasking.maskMobile(String.valueOf(mob)));
		}
		Object rm = row.get("receiver_mobile");
		if (rm != null) {
			row.put("receiver_mobile", DataMasking.maskMobile(String.valueOf(rm)));
		}
		Object addr = row.get("receiver_address");
		if (addr != null && StringUtils.hasText(String.valueOf(addr).trim())) {
			row.put("receiver_address", DataMasking.maskAddress(String.valueOf(addr)));
		}
		maskOperatorDescForRow(row);
		row.put("data_masking", true);
		Object app = row.get("app_info");
		if (app instanceof Map<?, ?> am) {
			Object buttons = am.get("buttons");
			if (buttons instanceof List<?> bl) {
				List<Object> nb = new ArrayList<>();
				for (Object b : bl) {
					if (b instanceof Map<?, ?> bm) {
						Object t = bm.get("type");
						if (!"contact".equals(String.valueOf(t))) {
							nb.add(b);
						}
					} else {
						nb.add(b);
					}
				}
				((Map<String, Object>) app).put("buttons", nb);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void touchAppInfo(Map<String, Object> row) {
		Object app = row.get("app_info");
		if (!(app instanceof Map)) {
			return;
		}
		Map<String, Object> am = (Map<String, Object>) app;
		Object buttons = am.get("buttons");
		if (!(buttons instanceof List)) {
			return;
		}
		List<Object> bl = (List<Object>) buttons;
		Map<String, Object> remarkBtn = new LinkedHashMap<>();
		remarkBtn.put("type", "remark");
		remarkBtn.put("name", "备注");
		bl.add(0, remarkBtn);
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
