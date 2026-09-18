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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityGoods;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityGoodsMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 对齐 PHP {@code ActivityItemsUploadService::handleRow}。
 */
@Service
public class EmployeePurchaseActivityItemsImportRowService {

	private final ActivitiesMapper activitiesMapper;
	private final ItemsMapper itemsMapper;
	private final ActivityItemsMapper activityItemsMapper;
	private final ActivityGoodsMapper activityGoodsMapper;
	private final DistributorItemsRepository distributorItemsRepository;
	private final EmployeePurchaseActivityItemsCategoryRedisService employeePurchaseActivityItemsCategoryRedisService;

	public EmployeePurchaseActivityItemsImportRowService(
			ActivitiesMapper activitiesMapper,
			ItemsMapper itemsMapper,
			ActivityItemsMapper activityItemsMapper,
			ActivityGoodsMapper activityGoodsMapper,
			DistributorItemsRepository distributorItemsRepository,
			EmployeePurchaseActivityItemsCategoryRedisService employeePurchaseActivityItemsCategoryRedisService) {
		this.activitiesMapper = activitiesMapper;
		this.itemsMapper = itemsMapper;
		this.activityItemsMapper = activityItemsMapper;
		this.activityGoodsMapper = activityGoodsMapper;
		this.distributorItemsRepository = distributorItemsRepository;
		this.employeePurchaseActivityItemsCategoryRedisService = employeePurchaseActivityItemsCategoryRedisService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void acceptRow(long companyId, long distributorId, Map<String, Object> row) {
		String itemBn = trim(row.get("item_bn"));
		String priceRaw = trim(row.get("activity_price"));
		if (!StringUtils.hasText(itemBn)) {
			throw new BadRequestException("SKU编码不能为空");
		}
		if (!StringUtils.hasText(priceRaw)) {
			throw new BadRequestException("内购活动商品价格异常");
		}
		BigDecimal priceYuan;
		try {
			priceYuan = new BigDecimal(priceRaw);
		} catch (NumberFormatException e) {
			throw new BadRequestException("内购活动商品价格异常");
		}
		if (priceYuan.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BadRequestException("内购活动商品价格异常");
		}
		int activityPriceFen = priceYuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValue();

		long relationId = parseRelationId(row.get("relation_id"));
		Activities activity = requireActivity(companyId, relationId);
		boolean ifShareStore = Boolean.TRUE.equals(activity.getIfShareStore());
		Long activityPk = activity.getId();

		long rowDistributorId = parseLongOrZero(row.get("distributor_id"), distributorId);
		Items item = resolveImportItem(companyId, itemBn, rowDistributorId);

		int activityStore = 0;
		if (!ifShareStore) {
			String storeRaw = trim(row.get("activity_store"));
			if (!StringUtils.hasText(storeRaw)) {
				throw new BadRequestException("活动库存不能为空");
			}
			if (!isNumericNonNegative(storeRaw)) {
				throw new BadRequestException("活动库存必须大于等于0");
			}
			activityStore = new BigDecimal(storeRaw).intValue();
		}

		int limitFeeFen = parseMoneyToFenOrZero(row.get("limit_fee"));
		int limitNum = parseOptionalNonNegativeInt(row.get("limit_num"), 0);
		int sort = parseOptionalNonNegativeInt(row.get("sort"), 0);
		int shelfStatus = parseShelfStatus(row.get("shelf_status"));

		int now = (int) (System.currentTimeMillis() / 1000L);

		ActivityItems existing =
				activityItemsMapper.selectOne(
						Wrappers.<ActivityItems>lambdaQuery()
								.eq(ActivityItems::getCompanyId, companyId)
								.eq(ActivityItems::getActivityId, activityPk)
								.eq(ActivityItems::getItemId, item.getItemId())
								.last("LIMIT 1"));

		if (existing == null) {
			ActivityItems entity = new ActivityItems();
			entity.setActivityId(activityPk);
			entity.setItemId(item.getItemId());
			entity.setGoodsId(item.getGoodsId());
			entity.setCompanyId(companyId);
			entity.setActivityPrice(activityPriceFen);
			entity.setActivityStore(activityStore);
			entity.setLimitFee(limitFeeFen);
			entity.setLimitNum(limitNum);
			entity.setSort(sort);
			entity.setShelfStatus(shelfStatus);
			entity.setCreated(now);
			entity.setUpdated(now);
			activityItemsMapper.insert(entity);
		} else {
			var update = Wrappers.<ActivityItems>lambdaUpdate()
					.eq(ActivityItems::getCompanyId, companyId)
					.eq(ActivityItems::getActivityId, activityPk)
					.eq(ActivityItems::getItemId, item.getItemId())
					.set(ActivityItems::getGoodsId, item.getGoodsId())
					.set(ActivityItems::getActivityPrice, activityPriceFen)
					.set(ActivityItems::getLimitFee, limitFeeFen)
					.set(ActivityItems::getLimitNum, limitNum)
					.set(ActivityItems::getSort, sort)
					.set(ActivityItems::getShelfStatus, shelfStatus)
					.set(ActivityItems::getUpdated, now);
			if (!ifShareStore) {
				update.set(ActivityItems::getActivityStore, activityStore);
			}
			activityItemsMapper.update(null, update);
		}

		ActivityGoods goodsProbe =
				activityGoodsMapper.selectOne(
						Wrappers.<ActivityGoods>lambdaQuery()
								.eq(ActivityGoods::getCompanyId, companyId)
								.eq(ActivityGoods::getActivityId, activityPk)
								.eq(ActivityGoods::getGoodsId, item.getGoodsId())
								.last("LIMIT 1"));
		if (goodsProbe == null) {
			ActivityGoods g = new ActivityGoods();
			g.setActivityId(activityPk);
			g.setGoodsId(item.getGoodsId());
			g.setCompanyId(companyId);
			activityGoodsMapper.insert(g);
		}

		employeePurchaseActivityItemsCategoryRedisService.store(companyId, activityPk, rowDistributorId);
	}

	private Activities requireActivity(long companyId, long relationId) {
		Activities activity =
				activitiesMapper.selectOne(
						Wrappers.<Activities>lambdaQuery()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, relationId)
								.last("LIMIT 1"));
		if (activity == null) {
			throw new BadRequestException("内购活动不存在");
		}
		return activity;
	}

