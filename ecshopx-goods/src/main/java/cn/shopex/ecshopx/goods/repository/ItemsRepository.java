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

package cn.shopex.ecshopx.goods.repository;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ItemsRepository {

	private static final Logger log = LoggerFactory.getLogger(ItemsRepository.class);

	public sealed interface GoodsIdClause permits GoodsIdClause.SingleId, GoodsIdClause.InIds, GoodsIdClause.IsNull, GoodsIdClause.Unrestricted {
		record SingleId(long goodsId) implements GoodsIdClause {}

		record InIds(List<Long> goodsIds) implements GoodsIdClause {}

		record IsNull() implements GoodsIdClause {}

		record Unrestricted() implements GoodsIdClause {}
	}

	private final ItemsMapper mapper;

	public ItemsRepository(ItemsMapper mapper) {
		this.mapper = mapper;
	}

	public Items getByItemIdAndCompany(long itemId, long companyId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getItemId, itemId).eq(Items::getCompanyId, companyId).last("LIMIT 1");
		return mapper.selectOne(w);
	}

	/** 小程序分享：公司已审单行，仅选取分享所需列。 */
	public Items getSimpleInfoForWxappShare(long companyId, long itemId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId)
				.eq(Items::getItemId, itemId)
				.eq(Items::getAuditStatus, "approved")
				.select(Items::getItemId, Items::getItemName, Items::getBrief, Items::getPrice, Items::getPics, Items::getPicsCreateQrcode)
				.last("LIMIT 1");
		return mapper.selectOne(w);
	}

	/** 公司已审、默认 SKU（单 goods 下一行）。 */
	public Items findApprovedDefaultSkuByGoodsIdAndCompany(long goodsId, long companyId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId)
				.eq(Items::getGoodsId, goodsId)
				.eq(Items::getAuditStatus, "approved")
				.eq(Items::getIsDefault, true)
				.last("LIMIT 1");
		return mapper.selectOne(w);
	}

	/** 仅按主键 {@code item_id} 查询一行，不按公司过滤。 */
	public Items findByItemId(long itemId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getItemId, itemId).last("LIMIT 1");
		return mapper.selectOne(w);
	}

	/** 若不存在对应 {@code item_id} 行则不做任何更新。 */
	public void updateSortByItemIdIfExists(long itemId, int sort) {
		Items row = findByItemId(itemId);
		if (row == null) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getItemId, itemId).set(Items::getSort, sort).set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	/**
	 * 若库中存在 {@code data_source} 列则返回其值；列不存在或查询失败（语法/未知列等）时返回 empty，不影响主流程。
	 */
	public Optional<String> trySelectDataSource(long itemId, long companyId) {
		try {
			return Optional.ofNullable(mapper.selectDataSourceRaw(itemId, companyId));
		} catch (RuntimeException e) {
			if (isDataSourceColumnQueryUnsupported(e)) {
				return Optional.empty();
			}
			throw e;
		}
	}

	private static boolean isDataSourceColumnQueryUnsupported(Throwable t) {
		for (Throwable c = t; c != null; c = c.getCause()) {
			if (c instanceof BadSqlGrammarException || c instanceof SQLSyntaxErrorException) {
				return true;
			}
			if (c instanceof SQLException sql && "42S22".equals(sql.getSQLState())) {
				return true;
			}
		}
		return false;
	}

	public boolean existsByItemIdAndCompany(long itemId, long companyId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getItemId, itemId).eq(Items::getCompanyId, companyId);
		return mapper.selectCount(w) > 0;
	}

	/**
	 * Resolves trimmed item_bn values to item_id for the company; first row wins when duplicates exist in query
	 * results.
	 */
	public Map<String, Long> mapItemBnToItemIdForCompany(long companyId, Collection<String> itemBns) {
		if (itemBns == null || itemBns.isEmpty()) {
			return Collections.emptyMap();
		}
		LinkedHashSet<String> distinctBns = new LinkedHashSet<>();
		for (String bn : itemBns) {
			if (bn == null) {
				continue;
			}
			String t = bn.trim();
			if (StringUtils.hasText(t)) {
				distinctBns.add(t);
			}
		}
		if (distinctBns.isEmpty()) {
			return Collections.emptyMap();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId)
				.in(Items::getItemBn, distinctBns)
				.select(Items::getItemBn, Items::getItemId);
		List<Items> rows = mapper.selectList(w);
		return rows.stream()
				.filter(it -> it.getItemBn() != null && it.getItemId() != null)
				.collect(
						Collectors.toMap(
								it -> it.getItemBn().trim(),
								Items::getItemId,
								(a, b) -> a,
								LinkedHashMap::new));
	}

	/**
	 * Batch load by company + item_bn IN, optionally eq distributor_id. Ordered by item_id DESC so later
	 * merges keyed by item_bn keep the lower id (PHP getItemsList default order).
	 */
	public List<Items> listByCompanyAndItemBns(long companyId, Collection<String> itemBns, Long distributorIdEq) {
		if (itemBns == null || itemBns.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<String> distinctBns = new LinkedHashSet<>();
		for (String bn : itemBns) {
			if (bn == null) {
				continue;
			}
			String t = bn.trim();
			if (StringUtils.hasText(t)) {
				distinctBns.add(t);
			}
		}
		if (distinctBns.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemBn, distinctBns);
		if (distributorIdEq != null) {
			w.eq(Items::getDistributorId, distributorIdEq.intValue());
		}
		w.orderByDesc(Items::getItemId);
		return mapper.selectList(w);
	}

	public Items findByItemBnAndCompany(String itemBn, long companyId) {
		if (itemBn == null || itemBn.isEmpty()) {
			return null;
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getItemBn, itemBn)
				.eq(Items::getCompanyId, companyId)
				.orderByAsc(Items::getItemId)
				.last("LIMIT 1");
		return mapper.selectOne(w);
	}

	public Items findByItemBnAndCompanyAndDistributorId(String itemBn, long companyId, long distributorId) {
		if (itemBn == null || itemBn.isEmpty()) {
			return null;
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getItemBn, itemBn)
				.eq(Items::getCompanyId, companyId)
				.eq(Items::getDistributorId, (int) distributorId)
				.orderByAsc(Items::getItemId)
				.last("LIMIT 1");
		return mapper.selectOne(w);
	}

	/**
	 * 缺 item_bn 时按 company_id 取任意一行；仅用于 OpenAPI ecx.item.store.get 无 item_code 边界。
	 */
	public Items findFirstByCompanyId(long companyId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).last("LIMIT 1");
		return mapper.selectOne(w);
	}

	/**
	 * OpenAPI ecx.item.entity.get：按 company_id + item_bn + is_default=1 定位默认 SKU 行。
	 */
	public Items findDefaultByItemBnAndCompany(String itemBn, long companyId) {
		if (!StringUtils.hasText(itemBn)) {
			return null;
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getItemBn, itemBn.trim())
				.eq(Items::getCompanyId, companyId)
				.apply("items.is_default = {0}", 1)
				.last("LIMIT 1");
		return mapper.selectOne(w);
	}

	public Items findDefaultByGoodsBnAndCompany(String goodsBn, long companyId) {
		return findSpuAnchorByGoodsBnAndCompany(goodsBn, companyId);
	}

	/**
	 * 按 SPU 编码定位归属锚点：优先已绑定完整 SPU 的默认 SKU，否则取同 goods_bn 最早一条（用于导入合并/脏数据纠偏）。
	 */
	public Items findSpuAnchorByGoodsBnAndCompany(String goodsBn, long companyId) {
		if (!StringUtils.hasText(goodsBn)) {
			return null;
		}
		String bn = goodsBn.trim();
		LambdaQueryWrapper<Items> linked = new LambdaQueryWrapper<>();
		linked.eq(Items::getGoodsBn, bn)
				.eq(Items::getCompanyId, companyId)
				.apply("items.is_default = {0}", 1)
				.gt(Items::getGoodsId, 0)
				.gt(Items::getDefaultItemId, 0)
				.orderByAsc(Items::getItemId)
				.last("LIMIT 1");
		Items row = mapper.selectOne(linked);
		if (row != null) {
			return row;
		}
		LambdaQueryWrapper<Items> earliest = new LambdaQueryWrapper<>();
		earliest.eq(Items::getGoodsBn, bn)
				.eq(Items::getCompanyId, companyId)
				.orderByAsc(Items::getItemId)
				.last("LIMIT 1");
		return mapper.selectOne(earliest);
	}

	public List<Items> listByGoodsBnAndCompany(String goodsBn, long companyId) {
		if (!StringUtils.hasText(goodsBn)) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getGoodsBn, goodsBn.trim())
				.eq(Items::getCompanyId, companyId)
				.orderByAsc(Items::getItemId);
		return mapper.selectList(w);
	}

	public List<Map<String, Object>> listGoodsIdRowsByDefaultItemBnIn(long companyId, Collection<String> itemBns) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).apply("items.is_default = {0}", 1);
		if (itemBns != null && !itemBns.isEmpty()) {
			LinkedHashSet<String> distinct = new LinkedHashSet<>();
			for (String bn : itemBns) {
				if (StringUtils.hasText(bn)) {
					distinct.add(bn.trim());
				}
			}
			if (!distinct.isEmpty()) {
				w.in(Items::getItemBn, distinct);
			}
		}
		w.select(Items::getGoodsId);
		List<Map<String, Object>> rows = new ArrayList<>();
		for (Items row : mapper.selectList(w)) {
			if (row.getGoodsId() != null) {
				rows.add(Map.of("goods_id", row.getGoodsId()));
			}
		}
		return rows;
	}

	public Items findByItemBnAndCompanyIdAndAuditStatus(String itemBn, Long companyId, String auditStatus) {
		if (!StringUtils.hasText(itemBn)) {
			return null;
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getItemBn, itemBn).eq(Items::getAuditStatus, auditStatus);
		if (companyId == null) {
			w.isNull(Items::getCompanyId);
		} else {
			w.eq(Items::getCompanyId, companyId);
		}
		w.last("LIMIT 1");
		return mapper.selectOne(w);
	}

	public List<Items> listByDefaultItemIdAndCompany(long defaultItemId, long companyId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getDefaultItemId, defaultItemId).eq(Items::getCompanyId, companyId).last("LIMIT 100");
		return mapper.selectList(w);
	}

	public List<Items> listBySupplierItemIds(Collection<Long> supplierItemIds) {
		if (supplierItemIds == null || supplierItemIds.isEmpty()) {
			return List.of();
		}
		List<Integer> ints = supplierItemIds.stream().map(Long::intValue).toList();
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.in(Items::getSupplierItemId, ints);
		return mapper.selectList(w);
	}

	/** 供应商 SKU id 列表映射为商品池行（含 company 约束）。 */
	public List<Items> listByCompanyIdAndSupplierItemIds(long companyId, Collection<Long> supplierItemIds) {
		if (supplierItemIds == null || supplierItemIds.isEmpty()) {
			return List.of();
		}
		List<Integer> ints = supplierItemIds.stream().map(Long::intValue).toList();
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getSupplierItemId, ints);
		return mapper.selectList(w);
	}

	public Items getBySupplierItemIdAndCompany(long supplierItemId, long companyId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getSupplierItemId, (int) supplierItemId).eq(Items::getCompanyId, companyId).last("LIMIT 1");
		return mapper.selectOne(w);
	}

	public Items getBySupplierItemIdCompanyAndDistributor(long supplierItemId, long companyId, long distributorId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getSupplierItemId, (int) supplierItemId)
				.eq(Items::getCompanyId, companyId)
				.eq(Items::getDistributorId, (int) distributorId)
				.last("LIMIT 1");
		return mapper.selectOne(w);
	}

	/** 按 itemId 列表顺序返回同一公司下的商品行（用于分销 JOIN 分页后再对齐顺序）。 */
	public List<Items> listByCompanyAndItemIdsPreservingOrder(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemId, itemIds);
		List<Items> loaded = mapper.selectList(w);
		Map<Long, Items> byId = loaded.stream().collect(Collectors.toMap(Items::getItemId, x -> x, (a, b) -> a, LinkedHashMap::new));
		List<Items> ordered = new ArrayList<>();
		for (Long id : itemIds) {
			Items it = byId.get(id);
			if (it != null) {
				ordered.add(it);
			}
		}
		return ordered;
	}

	public int deleteByItemIdsAndCompanyId(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemId, itemIds);
		return mapper.delete(w);
	}

	public void insert(Items row) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (row.getCreated() == null) {
			row.setCreated(now);
		}
		if (row.getUpdated() == null) {
			row.setUpdated(now);
		}
		mapper.insert(row);
	}

	public void updateByItemId(long itemId, long companyId, Items patch) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getItemId, itemId).eq(Items::getCompanyId, companyId);
		int now = (int) (System.currentTimeMillis() / 1000L);
		patch.setUpdated(now);
		mapper.update(patch, w);
	}

	public void updateByItemIds(long companyId, Collection<Long> itemIds, long defaultItemId, long goodsId) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getCompanyId, companyId).in(Items::getItemId, itemIds).set(Items::getDefaultItemId, defaultItemId).set(Items::getGoodsId, goodsId)
				.set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	public void clearDefaultFlagExcept(long companyId, long defaultItemId, long excludeItemId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getCompanyId, companyId).eq(Items::getDefaultItemId, defaultItemId).ne(Items::getItemId, excludeItemId).set(Items::getIsDefault, false)
				.set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	public void setDefaultItem(long companyId, long itemId, boolean isDefault) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getCompanyId, companyId).eq(Items::getItemId, itemId).set(Items::getIsDefault, isDefault).set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	public void updateAuditFieldsByCompanyAndGoodsId(long companyId, GoodsIdClause clause, String auditStatus, String auditReason) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getCompanyId, companyId);
		if (clause instanceof GoodsIdClause.SingleId s) {
			u.eq(Items::getGoodsId, s.goodsId());
		} else if (clause instanceof GoodsIdClause.InIds in) {
			u.in(Items::getGoodsId, in.goodsIds());
		} else if (clause instanceof GoodsIdClause.IsNull) {
			u.isNull(Items::getGoodsId);
		}
		u.set(Items::getAuditStatus, auditStatus).set(Items::getAuditReason, auditReason).set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	public List<Items> listItemIdSupplierItemIdByCompanyAndItemId(long companyId, long itemId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId)
				.eq(Items::getItemId, itemId)
				.select(Items::getItemId, Items::getSupplierItemId, Items::getGoodsId, Items::getApproveStatus);
		return mapper.selectList(w);
	}

	public List<Items> listItemIdGoodsIdStoreByCompanyAndGoodsIds(long companyId, Collection<Long> goodsIds) {
		if (goodsIds == null || goodsIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId)
				.in(Items::getGoodsId, goodsIds)
				.select(Items::getItemId, Items::getGoodsId, Items::getStore);
		return mapper.selectList(w);
	}

	public List<Items> listItemIdSupplierItemIdByCompanyAndGoodsIds(long companyId, Collection<Long> goodsIds) {
		if (goodsIds == null || goodsIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId)
				.in(Items::getGoodsId, goodsIds)
				.select(Items::getItemId, Items::getSupplierItemId, Items::getGoodsId, Items::getApproveStatus);
		return mapper.selectList(w);
	}

	public long countInstockByCompanyAndItemId(long companyId, long itemId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).eq(Items::getItemId, itemId).eq(Items::getApproveStatus, "instock");
		return mapper.selectCount(w);
	}

	public List<Items> listApproveStatusByCompanyAndGoodsIds(long companyId, Collection<Long> goodsIds) {
		if (goodsIds == null || goodsIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId)
				.in(Items::getGoodsId, goodsIds)
				.select(Items::getItemId, Items::getSupplierItemId, Items::getGoodsId, Items::getApproveStatus);
		return mapper.selectList(w);
	}

	public void updateProfitByCompanyAndItemCategory(long companyId, long categoryId, int profitType, String profitScalePlain) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getCompanyId, companyId)
				.eq(Items::getItemCategory, String.valueOf(categoryId))
				.set(Items::getProfitType, profitType)
				.setSql("profit_fee = FLOOR(price * " + profitScalePlain + ")")
				.set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	/**
	 * 按公司与 {@code profit_type} 批量更新分润金额 {@code profit_fee = FLOOR(price * scale)}；{@code profitScalePlain} 须为已校验的数值字面量。
	 */
	public void updateProfitByCompanyAndProfitType(long companyId, int profitType, String profitScalePlain) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getCompanyId, companyId)
				.eq(Items::getProfitType, profitType)
				.set(Items::getProfitType, profitType)
				.setSql("profit_fee = FLOOR(price * " + profitScalePlain + ")")
				.set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	/** 仅按 {@code goods_id} 查询，不按公司过滤。 */
	public List<Items> listByGoodsIds(Collection<Long> goodsIds) {
		if (goodsIds == null || goodsIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		if (goodsIds.size() == 1) {
			w.eq(Items::getGoodsId, goodsIds.iterator().next());
		} else {
			w.in(Items::getGoodsId, goodsIds);
		}
		return mapper.selectList(w);
	}

	/** 仅按 {@code item_id} 更新分润类型与固定金额（分），与类目/默认比例路径无关。 */
	public void updateProfitFeeByItemId(long itemId, int itemsTableProfitType, int profitFee) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getItemId, itemId)
				.set(Items::getProfitType, itemsTableProfitType)
				.set(Items::getProfitFee, profitFee)
				.set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	/**
	 * 仅按 {@code item_id} 设置分润类型，并按 {@code profit_fee = FLOOR(price * scale)} 更新；{@code profitScalePlain} 须为已校验的数值字面量。
	 */
	public void updateProfitByItemId(long itemId, int profitType, String profitScalePlain) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getItemId, itemId)
				.set(Items::getProfitType, profitType)
				.setSql("profit_fee = FLOOR(price * " + profitScalePlain + ")")
				.set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	public void updateTemplatesIdByDefaultItemIdAndCompany(long defaultItemId, long companyId, int templatesId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getDefaultItemId, defaultItemId)
				.eq(Items::getCompanyId, companyId)
				.set(Items::getTemplatesId, templatesId)
				.set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	public long countBySupplierItemIds(Collection<Long> supplierItemIds) {
		if (supplierItemIds == null || supplierItemIds.isEmpty()) {
			return 0L;
		}
		List<Integer> ints = supplierItemIds.stream().map(Long::intValue).toList();
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.in(Items::getSupplierItemId, ints);
		return mapper.selectCount(w);
	}

	public void updateTemplatesIdBySupplierItemIds(Collection<Long> supplierItemIds, int templatesId) {
		if (supplierItemIds == null || supplierItemIds.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		List<Integer> ints = supplierItemIds.stream().map(Long::intValue).toList();
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.in(Items::getSupplierItemId, ints).set(Items::getTemplatesId, templatesId).set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	public List<Items> listByCompanyIdAndGoodsId(long companyId, long goodsId, boolean priceGreaterThanZero) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).eq(Items::getGoodsId, goodsId);
		if (priceGreaterThanZero) {
			w.gt(Items::getPrice, 0);
		}
		return mapper.selectList(w);
	}

	public void updateIsGiftByCompanyAndGoodsId(long companyId, long goodsId, boolean isGift) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getCompanyId, companyId)
				.eq(Items::getGoodsId, goodsId)
				.set(Items::getIsGift, isGift)
				.set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	public void updateApproveStatusByCompanyAndGoodsId(long companyId, long goodsId, String approveStatus) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getCompanyId, companyId)
				.eq(Items::getGoodsId, goodsId)
				.set(Items::getApproveStatus, approveStatus)
				.set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	public List<Long> listSupplierItemIdsGe1ByCompanyAndGoodsIds(long companyId, Collection<Long> goodsIds) {
		if (goodsIds == null || goodsIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getGoodsId, goodsIds).ge(Items::getSupplierItemId, 1).select(Items::getSupplierItemId);
		return mapper.selectList(w).stream()
				.map(Items::getSupplierItemId)
				.filter(Objects::nonNull)
				.map(Integer::longValue)
				.distinct()
				.collect(Collectors.toList());
	}

	public List<Items> listItemIdAndItemNameByCompanyAndGoodsIds(long companyId, Collection<Long> goodsIds) {
		if (goodsIds == null || goodsIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getGoodsId, goodsIds).select(Items::getItemId, Items::getItemName);
		return mapper.selectList(w);
	}

	/**
	 * 按 item_bn IN 查询，不加 company_id 条件。同 bn 多行时后者覆盖前者。
	 */
	public Map<String, Items> mapItemsByItemBnIn(Collection<String> itemBns) {
		if (itemBns == null || itemBns.isEmpty()) {
			return Collections.emptyMap();
		}
		LinkedHashSet<String> distinctBns = new LinkedHashSet<>();
		for (String bn : itemBns) {
			if (bn == null) {
				continue;
			}
			String t = bn.trim();
			if (StringUtils.hasText(t)) {
				distinctBns.add(t);
			}
		}
		if (distinctBns.isEmpty()) {
			return Collections.emptyMap();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.in(Items::getItemBn, distinctBns)
				.select(Items::getItemId, Items::getCompanyId, Items::getItemBn);
		List<Items> rows = mapper.selectList(w);
		return rows.stream()
				.filter(it -> it.getItemBn() != null && it.getItemId() != null)
				.collect(Collectors.toMap(
						it -> it.getItemBn().trim(),
						it -> it,
						(a, b) -> b,
						LinkedHashMap::new));
	}

	/**
	 * 按 company_id + item_bn 自增/自减总部库存。
	 *
	 * @param delta 正数自增、负数自减、0 不更新
	 * @return 受影响行数；减库存且 store 不足时返回 0
	 */
	public int adjustItemStoreByCompanyAndItemBn(long companyId, String itemBn, int delta) {
		if (delta == 0) {
			return 0;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getCompanyId, companyId).eq(Items::getItemBn, itemBn);
		if (delta > 0) {
			u.setSql("store = store + " + delta);
		} else {
			int absDelta = Math.abs(delta);
			u.ge(Items::getStore, absDelta);
			u.setSql("store = store - " + absDelta);
		}
		u.set(Items::getUpdated, now);
		return mapper.update(null, u);
	}

	/** 仅按主键 item_id 更新库存；无行则静默。 */
	public void updateSingleItemStoreIfExists(long itemId, int store) {
		Items row = findByItemId(itemId);
		if (row == null) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getItemId, itemId).set(Items::getStore, store).set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	public void updateStoreByCompanyAndDefaultItemId(long companyId, long defaultItemId, int store) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getCompanyId, companyId)
				.eq(Items::getDefaultItemId, defaultItemId)
				.set(Items::getStore, store)
				.set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	public void updateStoreByItemIds(long companyId, Collection<Long> itemIds, int store) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getCompanyId, companyId).in(Items::getItemId, itemIds).set(Items::getStore, store).set(Items::getUpdated, now);
		mapper.update(null, u);
	}

	/**
	 * 供应商编辑回写已关联商品（平台池 + 店铺副本）：仅更新 PHP {@code createItems} 白名单字段。
	 */
	public void updateLinkedItemsOnSupplierEdit(long companyId, Collection<Long> itemIds, SupplierLinkedItemsSyncPatch patch) {
		if (itemIds == null || itemIds.isEmpty() || patch == null) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getCompanyId, companyId)
				.in(Items::getItemId, itemIds)
				.set(Items::getPics, patch.pics())
				.set(Items::getApproveStatus, patch.approveStatus())
				.set(Items::getItemName, patch.itemName())
				.set(Items::getUpdated, now);
		if (patch.costPrice() != null) {
			u.set(Items::getCostPrice, patch.costPrice());
		}
		if (patch.startNum() != null) {
			u.set(Items::getStartNum, patch.startNum());
		}
		if (patch.auditStatus() != null) {
			u.set(Items::getAuditStatus, patch.auditStatus());
			u.set(Items::getAuditReason, patch.auditReason() != null ? patch.auditReason() : "");
			if (patch.auditDate() != null) {
				u.set(Items::getAuditDate, patch.auditDate());
			} else {
				u.setSql("audit_date = NULL");
			}
		}
		mapper.update(null, u);
	}

	public Items selectOneForPriceCheckAndPromotion(long companyId, Long itemIdOrNull, Long goodsIdOrNull) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId);
		if (itemIdOrNull != null) {
			w.eq(Items::getItemId, itemIdOrNull).last("LIMIT 1");
			return mapper.selectOne(w);
		}
		if (goodsIdOrNull != null) {
			w.eq(Items::getGoodsId, goodsIdOrNull).orderByAsc(Items::getItemId).last("LIMIT 1");
			return mapper.selectOne(w);
		}
		return null;
	}

	/** 按公司与条码精确匹配（trim 后的条码与库内 {@code items.barcode} 全等），用于扫码等多行判定。 */
	public List<Items> listByCompanyIdAndBarcodeExact(long companyId, String barcode) {
		if (barcode == null) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).eq(Items::getBarcode, barcode);
		return mapper.selectList(w);
	}

	/** 按公司、店铺与条码精确匹配 {@code items.barcode}（店铺维度扫码）。 */
	public List<Items> listByCompanyIdAndDistributorIdAndBarcodeExact(
			long companyId, long distributorId, String barcode) {
		if (barcode == null) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId)
				.eq(Items::getDistributorId, (int) distributorId)
				.eq(Items::getBarcode, barcode);
		return mapper.selectList(w);
	}

	/**
	 * 小程序类目等级列表：在售/仅展示、已审核、普通默认商品，按店铺维度 item_id 列表（无 DISTINCT）。
	 */
	public List<Long> listItemIdsForWxappLevelCategory(long companyId, Collection<Long> distributorIdsIncludingZero,
			boolean requireRebateOne) {
		if (distributorIdsIncludingZero == null || distributorIdsIncludingZero.isEmpty()) {
			return List.of();
		}
		List<Integer> distInts = new ArrayList<>(distributorIdsIncludingZero.size());
		for (Long d : distributorIdsIncludingZero) {
			if (d == null) {
				continue;
			}
			if (d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
				continue;
			}
			distInts.add(d.intValue());
		}
		if (distInts.isEmpty()) {
			return List.of();
		}
		if (log.isDebugEnabled()) {
			log.debug("listItemIdsForWxappLevelCategory companyId={} distCount={} requireRebateOne={}", companyId,
					distInts.size(), requireRebateOne);
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId)
				.in(Items::getApproveStatus, List.of("onsale", "only_show"))
				.eq(Items::getAuditStatus, "approved")
				.eq(Items::getItemType, "normal")
				.eq(Items::getIsDefault, Boolean.TRUE)
				.in(Items::getDistributorId, distInts);
		if (requireRebateOne) {
			w.eq(Items::getRebate, 1);
		}
		w.select(Items::getItemId);
		return mapper.selectList(w).stream().map(Items::getItemId).filter(Objects::nonNull).collect(Collectors.toList());
	}

	public int updateByItemsPriceStoreFilter(
			long companyId,
			Long itemIdOrNull,
			Long goodsIdOrNull,
			List<String> auditStatusInOrNull,
			ItemsPriceStoreStatusPatch patch) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getCompanyId, companyId);
		if (itemIdOrNull != null) {
			u.eq(Items::getItemId, itemIdOrNull);
		} else {
			u.eq(Items::getGoodsId, goodsIdOrNull);
		}
		if (auditStatusInOrNull != null && !auditStatusInOrNull.isEmpty()) {
			u.in(Items::getAuditStatus, auditStatusInOrNull);
		}
		if (patch.price() != null) {
			u.set(Items::getPrice, patch.price());
		}
		if (patch.costPrice() != null) {
			u.set(Items::getCostPrice, patch.costPrice());
		}
		if (patch.marketPrice() != null) {
			u.set(Items::getMarketPrice, patch.marketPrice());
		}
		if (patch.store() != null) {
			u.set(Items::getStore, patch.store());
		}
		if (patch.rebate() != null) {
			u.set(Items::getRebate, patch.rebate());
		}
		if (patch.rebateType() != null) {
			u.set(Items::getRebateType, patch.rebateType());
		}
		if (patch.approveStatus() != null) {
			u.set(Items::getApproveStatus, patch.approveStatus());
		}
		if (patch.isMarket() != null) {
			u.set(Items::getIsMarket, patch.isMarket());
		}
		if (patch.auditStatus() != null) {
			u.set(Items::getAuditStatus, patch.auditStatus());
		}
		u.set(Items::getUpdated, now);
		return mapper.update(null, u);
	}

	/**
	 * DISTINCT {@code item_id} for {@code items} rows matching company, optional main category ids, optional name
	 * contains, optional distributor.
	 */
	public List<Long> selectDistinctItemIdsByCompanyAndItemCategoryIn(long companyId, Collection<Long> itemCategoryIds,
			String itemNameContains, Long distributorId) {
		if (itemCategoryIds == null || itemCategoryIds.isEmpty()) {
			return List.of();
		}
		List<String> catStrs = itemCategoryIds.stream().map(String::valueOf).toList();
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemCategory, catStrs);
		if (StringUtils.hasText(itemNameContains)) {
			String pat = "%" + escapeSqlLike(itemNameContains.trim()) + "%";
			w.like(Items::getItemName, pat);
		}
		if (distributorId != null) {
			w.eq(Items::getDistributorId, distributorId.intValue());
		}
		w.select(Items::getItemId);
		w.groupBy(Items::getItemId);
		return mapper.selectList(w).stream().map(Items::getItemId).filter(Objects::nonNull).distinct().toList();
	}

	/**
	 * Returns distinct positive {@code brand_id} values for items belonging to the company. When {@code itemIds} is
	 * non-empty, results are limited to those item ids; when empty, no {@code item_id} predicate is applied and all
	 * matching company items are considered (subject to optional name and distributor filters).
	 */
	public List<Long> selectDistinctPositiveBrandIdsByCompanyAndItemIdIn(long companyId, Collection<Long> itemIds,
			String itemNameContains, Long distributorId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).gt(Items::getBrandId, 0);
		if (itemIds != null && !itemIds.isEmpty()) {
			w.in(Items::getItemId, itemIds);
		}
		if (StringUtils.hasText(itemNameContains)) {
			String pat = "%" + escapeSqlLike(itemNameContains.trim()) + "%";
			w.like(Items::getItemName, pat);
		}
		if (distributorId != null) {
			w.eq(Items::getDistributorId, distributorId.intValue());
		}
		w.select(Items::getBrandId);
		w.groupBy(Items::getBrandId);
		return mapper.selectList(w).stream()
				.map(Items::getBrandId)
				.filter(Objects::nonNull)
				.filter(b -> b > 0)
				.map(Integer::longValue)
				.distinct()
				.toList();
	}

	/**
	 * 小店上架类目：候选 {@code goods_id} 中，在公司内存在 rebate=1 货品的去重商品 id（对齐推广商品库 lists 的 join 条件）。
	 */
	public List<Long> listDistinctGoodsIdsHavingRebateItem(long companyId, Collection<Long> goodsIds) {
		if (goodsIds == null || goodsIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getGoodsId, goodsIds).eq(Items::getRebate, 1);
		w.select(Items::getGoodsId);
		return mapper.selectList(w).stream().map(Items::getGoodsId).filter(Objects::nonNull).distinct()
				.collect(Collectors.toList());
	}

	/**
	 * 小店上架类目：按货品取前台可售/线下可售货品的 {@code item_id}；{@code requireRebateOne} 时与 configure goods=select 一致再限制 rebate=1。
	 * <p>
	 * {@code goods_id} 为空数组时不加 IN 条件，等价于按公司+状态（+rebate）查全部货品。
	 */
	public List<Long> listItemIdsForShopShelvesListing(long companyId, Collection<Long> goodsIds, boolean requireRebateOne) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getApproveStatus, List.of("onsale", "offline_sale"));
		if (goodsIds != null && !goodsIds.isEmpty()) {
			w.in(Items::getGoodsId, goodsIds);
		}
		if (requireRebateOne) {
			w.eq(Items::getRebate, 1);
		}
		w.select(Items::getItemId);
		return mapper.selectList(w).stream().map(Items::getItemId).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private static String escapeSqlLike(String raw) {
		if (raw == null || raw.isEmpty()) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
