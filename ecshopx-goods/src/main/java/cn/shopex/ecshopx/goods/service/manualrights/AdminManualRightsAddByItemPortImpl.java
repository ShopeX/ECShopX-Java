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

package cn.shopex.ecshopx.goods.service.manualrights;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.port.AdminManualRightsAddByItemPort;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsRelType;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTypeRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsMedicineService;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class AdminManualRightsAddByItemPortImpl implements AdminManualRightsAddByItemPort {

	private static final String DATE_TYPE_FIX_TIME_RANGE = "DATE_TYPE_FIX_TIME_RANGE";
	private static final String DATE_TYPE_FIX_TERM = "DATE_TYPE_FIX_TERM";
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private final ItemsMapper itemsMapper;
	private final ItemsRelTypeRepository itemsRelTypeRepository;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ItemsMedicineService itemsMedicineService;
	private final RightsMapper rightsMapper;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate manualRightsTxTemplate;

	public AdminManualRightsAddByItemPortImpl(
			ItemsMapper itemsMapper,
			ItemsRelTypeRepository itemsRelTypeRepository,
			ItemRelAttributesRepository itemRelAttributesRepository,
			ItemsMedicineService itemsMedicineService,
			RightsMapper rightsMapper,
			ObjectMapper objectMapper,
			PlatformTransactionManager transactionManager) {
		this.itemsMapper = itemsMapper;
		this.itemsRelTypeRepository = itemsRelTypeRepository;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.itemsMedicineService = itemsMedicineService;
		this.rightsMapper = rightsMapper;
		this.objectMapper = objectMapper;
		this.manualRightsTxTemplate = new TransactionTemplate(transactionManager);
		this.manualRightsTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@SuppressWarnings("unused")
	@Override
	public void addRightsByItemId(long itemId, long userId, long jwtCompanyId, String mobile, String rightsFrom) {
		Items item = itemsMapper.selectById(itemId);
		if (item == null) {
			throw new ResourceException("商品不存在");
		}

		Map<String, Object> enrichRow = new LinkedHashMap<>();
		enrichRow.put("item_id", item.getItemId());
		enrichRow.put("nospec", item.getNospec());
		enrichRow.put("is_medicine", item.getIsMedicine());
		maybeApplySpecImageToRow(item, enrichRow);
		itemsMedicineService.applyMedicineDataToRows(item.getCompanyId(), List.of(enrichRow));

		String itemType = item.getItemType();
		if (!StringUtils.hasText(itemType)) {
			itemType = "services";
		}
		if (!"services".equals(itemType)) {
			return;
		}

		List<ItemsRelType> relList = new ArrayList<>(itemsRelTypeRepository.listByItemIds(List.of(itemId)));
		relList.sort(Comparator.comparing(ItemsRelType::getCreated, Comparator.nullsLast(Comparator.reverseOrder())));
		List<Map<String, Object>> typeLabels = new ArrayList<>(relList.size());
		for (ItemsRelType r : relList) {
			typeLabels.add(relTypeToLabelMap(r));
		}

		String consume = item.getConsumeType();
		if (consume != null) {
			consume = consume.trim().toLowerCase();
		} else {
			consume = "";
		}
		if ("all".equals(consume)) {
			addRightsConsumeAll(item, userId, mobile, rightsFrom, typeLabels);
		} else if ("every".equals(consume)) {
			addRightsConsumeEvery(item, userId, mobile, rightsFrom, typeLabels);
		}
	}

	private void addRightsConsumeAll(
			Items item, long userId, String mobile, String rightsFrom, List<Map<String, Object>> typeLabels) {
		Integer start;
		Integer end;
		String dt = item.getDateType();
		if (!StringUtils.hasText(dt)) {
			throw new ResourceException("商品权益日期配置不完整");
		}
		if (DATE_TYPE_FIX_TIME_RANGE.equals(dt)) {
			if (item.getBeginDate() == null || item.getEndDate() == null) {
				throw new ResourceException("商品权益日期配置不完整");
			}
			start = item.getBeginDate();
			end = item.getEndDate();
		} else if (DATE_TYPE_FIX_TERM.equals(dt)) {
			if (item.getFixedTerm() == null || item.getFixedTerm() <= 0) {
				throw new ResourceException("商品权益日期配置不完整");
			}
			ZonedDateTime startOfDay = LocalDate.now(SHANGHAI).atStartOfDay(SHANGHAI);
			start = (int) startOfDay.toEpochSecond();
			ZonedDateTime endAt =
					LocalDate.now(SHANGHAI).plusDays(item.getFixedTerm()).atTime(23, 59, 59).atZone(SHANGHAI);
			end = (int) endAt.toEpochSecond();
		} else {
			throw new ResourceException("商品权益日期配置不完整");
		}

		String labelInfosJson = writeLabelInfosJson(typeLabels);
		Rights rights = buildRightsBase(item, userId, mobile, rightsFrom);
		rights.setStartTime(start);
		rights.setEndTime(end);
		rights.setLabelInfos(labelInfosJson);
		rights.setTotalNum(1L);
		rights.setIsNotLimitNum(2);
		rights.setCanReservation(false);
		rights.setCompanyId(item.getCompanyId());
		insertRights(rights);
	}

	private void addRightsConsumeEvery(
			Items item, long userId, String mobile, String rightsFrom, List<Map<String, Object>> typeLabels) {
		ZonedDateTime startOfDay = LocalDate.now(SHANGHAI).atStartOfDay(SHANGHAI);
		int startEpoch = (int) startOfDay.toEpochSecond();
		final long numMultiplier = 1L;
		for (Map<String, Object> v : typeLabels) {
			long limitDays = parseLongFlexible(v.get("limitTime"));
			ZonedDateTime endAt = LocalDate.now(SHANGHAI).plusDays(limitDays).atTime(23, 59, 59).atZone(SHANGHAI);
			int endEpoch = (int) endAt.toEpochSecond();
			int isNotLimit = parseIsNotLimitNumFlexible(v.get("isNotLimitNum"));
			long perLabelNum = parseLongFlexible(v.get("num")) * numMultiplier;
			long totalNum = (isNotLimit != 2) ? 0L : perLabelNum;
			long labelCompanyId = parseLongFlexible(v.get("companyId"));
			String labelInfosJson = writeLabelInfosJson(List.of(v));
			Rights rights = buildRightsBase(item, userId, mobile, rightsFrom);
			rights.setStartTime(startEpoch);
			rights.setEndTime(endEpoch);
			rights.setLabelInfos(labelInfosJson);
			rights.setTotalNum(totalNum);
			rights.setIsNotLimitNum(isNotLimit);
			rights.setCanReservation(true);
			rights.setCompanyId(labelCompanyId > 0L ? labelCompanyId : item.getCompanyId());
			insertRights(rights);
		}
	}

	private void insertRights(Rights rights) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (rights.getCreated() == null) {
			rights.setCreated(now);
		}
		if (rights.getUpdated() == null) {
			rights.setUpdated(now);
		}
		manualRightsTxTemplate.executeWithoutResult(status -> rightsMapper.insert(rights));
	}

	private Rights buildRightsBase(Items item, long userId, String mobile, String rightsFrom) {
		Rights r = new Rights();
		r.setUserId(userId);
		r.setRightsName(item.getItemName());
		r.setRightsSubname("");
		r.setRightsFrom(rightsFrom);
		r.setMobile(mobile);
		r.setTotalConsumNum(0L);
		r.setOrderId(0L);
		r.setStatus("valid");
		r.setOperatorDesc("");
		return r;
	}

	private String writeLabelInfosJson(List<Map<String, Object>> typeLabelMaps) {
		List<Map<String, Object>> payload = new ArrayList<>(typeLabelMaps.size());
		for (Map<String, Object> v : typeLabelMaps) {
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("label_id", parseLongFlexible(v.get("labelId")));
			Object name = v.get("labelName");
			entry.put("label_name", name == null ? "" : String.valueOf(name));
			payload.add(entry);
		}
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	private void maybeApplySpecImageToRow(Items item, Map<String, Object> row) {
		if (!isMultiSpecItem(item)) {
			return;
		}
		List<ItemRelAttributes> attrs = itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(
				item.getCompanyId(), List.of(item.getItemId()), "item_spec");
		for (ItemRelAttributes a : attrs) {
			if (StringUtils.hasText(a.getImageUrl())) {
				row.put("pics", a.getImageUrl());
				break;
			}
		}
	}

	private static boolean isMultiSpecItem(Items item) {
		String n = item.getNospec();
		if (!StringUtils.hasText(n)) {
			return false;
		}
		String t = n.trim().toLowerCase();
		return "false".equals(t) || "0".equals(t);
	}

	private static Map<String, Object> relTypeToLabelMap(ItemsRelType r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("itemId", r.getItemId() != null ? Long.toString(r.getItemId()) : null);
		m.put("labelId", r.getLabelId() != null ? Long.toString(r.getLabelId()) : null);
		m.put("labelName", r.getLabelName());
		m.put("labelPrice", r.getLabelPrice());
		m.put("numType", r.getNumType());
		m.put("num", r.getNum() != null ? Long.toString(r.getNum()) : null);
		m.put("isNotLimitNum", r.getIsNotLimitNum());
		m.put("limitTime", r.getLimitTime() != null ? Long.toString(r.getLimitTime()) : null);
		m.put("companyId", r.getCompanyId() != null ? Long.toString(r.getCompanyId()) : null);
		m.put("created", r.getCreated());
		m.put("updated", r.getUpdated());
		return m;
	}

	private static long parseLongFlexible(Object o) {
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

	private static int parseIsNotLimitNumFlexible(Object o) {
		if (o == null) {
			return 2;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 2;
		}
	}
}