	/**
	 * 按 distributor_id 解析导入 SKU：平台走主商城商品库，店铺走店铺可售商品。
	 * 对齐 PHP {@code ActivityItemsUploadService::resolveImportItem}。
	 */
	private Items resolveImportItem(long companyId, String itemBn, long distributorId) {
		var query = Wrappers.<Items>lambdaQuery()
				.eq(Items::getCompanyId, companyId)
				.eq(Items::getItemBn, itemBn);
		if (distributorId == 0L) {
			query.eq(Items::getDistributorId, 0);
		}
		Items item = itemsMapper.selectOne(query.last("LIMIT 1"));
		if (item == null || item.getItemId() == null || item.getGoodsId() == null) {
			String label = distributorId > 0 ? "店铺" : "平台";
			throw new BadRequestException(label + "商品不存在:" + itemBn);
		}

		if (distributorId > 0L) {
			Optional<DistributorItems> distributorItem =
					distributorItemsRepository.findByDistributorIdAndCompanyIdAndItemId(
							distributorId, companyId, item.getItemId());
			long itemOwnDid = item.getDistributorId() == null ? 0L : item.getDistributorId().longValue();
			if (distributorItem.isEmpty() && itemOwnDid != distributorId) {
				throw new BadRequestException("店铺商品不存在:" + itemBn);
			}
		} else {
			long itemOwnDid = item.getDistributorId() == null ? 0L : item.getDistributorId().longValue();
			if (itemOwnDid != 0L) {
				throw new BadRequestException("平台商品不存在:" + itemBn);
			}
		}
		return item;
	}

	private static long parseRelationId(Object raw) {
		if (raw == null) {
			throw new BadRequestException("关联id不能为空");
		}
		try {
			long v = new BigDecimal(String.valueOf(raw).trim()).longValueExact();
			if (v <= 0) {
				throw new BadRequestException("关联id不能为空");
			}
			return v;
		} catch (ArithmeticException | NumberFormatException e) {
			throw new BadRequestException("关联id不能为空");
		}
	}

	private static int parseShelfStatus(Object raw) {
		String s = trim(raw);
		if (!StringUtils.hasText(s)) {
			return 1;
		}
		if (!"0".equals(s) && !"1".equals(s)) {
			throw new BadRequestException("状态只能填写0或1");
		}
		return Integer.parseInt(s);
	}

	private static boolean isNumericNonNegative(String s) {
		try {
			BigDecimal v = new BigDecimal(s);
			return v.compareTo(BigDecimal.ZERO) >= 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static int parseMoneyToFenOrZero(Object raw) {
		String s = trim(raw);
		if (!StringUtils.hasText(s)) {
			return 0;
		}
		try {
			BigDecimal v = new BigDecimal(s);
			if (v.compareTo(BigDecimal.ZERO) == 0) {
				return 0;
			}
			return v.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValue();
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int parseOptionalNonNegativeInt(Object raw, int defaultVal) {
		String s = trim(raw);
		if (!StringUtils.hasText(s)) {
			return defaultVal;
		}
		try {
			int v = new BigDecimal(s).intValue();
			if (v < 0) {
				return defaultVal;
			}
			return v;
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static long parseLongOrZero(Object raw, long fallback) {
		String s = trim(raw);
		if (!StringUtils.hasText(s)) {
			return fallback;
		}
		try {
			return new BigDecimal(s).longValue();
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static String trim(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}
}
