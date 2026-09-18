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

package cn.shopex.ecshopx.goods.service.recommend;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendRule;
import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendRuleMainItem;
import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendRuleRecommendItem;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.mapper.recommend.GoodsRecommendRuleMainItemMapper;
import cn.shopex.ecshopx.goods.mapper.recommend.GoodsRecommendRuleMapper;
import cn.shopex.ecshopx.goods.mapper.recommend.GoodsRecommendRuleRecommendItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GoodsRecommendRuleAdminService {

	private static final int DEFAULT_PAGE_SIZE = 20;

	private static final int MAX_PAGE_SIZE = 100;

	private final GoodsRecommendRuleMapper ruleMapper;

	private final GoodsRecommendRuleMainItemMapper mainItemMapper;

	private final GoodsRecommendRuleRecommendItemMapper recommendItemMapper;

	private final ItemsMapper itemsMapper;

	private final GoodsRecommendGoodsIdResolver goodsIdResolver;

	private final MessageSource messageSource;

	public GoodsRecommendRuleAdminService(
			GoodsRecommendRuleMapper ruleMapper,
			GoodsRecommendRuleMainItemMapper mainItemMapper,
			GoodsRecommendRuleRecommendItemMapper recommendItemMapper,
			ItemsMapper itemsMapper,
			GoodsRecommendGoodsIdResolver goodsIdResolver,
			MessageSource messageSource) {
		this.ruleMapper = ruleMapper;
		this.mainItemMapper = mainItemMapper;
		this.recommendItemMapper = recommendItemMapper;
		this.itemsMapper = itemsMapper;
		this.goodsIdResolver = goodsIdResolver;
		this.messageSource = messageSource;
	}

	public Map<String, Object> listRules(long companyId, int page, int pageSize, String keyword) {
		int p = Math.max(1, page);
		int ps = Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize));
		LambdaQueryWrapper<GoodsRecommendRule> wrapper =
				new LambdaQueryWrapper<GoodsRecommendRule>()
						.eq(GoodsRecommendRule::getCompanyId, companyId)
						.orderByDesc(GoodsRecommendRule::getCreated);
		if (StringUtils.hasText(keyword)) {
			wrapper.apply("LOWER(rule_name) LIKE {0}", "%" + keyword.trim().toLowerCase() + "%");
		}
		Page<GoodsRecommendRule> result = ruleMapper.selectPage(new Page<>(p, ps), wrapper);
		List<Map<String, Object>> list = new ArrayList<>();
		for (GoodsRecommendRule rule : result.getRecords()) {
			list.add(toListRow(rule, companyId));
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total", result.getTotal());
		out.put("page", p);
		out.put("pageSize", ps);
		return out;
	}

	public Map<String, Object> getRuleDetail(
			long companyId,
			long ruleId,
			int mainPage,
			int mainPageSize,
			int recommendPage,
			int recommendPageSize) {
		GoodsRecommendRule rule = requireRule(companyId, ruleId);
		int mp = Math.max(1, mainPage);
		int mps = boundedPageSize(mainPageSize);
		int rp = Math.max(1, recommendPage);
		int rps = boundedPageSize(recommendPageSize);

		long mainTotal =
				mainItemMapper.selectCount(
						new LambdaQueryWrapper<GoodsRecommendRuleMainItem>()
								.eq(GoodsRecommendRuleMainItem::getCompanyId, companyId)
								.eq(GoodsRecommendRuleMainItem::getRuleId, ruleId));
		long recommendTotal =
				recommendItemMapper.selectCount(
						new LambdaQueryWrapper<GoodsRecommendRuleRecommendItem>()
								.eq(GoodsRecommendRuleRecommendItem::getCompanyId, companyId)
								.eq(GoodsRecommendRuleRecommendItem::getRuleId, ruleId));

		Page<GoodsRecommendRuleMainItem> mainPageResult =
				mainItemMapper.selectPage(
						new Page<>(mp, mps),
						new LambdaQueryWrapper<GoodsRecommendRuleMainItem>()
								.eq(GoodsRecommendRuleMainItem::getCompanyId, companyId)
								.eq(GoodsRecommendRuleMainItem::getRuleId, ruleId)
								.orderByAsc(GoodsRecommendRuleMainItem::getId));
		Page<GoodsRecommendRuleRecommendItem> recommendPageResult =
				recommendItemMapper.selectPage(
						new Page<>(rp, rps),
						new LambdaQueryWrapper<GoodsRecommendRuleRecommendItem>()
								.eq(GoodsRecommendRuleRecommendItem::getCompanyId, companyId)
								.eq(GoodsRecommendRuleRecommendItem::getRuleId, ruleId)
								.orderByAsc(GoodsRecommendRuleRecommendItem::getSort)
								.orderByAsc(GoodsRecommendRuleRecommendItem::getId));

		Map<Long, Items> itemsById =
				loadItemsForRuleRows(companyId, mainPageResult.getRecords(), recommendPageResult.getRecords());

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", rule.getId());
		out.put("rule_name", rule.getRuleName());
		out.put("created", rule.getCreated());
		out.put("main_total", mainTotal);
		out.put("recommend_total", recommendTotal);
		out.put("main_page", mp);
		out.put("main_page_size", mps);
		out.put("recommend_page", rp);
		out.put("recommend_page_size", rps);
		out.put("main_items", toItemViews(mainPageResult.getRecords(), itemsById));
		out.put("recommend_items", toRecommendItemViews(recommendPageResult.getRecords(), itemsById));
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> saveRule(long companyId, Map<String, Object> input, boolean update) {
		Long ruleId = update ? readRuleId(input) : null;
		if (update) {
			requireRule(companyId, ruleId);
		}
		String ruleName = readRuleName(input);
		List<Long> mainItemIds = readItemIdList(input, "main_item_ids", messageSource);
		List<Long> recommendItemIds = readItemIdList(input, "recommend_item_ids", messageSource);
		boolean excludeConflict = readBooleanFlag(input, "exclude_conflict_main_items");

		mainItemIds = dedupePreserveOrder(mainItemIds);
		recommendItemIds = dedupePreserveOrder(recommendItemIds);

		Map<Long, Items> itemsById = requireCompanyItems(companyId, union(mainItemIds, recommendItemIds));
		validateSaveBasics(mainItemIds, recommendItemIds, itemsById);

		List<Long> effectiveMainIds = new ArrayList<>(mainItemIds);
		if (excludeConflict) {
			Set<Long> conflicts = findConflictMainItemIds(companyId, effectiveMainIds, itemsById, ruleId);
			effectiveMainIds.removeIf(conflicts::contains);
			if (effectiveMainIds.isEmpty()) {
				throw badRequest(GoodsRecommendErrorCodes.MAIN_ITEMS_EMPTY_AFTER_EXCLUDE);
			}
		} else {
			checkMainItemConflicts(companyId, effectiveMainIds, ruleId, itemsById);
		}

		int now = (int) Instant.now().getEpochSecond();
		GoodsRecommendRule rule;
		if (update) {
			rule = requireRule(companyId, ruleId);
			rule.setRuleName(ruleName);
			rule.setUpdated(now);
			ruleMapper.updateById(rule);
			deleteAssociations(companyId, ruleId);
		} else {
			rule = new GoodsRecommendRule();
			rule.setCompanyId(companyId);
			rule.setRuleName(ruleName);
			rule.setCreated(now);
			rule.setUpdated(now);
			ruleMapper.insert(rule);
			ruleId = rule.getId();
		}

		try {
			insertMainItems(companyId, ruleId, effectiveMainIds, itemsById, now);
			insertRecommendItems(companyId, ruleId, recommendItemIds, itemsById, now);
		} catch (DuplicateKeyException ex) {
			if (!excludeConflict) {
				checkMainItemConflicts(companyId, effectiveMainIds, ruleId, itemsById);
			}
			throw mainItemConflictFallback();
		}

		return getRuleDetail(companyId, ruleId, 1, DEFAULT_PAGE_SIZE, 1, DEFAULT_PAGE_SIZE);
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteRule(long companyId, long ruleId) {
		requireRule(companyId, ruleId);
		deleteAssociations(companyId, ruleId);
		ruleMapper.delete(
				new LambdaQueryWrapper<GoodsRecommendRule>()
						.eq(GoodsRecommendRule::getCompanyId, companyId)
						.eq(GoodsRecommendRule::getId, ruleId));
	}

	private void deleteAssociations(long companyId, long ruleId) {
		mainItemMapper.delete(
				new LambdaQueryWrapper<GoodsRecommendRuleMainItem>()
						.eq(GoodsRecommendRuleMainItem::getCompanyId, companyId)
						.eq(GoodsRecommendRuleMainItem::getRuleId, ruleId));
		recommendItemMapper.delete(
				new LambdaQueryWrapper<GoodsRecommendRuleRecommendItem>()
						.eq(GoodsRecommendRuleRecommendItem::getCompanyId, companyId)
						.eq(GoodsRecommendRuleRecommendItem::getRuleId, ruleId));
	}

	private void insertMainItems(
			long companyId, long ruleId, List<Long> itemIds, Map<Long, Items> itemsById, int now) {
		for (Long itemId : itemIds) {
			Items item = itemsById.get(itemId);
			long goodsId = goodsIdResolver.resolveGoodsId(item);
			GoodsRecommendRuleMainItem row = new GoodsRecommendRuleMainItem();
			row.setCompanyId(companyId);
			row.setRuleId(ruleId);
			row.setGoodsId(goodsId);
			row.setDistributorId(item != null && item.getDistributorId() != null ? item.getDistributorId() : 0L);
			row.setCreated(now);
			mainItemMapper.insert(row);
		}
	}

	private void insertRecommendItems(
			long companyId, long ruleId, List<Long> itemIds, Map<Long, Items> itemsById, int now) {
		int sort = 0;
		for (Long itemId : itemIds) {
			Items item = itemsById.get(itemId);
			GoodsRecommendRuleRecommendItem row = new GoodsRecommendRuleRecommendItem();
			row.setCompanyId(companyId);
			row.setRuleId(ruleId);
			row.setGoodsId(goodsIdResolver.resolveGoodsId(item));
			row.setSort(sort++);
			row.setCreated(now);
			recommendItemMapper.insert(row);
		}
	}

	private GoodsRecommendMainItemConflictException mainItemConflictFallback() {
		String message =
				GoodsRecommendErrorMessages.message(messageSource, GoodsRecommendErrorCodes.MAIN_ITEM_CONFLICT);
		return new GoodsRecommendMainItemConflictException(
				message, GoodsRecommendErrorCodes.MAIN_ITEM_CONFLICT, List.of());
	}

	private void checkMainItemConflicts(
			long companyId, List<Long> mainItemIds, Long excludeRuleId, Map<Long, Items> itemsById) {
		if (mainItemIds.isEmpty()) {
			return;
		}
		Set<Long> goodsIds = goodsIdResolver.resolveGoodsIdSet(itemsById, mainItemIds);
		if (goodsIds.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<GoodsRecommendRuleMainItem> wrapper =
				new LambdaQueryWrapper<GoodsRecommendRuleMainItem>()
						.eq(GoodsRecommendRuleMainItem::getCompanyId, companyId)
						.in(GoodsRecommendRuleMainItem::getGoodsId, goodsIds);
		if (excludeRuleId != null) {
			wrapper.ne(GoodsRecommendRuleMainItem::getRuleId, excludeRuleId);
		}
		List<GoodsRecommendRuleMainItem> occupied = mainItemMapper.selectList(wrapper);
		if (occupied.isEmpty()) {
			return;
		}
		Map<Long, Items> displayByGoodsId =
				goodsIdResolver.loadDisplayItemsByGoodsId(
						companyId,
						occupied.stream().map(GoodsRecommendRuleMainItem::getGoodsId).collect(Collectors.toSet()));
		Map<Long, GoodsRecommendRule> ruleById = new HashMap<>();
		List<Map<String, Object>> conflicts = new ArrayList<>();
		for (GoodsRecommendRuleMainItem row : occupied) {
			GoodsRecommendRule occupiedRule = ruleById.computeIfAbsent(row.getRuleId(), ruleMapper::selectById);
			Items item = displayByGoodsId.get(row.getGoodsId());
			Map<String, Object> c = new LinkedHashMap<>();
			c.put("itemId", item != null ? item.getItemId() : null);
			c.put("goodsId", row.getGoodsId());
			c.put("itemName", item != null ? item.getItemName() : "");
			c.put("occupiedRuleId", row.getRuleId());
			c.put("occupiedRuleName", occupiedRule != null ? occupiedRule.getRuleName() : "");
			conflicts.add(c);
		}
		String message = GoodsRecommendErrorMessages.message(messageSource, GoodsRecommendErrorCodes.MAIN_ITEM_CONFLICT);
		throw new GoodsRecommendMainItemConflictException(message, GoodsRecommendErrorCodes.MAIN_ITEM_CONFLICT, conflicts);
	}

	private Set<Long> findConflictMainItemIds(
			long companyId, List<Long> mainItemIds, Map<Long, Items> itemsById, Long excludeRuleId) {
		if (mainItemIds.isEmpty()) {
			return Set.of();
		}
		Map<Long, Long> itemToGoods = goodsIdResolver.itemIdsToGoodsIds(itemsById, mainItemIds);
		if (itemToGoods.isEmpty()) {
			return Set.of();
		}
		LambdaQueryWrapper<GoodsRecommendRuleMainItem> wrapper =
				new LambdaQueryWrapper<GoodsRecommendRuleMainItem>()
						.eq(GoodsRecommendRuleMainItem::getCompanyId, companyId)
						.in(GoodsRecommendRuleMainItem::getGoodsId, itemToGoods.values());
		if (excludeRuleId != null) {
			wrapper.ne(GoodsRecommendRuleMainItem::getRuleId, excludeRuleId);
		}
		Set<Long> occupiedGoodsIds =
				mainItemMapper.selectList(wrapper).stream()
						.map(GoodsRecommendRuleMainItem::getGoodsId)
						.collect(Collectors.toSet());
		Set<Long> conflicts = new HashSet<>();
		for (Map.Entry<Long, Long> entry : itemToGoods.entrySet()) {
			if (occupiedGoodsIds.contains(entry.getValue())) {
				conflicts.add(entry.getKey());
			}
		}
		return conflicts;
	}

	private void validateSaveBasics(
			List<Long> mainItemIds, List<Long> recommendItemIds, Map<Long, Items> itemsById) {
		if (mainItemIds.isEmpty()) {
			throw badRequest(GoodsRecommendErrorCodes.MAIN_ITEMS_EMPTY);
		}
		if (recommendItemIds.isEmpty()) {
			throw badRequest(GoodsRecommendErrorCodes.RECOMMEND_ITEMS_EMPTY);
		}
		Set<Long> mainGoods = goodsIdResolver.resolveGoodsIdSet(itemsById, mainItemIds);
		Set<Long> recommendGoods = goodsIdResolver.resolveGoodsIdSet(itemsById, recommendItemIds);
		Set<Long> intersection = new HashSet<>(mainGoods);
		intersection.retainAll(recommendGoods);
		if (!intersection.isEmpty()) {
			throw badRequest(GoodsRecommendErrorCodes.MAIN_RECOMMEND_INTERSECT);
		}
	}

	private Map<Long, Items> requireCompanyItems(long companyId, Collection<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return Map.of();
		}
		List<Items> rows =
				itemsMapper.selectList(
						new LambdaQueryWrapper<Items>()
								.eq(Items::getCompanyId, companyId)
								.in(Items::getItemId, itemIds));
		Map<Long, Items> map = rows.stream().collect(Collectors.toMap(Items::getItemId, i -> i, (a, b) -> a));
		if (map.size() != new HashSet<>(itemIds).size()) {
			throw badRequest(GoodsRecommendErrorCodes.ITEM_NOT_FOUND);
		}
		return map;
	}

	private GoodsRecommendRule requireRule(long companyId, long ruleId) {
		GoodsRecommendRule rule = ruleMapper.selectById(ruleId);
		if (rule == null || !Objects.equals(rule.getCompanyId(), companyId)) {
			throw new ResourceException(
					GoodsRecommendErrorMessages.message(messageSource, GoodsRecommendErrorCodes.RULE_NOT_FOUND));
		}
		return rule;
	}

	private Map<String, Object> toListRow(GoodsRecommendRule rule, long companyId) {
		long mainTotal =
				mainItemMapper.selectCount(
						new LambdaQueryWrapper<GoodsRecommendRuleMainItem>()
								.eq(GoodsRecommendRuleMainItem::getCompanyId, companyId)
								.eq(GoodsRecommendRuleMainItem::getRuleId, rule.getId()));
		long recommendTotal =
				recommendItemMapper.selectCount(
						new LambdaQueryWrapper<GoodsRecommendRuleRecommendItem>()
								.eq(GoodsRecommendRuleRecommendItem::getCompanyId, companyId)
								.eq(GoodsRecommendRuleRecommendItem::getRuleId, rule.getId()));
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", rule.getId());
		row.put("rule_name", rule.getRuleName());
		row.put("created", rule.getCreated());
		row.put("main_total", mainTotal);
		row.put("recommend_total", recommendTotal);
		return row;
	}

	private List<Map<String, Object>> toItemViews(List<GoodsRecommendRuleMainItem> rows, Map<Long, Items> itemsById) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (GoodsRecommendRuleMainItem row : rows) {
			out.add(toItemView(row.getGoodsId(), itemsById.get(row.getGoodsId())));
		}
		return out;
	}

	private List<Map<String, Object>> toRecommendItemViews(
			List<GoodsRecommendRuleRecommendItem> rows, Map<Long, Items> itemsById) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (GoodsRecommendRuleRecommendItem row : rows) {
			out.add(toItemView(row.getGoodsId(), itemsById.get(row.getGoodsId())));
		}
		return out;
	}

	private Map<String, Object> toItemView(Long goodsId, Items item) {
		Map<String, Object> view = new LinkedHashMap<>();
		view.put("goods_id", goodsId);
		view.put("item_id", item != null ? item.getItemId() : null);
		if (item != null) {
			view.put("item_name", item.getItemName());
			view.put("price", item.getPrice());
			view.put("pics", item.getPics());
			view.put("distributor_id", item.getDistributorId());
			view.put("approve_status", item.getApproveStatus());
		}
		return view;
	}

	private Map<Long, Items> loadItemsForRuleRows(
			long companyId,
			List<GoodsRecommendRuleMainItem> mainRows,
			List<GoodsRecommendRuleRecommendItem> recommendRows) {
		Set<Long> goodsIds = new LinkedHashSet<>();
		for (GoodsRecommendRuleMainItem row : mainRows) {
			if (row.getGoodsId() != null) {
				goodsIds.add(row.getGoodsId());
			}
		}
		for (GoodsRecommendRuleRecommendItem row : recommendRows) {
			if (row.getGoodsId() != null) {
				goodsIds.add(row.getGoodsId());
			}
		}
		return goodsIdResolver.loadDisplayItemsByGoodsId(companyId, goodsIds);
	}

	private static List<Long> union(List<Long> a, List<Long> b) {
		LinkedHashSet<Long> set = new LinkedHashSet<>(a);
		set.addAll(b);
		return new ArrayList<>(set);
	}

	private static List<Long> dedupePreserveOrder(List<Long> ids) {
		LinkedHashSet<Long> set = new LinkedHashSet<>();
		for (Long id : ids) {
			if (id != null && id > 0) {
				set.add(id);
			}
		}
		return new ArrayList<>(set);
	}

	private static int boundedPageSize(int pageSize) {
		if (pageSize <= 0) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(MAX_PAGE_SIZE, pageSize);
	}

	private String readRuleName(Map<String, Object> input) {
		Object raw = input.get("rule_name");
		if (raw == null) {
			raw = input.get("ruleName");
		}
		if (raw == null) {
			throw badRequest(GoodsRecommendErrorCodes.RULE_NAME_INVALID);
		}
		String name = raw.toString().trim();
		if (!StringUtils.hasText(name) || name.length() > 50) {
			throw badRequest(GoodsRecommendErrorCodes.RULE_NAME_INVALID);
		}
		return name;
	}

	private Long readRuleId(Map<String, Object> input) {
		Object raw = input.get("id");
		if (raw == null) {
			throw badRequest(GoodsRecommendErrorCodes.RULE_NOT_FOUND);
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw badRequest(GoodsRecommendErrorCodes.RULE_NOT_FOUND);
		}
	}

	private BadRequestException badRequest(String errorCode) {
		return new BadRequestException(GoodsRecommendErrorMessages.message(messageSource, errorCode));
	}

	private static boolean readBooleanFlag(Map<String, Object> input, String key) {
		Object raw = input.get(key);
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = raw.toString().trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	@SuppressWarnings("unchecked")
	private static List<Long> readItemIdList(Map<String, Object> input, String key, MessageSource messageSource) {
		Object raw = input.get(key);
		if (raw == null) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		if (raw instanceof Collection<?> collection) {
			for (Object element : collection) {
				try {
					out.add(parseLongId(element));
				} catch (NumberFormatException e) {
					throw itemIdsInvalid(messageSource);
				}
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			for (String part : s.split(",")) {
				if (StringUtils.hasText(part)) {
					try {
						out.add(Long.parseLong(part.trim()));
					} catch (NumberFormatException e) {
						throw itemIdsInvalid(messageSource);
					}
				}
			}
		}
		return out;
	}

	private static BadRequestException itemIdsInvalid(MessageSource messageSource) {
		return new BadRequestException(
				GoodsRecommendErrorMessages.message(messageSource, GoodsRecommendErrorCodes.ITEM_IDS_INVALID));
	}

	private static long parseLongId(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof Map<?, ?> map) {
			Object id = map.get("item_id");
			if (id == null) {
				id = map.get("itemId");
			}
			return parseLongId(id);
		}
		return Long.parseLong(raw.toString().trim());
	}
}
