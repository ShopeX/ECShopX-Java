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

package cn.shopex.ecshopx.community.service;

import cn.shopex.ecshopx.common.cron.CommunitySettingReadPort;
import cn.shopex.ecshopx.common.dispatch.CancelActivityOrdersJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.orders.CommunityActivityCancelOrderRow;
import cn.shopex.ecshopx.community.domain.CommunityActivity;
import cn.shopex.ecshopx.community.domain.CommunityActivityItem;
import cn.shopex.ecshopx.community.domain.CommunityActivityZiti;
import cn.shopex.ecshopx.community.domain.CommunityChief;
import cn.shopex.ecshopx.community.domain.CommunityChiefZiti;
import cn.shopex.ecshopx.community.domain.CommunityItems;
import cn.shopex.ecshopx.community.domain.CommunityOrderRelActivity;
import cn.shopex.ecshopx.community.dto.ActivityGoodsBuyRow;
import cn.shopex.ecshopx.community.dto.ActivityItemNumRow;
import cn.shopex.ecshopx.community.dto.ActivityOrderAggRow;
import cn.shopex.ecshopx.community.dto.ActivityWriteOffRow;
import cn.shopex.ecshopx.community.dto.CommunityActivityAdminListQuery;
import cn.shopex.ecshopx.community.dto.MemberH5ActivityListOrderNumQuery;
import cn.shopex.ecshopx.community.dto.MemberH5ActivityListParams;
import cn.shopex.ecshopx.community.dto.ScheduleFinishGoodsRow;
import cn.shopex.ecshopx.community.dto.chief.ChiefActivityZitiInput;
import cn.shopex.ecshopx.community.dto.chief.CreateChiefActivityInput;
import cn.shopex.ecshopx.community.dto.chief.UpdateChiefActivityInput;
import cn.shopex.ecshopx.community.mapper.CommunityActivityItemMapper;
import cn.shopex.ecshopx.community.mapper.CommunityActivityMapper;
import cn.shopex.ecshopx.community.mapper.CommunityActivityOrderStatsMapper;
import cn.shopex.ecshopx.community.mapper.CommunityActivityZitiMapper;
import cn.shopex.ecshopx.community.mapper.CommunityChiefMapper;
import cn.shopex.ecshopx.community.mapper.CommunityChiefZitiMapper;
import cn.shopex.ecshopx.community.mapper.CommunityItemsMapper;
import cn.shopex.ecshopx.community.mapper.CommunityOrderRelActivityMapper;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsRelAttrValuesQueryService;
import com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class CommunityActivityService {

	private static final Logger log = LoggerFactory.getLogger(CommunityActivityService.class);

	private static final int MAX_CHIEF_ACTIVITY_ITEM_IDS = 2000;

	private static final DateTimeFormatter LIST_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final Map<String, String> ACTIVITY_STATUS_MSG =
			Map.ofEntries(
					Map.entry("private", "草稿箱"),
					Map.entry("public", "跟团中"),
					Map.entry("protected", "暂停中"),
					Map.entry("success", "已成团"),
					Map.entry("fail", "成团失败"));

	private static final Map<String, String> ACTIVITY_DELIVERY_STATUS_MSG =
			Map.of("PENDING", "未发货", "DONE", "已发货", "SUCCESS", "已收货");

	private static final List<String> ITEM_REPO_COLS =
			List.of(
					"item_id",
					"item_type",
					"consume_type",
					"is_show_specimg",
					"store",
					"barcode",
					"sales",
					"approve_status",
					"rebate",
					"rebate_conf",
					"cost_price",
					"is_point",
					"point",
					"item_source",
					"goods_id",
					"brand_id",
					"is_market",
					"item_name",
					"item_unit",
					"item_bn",
					"brief",
					"price",
					"market_price",
					"special_type",
					"goods_function",
					"goods_series",
					"volume",
					"supplier_id",
					"supplier_item_id",
					"goods_color",
					"goods_brand",
					"item_address_province",
					"item_address_city",
					"regions_id",
					"regions",
					"brand_logo",
					"sort",
					"templates_id",
					"is_default",
					"nospec",
					"default_item_id",
					"pics",
					"pics_create_qrcode",
					"distributor_id",
					"company_id",
					"enable_agreement",
					"date_type",
					"item_category",
					"rebate_type",
					"weight",
					"begin_date",
					"end_date",
					"fixed_term",
					"tax_rate",
					"created",
					"updated",
					"video_type",
					"videos",
					"video_pic_url",
					"audit_status",
					"audit_reason",
					"is_gift",
					"is_package",
					"profit_type",
					"profit_fee",
					"is_profit",
					"crossborder_tax_rate",
					"origincountry_id",
					"taxstrategy_id",
					"taxation_num",
					"type",
					"tdk_content",
					"is_epidemic",
					"goods_bn",
					"audit_date",
					"is_medicine",
					"is_prescription",
					"start_num",
					"delivery_time",
					"is_taobao");

	private final CommunityActivityMapper communityActivityMapper;
	private final CommunityActivityItemMapper communityActivityItemMapper;
	private final CommunityActivityZitiMapper communityActivityZitiMapper;
	private final CommunityChiefZitiMapper communityChiefZitiMapper;
	private final ItemsMapper itemsMapper;
	private final CommunityActivityOrderStatsMapper communityActivityOrderStatsMapper;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService;
	private final CommunitySettingService communitySettingService;
	private final CommunityChiefMapper communityChiefMapper;
	private final CommunityItemsMapper communityItemsMapper;
	private final CommunityOrderRelActivityMapper communityOrderRelActivityMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final CommunitySettingReadPort communitySettingReadPort;
	private final CancelActivityOrdersJobDispatchPublisher cancelActivityOrdersJobDispatchPublisher;

	private static final String CANCEL_ON_FAIL_MSG = "成团失败，取消订单并退款";
	private static final String CANCEL_NOTPAY_ON_SUCCESS_MSG = "已成团，取消未支付订单";

	public CommunityActivityService(
			CommunityActivityMapper communityActivityMapper,
			CommunityActivityItemMapper communityActivityItemMapper,
			CommunityActivityZitiMapper communityActivityZitiMapper,
			CommunityChiefZitiMapper communityChiefZitiMapper,
			ItemsMapper itemsMapper,
			CommunityActivityOrderStatsMapper communityActivityOrderStatsMapper,
			ObjectMapper objectMapper,
			TransactionTemplate transactionTemplate,
			ItemRelAttributesRepository itemRelAttributesRepository,
			ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService,
			CommunitySettingService communitySettingService,
			CommunityChiefMapper communityChiefMapper,
			CommunityItemsMapper communityItemsMapper,
			CommunityOrderRelActivityMapper communityOrderRelActivityMapper,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			MembersInfoMapper membersInfoMapper,
			CommunitySettingReadPort communitySettingReadPort,
			CancelActivityOrdersJobDispatchPublisher cancelActivityOrdersJobDispatchPublisher) {
		this.communityActivityMapper = communityActivityMapper;
		this.communityActivityItemMapper = communityActivityItemMapper;
		this.communityActivityZitiMapper = communityActivityZitiMapper;
		this.communityChiefZitiMapper = communityChiefZitiMapper;
		this.itemsMapper = itemsMapper;
		this.communityActivityOrderStatsMapper = communityActivityOrderStatsMapper;
		this.objectMapper = objectMapper;
		this.transactionTemplate = transactionTemplate;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.itemsRelAttrValuesQueryService = itemsRelAttrValuesQueryService;
		this.communitySettingService = communitySettingService;
		this.communityChiefMapper = communityChiefMapper;
		this.communityItemsMapper = communityItemsMapper;
		this.communityOrderRelActivityMapper = communityOrderRelActivityMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.communitySettingReadPort = communitySettingReadPort;
		this.cancelActivityOrdersJobDispatchPublisher = cancelActivityOrdersJobDispatchPublisher;
	}

	public Map<String, Object> createChiefActivity(CreateChiefActivityInput input) {
		List<Long> itemIds = input.itemIds();
		if (itemIds == null || itemIds.isEmpty()) {
			throw new BadRequestException("商品参数错误");
		}
		List<ResolvedItemRow> resolved = resolveItemRowsForCreation(input.companyId(), itemIds);
		return transactionTemplate.execute(
				status -> {
					try {
						return persistChiefActivityInTransaction(input, resolved);
					} catch (BadRequestException | ResourceException | ForbiddenException e) {
						throw e;
					} catch (RuntimeException e) {
						log.error("persist chief activity failed", e);
						throw new ResourceException("活动创建失败");
					}
				});
	}

	public CommunityActivity loadActivityForChiefUpdate(long companyId, long chiefId, long activityId) {
		CommunityActivity act = communityActivityMapper.selectById(activityId);
		if (act == null) {
			throw new ResourceException("无效的活动");
		}
		if (act.getCompanyId() == null || act.getCompanyId().longValue() != companyId) {
			throw new ResourceException("无效的活动");
		}
		assertChiefOwnsActivity(act, chiefId);
		return act;
	}

	private void assertChiefOwnsActivity(CommunityActivity activity, long authChiefId) {
		long rowChiefId = activity.getChiefId() == null ? 0L : activity.getChiefId().longValue();
		if (rowChiefId != authChiefId) {
			throw new ForbiddenException("只能修改自己的拼团活动");
		}
	}

	/**
	 * Chief H5: set activity to {@code success} or {@code fail}; enqueues cancel work on the dispatch bus after commit.
	 */
	public Map<String, Object> updateChiefActivityStatus(
			long companyId,
			long chiefId,
			long activityId,
			String activityStatus,
			@SuppressWarnings("unused") Map<String, Object> requestBodyHint) {
		if (!StringUtils.hasText(activityStatus) || !ACTIVITY_STATUS_MSG.containsKey(activityStatus)) {
			throw new ResourceException("活动状态错误");
		}
		CommunityActivity activity = loadActivityForChiefUpdate(companyId, chiefId, activityId);
		if ("success".equals(activityStatus)) {
			Object distributorRaw = activity.getDistributorId();
			validateChiefManualSuccessConditions(activityId, companyId, distributorRaw);
		}
		List<List<CommunityActivityCancelOrderRow>> pendingBatches = new ArrayList<>();
		boolean[] syncRegistered = { false };
		transactionTemplate.executeWithoutResult(
				st -> {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						syncRegistered[0] = true;
						TransactionSynchronizationManager.registerSynchronization(
								new TransactionSynchronization() {
									@Override
									public void afterCommit() {
										publishChiefCancelBatches(pendingBatches);
									}
								});
					}
					int nowSec = (int) (System.currentTimeMillis() / 1000L);
					LambdaUpdateWrapper<CommunityActivity> uw = new LambdaUpdateWrapper<>();
					uw.eq(CommunityActivity::getActivityId, activityId)
							.set(CommunityActivity::getActivityStatus, activityStatus)
							.set(CommunityActivity::getUpdatedAt, nowSec);
					communityActivityMapper.update(null, uw);
					if ("fail".equals(activityStatus)) {
						buildChiefCancelOrderBatches(activityId, ActivityOrderCancelScope.FAIL, chiefId, pendingBatches);
					} else if ("success".equals(activityStatus)) {
						buildChiefCancelOrderBatches(activityId, ActivityOrderCancelScope.SUCCESS, chiefId, pendingBatches);
					}
				});
		if (!syncRegistered[0]) {
			publishChiefCancelBatches(pendingBatches);
		}
		return Map.of("activity_id", activityId, "activity_status", activityStatus);
	}

	private void validateChiefManualSuccessConditions(long activityId, long companyId, Object distributorIdRaw) {
		List<ScheduleFinishGoodsRow> itemList =
				communityActivityOrderStatsMapper.listScheduleFinishGoodsForActivity(activityId);
		if (itemList == null || itemList.isEmpty()) {
			throw new ResourceException("没有商品被购买，不能成团");
		}
		Map<String, Object> setting = communitySettingReadPort.getSetting(companyId, true, distributorIdRaw);
		String conditionType = Objects.toString(setting.get("condition_type"), "num");
		if ("money".equalsIgnoreCase(conditionType)) {
			BigDecimal condMoney = parseConditionMoney(setting.get("condition_money"));
			if (condMoney.compareTo(BigDecimal.ZERO) > 0) {
				long totalFen = 0L;
				for (ScheduleFinishGoodsRow r : itemList) {
					totalFen += (long) r.getTotalFeeCents();
				}
				long thresholdFen = condMoney.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
				if (totalFen < thresholdFen) {
					throw new ResourceException("未达最低成团金额，不能成团");
				}
			}
		} else {
			for (ScheduleFinishGoodsRow item : itemList) {
				if (item.getMinDeliveryNum() > 0 && item.getBuyNum() < item.getMinDeliveryNum()) {
					throw new ResourceException("未达起送量，不能成团");
				}
			}
		}
	}

	/**
	 * Sends each assembled batch to {@link CancelActivityOrdersJobDispatchPublisher#publishBatch}; cancel work is
	 * scheduled asynchronously on queue {@code slow} through the dispatch bus (not executed in the calling thread).
	 */
	private void publishChiefCancelBatches(List<List<CommunityActivityCancelOrderRow>> batches) {
		for (List<CommunityActivityCancelOrderRow> batch : batches) {
			if (batch != null && !batch.isEmpty()) {
				cancelActivityOrdersJobDispatchPublisher.publishBatch(batch);
			}
		}
	}

	/**
	 * When transaction synchronization is active, registers {@link CancelActivityOrdersJobDispatchPublisher#publishBatch}
	 * to run on {@link org.springframework.transaction.support.TransactionSynchronization#afterCommit()}; otherwise
	 * invokes {@code publishBatch} immediately. Batches are published through the publisher implementation, which
	 * enqueues cancel work on the {@code slow} queue via the platform job dispatch entry point ({@code dispatchJob}),
	 * not on the calling thread.
	 */
	private void registerPublishCancelBatchAfterCommit(List<CommunityActivityCancelOrderRow> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							cancelActivityOrdersJobDispatchPublisher.publishBatch(rows);
						}
					});
		} else {
			cancelActivityOrdersJobDispatchPublisher.publishBatch(rows);
		}
	}

	private void buildChiefCancelOrderBatches(
			long activityId,
			ActivityOrderCancelScope scope,
			long chiefId,
			List<List<CommunityActivityCancelOrderRow>> outPendingBatches) {
		LambdaQueryWrapper<NormalOrders> w = new LambdaQueryWrapper<>();
		w.eq(NormalOrders::getOrderClass, "community");
		w.eq(NormalOrders::getOrderType, "normal");
		w.eq(NormalOrders::getActId, activityId);
		w.in(NormalOrders::getCancelStatus, "NO_APPLY_CANCEL", "FAILS");
		if (scope == ActivityOrderCancelScope.FAIL) {
			w.in(NormalOrders::getOrderStatus, "NOTPAY", "PAYED");
		} else {
			w.eq(NormalOrders::getOrderStatus, "NOTPAY");
		}
		w.orderByAsc(NormalOrders::getCreateTime);
		long count = normalOrdersMapper.selectCount(w);
		if (count == 0) {
			return;
		}
		int pageSize = 50;
		long pages = (count + pageSize - 1) / pageSize;
		String reason =
				scope == ActivityOrderCancelScope.FAIL ? CANCEL_ON_FAIL_MSG : CANCEL_NOTPAY_ON_SUCCESS_MSG;
		for (long p = 1; p <= pages; p++) {
			Page<NormalOrders> page = new Page<>(p, pageSize, false);
			List<NormalOrders> list = normalOrdersMapper.selectPage(page, w).getRecords();
			if (list == null || list.isEmpty()) {
				continue;
			}
			List<CommunityActivityCancelOrderRow> rows = new ArrayList<>();
			for (NormalOrders o : list) {
				CommunityActivityCancelOrderRow r = new CommunityActivityCancelOrderRow();
				r.setCompanyId(o.getCompanyId() == null ? 0L : o.getCompanyId().longValue());
				r.setOrderId(o.getOrderId() == null ? 0L : o.getOrderId().longValue());
				r.setCancelReason(reason);
				r.setUserId(o.getUserId() == null ? 0L : o.getUserId().longValue());
				r.setMobile(o.getMobile() == null ? "" : o.getMobile());
				if (chiefId > 0L) {
					r.setCancelFrom("chief");
					r.setChiefId(chiefId);
				} else {
					r.setCancelFrom("system");
				}
				rows.add(r);
			}
			outPendingBatches.add(rows);
		}
	}

	public Map<String, Object> updateChiefActivity(UpdateChiefActivityInput input) {
		List<Long> itemIds = input.itemIds();
		if (itemIds == null || itemIds.isEmpty()) {
			throw new BadRequestException("商品参数错误");
		}
		List<ResolvedItemRow> resolved = resolveItemRowsForCreation(input.companyId(), itemIds);
		return transactionTemplate.execute(
				status -> {
					try {
						return persistChiefActivityUpdateInTransaction(input, resolved);
					} catch (BadRequestException | ResourceException | ForbiddenException e) {
						throw e;
					} catch (RuntimeException e) {
						log.error("update chief activity failed", e);
						throw new ResourceException("活动更新失败");
					}
				});
	}

	private Map<String, Object> persistChiefActivityUpdateInTransaction(
			UpdateChiefActivityInput input, List<ResolvedItemRow> resolved) {
		long activityId = input.activityId();
		CommunityActivity activity = communityActivityMapper.selectById(activityId);
		if (activity == null) {
			throw new ResourceException("无效的活动");
		}
		if (activity.getCompanyId() == null || activity.getCompanyId().longValue() != input.companyId()) {
			throw new ResourceException("无效的活动");
		}
		assertChiefOwnsActivity(activity, input.chiefId());

		Integer bodyDist = input.distributorIdFromBodyOrNull();
		if (bodyDist != null) {
			int rowDist = activity.getDistributorId() == null ? 0 : activity.getDistributorId();
			if (rowDist != bodyDist.intValue()) {
				throw new ResourceException("不能修改活动绑定的店铺");
			}
		}

		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		String activityPics =
				StringUtils.hasText(input.activityPicsOrNull()) ? input.activityPicsOrNull() : "[]";
		String activityDesc =
				StringUtils.hasText(input.activityDescOrNull()) ? input.activityDescOrNull() : "";

		LambdaUpdateWrapper<CommunityActivity> u = new LambdaUpdateWrapper<>();
		u.eq(CommunityActivity::getActivityId, activityId);
		u.set(CommunityActivity::getActivityName, input.activityName());
		u.set(CommunityActivity::getStartTime, input.startTime());
		u.set(CommunityActivity::getEndTime, input.endTime());
		u.set(CommunityActivity::getActivityPics, activityPics);
		u.set(CommunityActivity::getActivityDesc, activityDesc);
		if (input.activityIntroKeyPresent()) {
			u.set(CommunityActivity::getActivityIntro, input.activityIntroOrNull());
		}
		if (StringUtils.hasText(input.activityStatusOrNull())) {
			u.set(CommunityActivity::getActivityStatus, input.activityStatusOrNull());
		}
		if (bodyDist != null) {
			u.set(CommunityActivity::getDistributorId, bodyDist);
		}
		u.set(CommunityActivity::getUpdatedAt, nowSec);

		int affected = communityActivityMapper.update(null, u);
		if (affected == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		int activityDistributorId = activity.getDistributorId() == null ? 0 : activity.getDistributorId();

		List<CommunityActivityItem> oldItems =
				communityActivityItemMapper.selectList(
						new LambdaQueryWrapper<CommunityActivityItem>()
								.eq(CommunityActivityItem::getActivityId, activityId));
		/* 同一活动下若存在重复 item_id 行，后遍历覆盖映射 */
		Map<Long, Long> itemRowIdBySkuItemId = new LinkedHashMap<>();
		for (CommunityActivityItem row : oldItems) {
			if (row.getItemId() != null && row.getId() != null) {
				itemRowIdBySkuItemId.put(row.getItemId(), row.getId());
			}
		}
		Set<Long> updateRowIds = new HashSet<>();
		for (ResolvedItemRow row : resolved) {
			Items it = row.item();
			Integer itemDist = it.getDistributorId();
			int itemDistVal = itemDist == null ? 0 : itemDist;
			if (itemDistVal != activityDistributorId) {
				throw new ResourceException("只能选择一个店铺的商品开团");
			}
			Long skuItemId = it.getItemId();
			if (skuItemId == null || skuItemId <= 0L) {
				throw new BadRequestException("商品参数错误");
			}
			Long existId = itemRowIdBySkuItemId.get(skuItemId);
			if (existId != null) {
				CommunityActivityItem line = new CommunityActivityItem();
				line.setId(existId);
				line.setActivityId(activityId);
				line.setGoodsId(it.getGoodsId());
				line.setItemId(it.getItemId());
				line.setItemName(it.getItemName());
				line.setItemSpecDesc(row.itemSpecDesc());
				line.setItemBrief(it.getBrief());
				line.setItemPics(it.getPics());
				line.setPrice(it.getPrice());
				line.setCostPrice(it.getCostPrice());
				line.setMarketPrice(it.getMarketPrice());
				line.setStore(it.getStore());
				line.setUpdatedAt(nowSec);
				communityActivityItemMapper.updateById(line);
				updateRowIds.add(existId);
			} else {
				CommunityActivityItem line = new CommunityActivityItem();
				line.setActivityId(activityId);
				line.setGoodsId(it.getGoodsId());
				line.setItemId(it.getItemId());
				line.setItemName(it.getItemName());
				line.setItemSpecDesc(row.itemSpecDesc());
				line.setItemBrief(it.getBrief());
				line.setItemPics(it.getPics());
				line.setPrice(it.getPrice());
				line.setCostPrice(it.getCostPrice());
				line.setMarketPrice(it.getMarketPrice());
				line.setStore(it.getStore());
				line.setCreatedAt(nowSec);
				line.setUpdatedAt(nowSec);
				int ir = communityActivityItemMapper.insert(line);
				if (ir <= 0) {
					throw new ResourceException("活动更新失败");
				}
			}
		}
		Set<Long> oldRowIdSet = new HashSet<>(itemRowIdBySkuItemId.values());
		List<Long> toDeleteItems = oldRowIdSet.stream().filter(id -> !updateRowIds.contains(id)).toList();
		if (!resolved.isEmpty() && !toDeleteItems.isEmpty()) {
			communityActivityItemMapper.deleteBatchIds(toDeleteItems);
		}

		List<CommunityActivityZiti> activityZitiList =
				communityActivityZitiMapper.selectList(
						new LambdaQueryWrapper<CommunityActivityZiti>()
								.eq(CommunityActivityZiti::getActivityId, activityId));
		/* 同一活动下若存在重复 ziti_id 行，后遍历覆盖映射 */
		Map<Long, Long> zitiRowIdByZitiId = new LinkedHashMap<>();
		for (CommunityActivityZiti zr : activityZitiList) {
			if (zr.getZitiId() != null && zr.getId() != null) {
				zitiRowIdByZitiId.put(zr.getZitiId(), zr.getId());
			}
		}
		Set<Long> keptZitiRowIds = new HashSet<>();
		List<CommunityActivityZiti> zitiToInsert = new ArrayList<>();
		for (ChiefActivityZitiInput z : input.zitiRows()) {
			long zitiId = z.zitiId();
			if (zitiId <= 0L) {
				throw new BadRequestException("自提点参数错误");
			}
			Long existRowId = zitiRowIdByZitiId.get(zitiId);
			if (existRowId != null) {
				keptZitiRowIds.add(existRowId);
			} else {
				CommunityActivityZiti nz = new CommunityActivityZiti();
				nz.setActivityId(activityId);
				nz.setZitiId(zitiId);
				zitiToInsert.add(nz);
			}
		}
		for (CommunityActivityZiti nz : zitiToInsert) {
			int zins = communityActivityZitiMapper.insert(nz);
			if (zins <= 0) {
				throw new ResourceException("活动更新失败");
			}
		}
		Set<Long> oldZitiRowIds = new HashSet<>();
		for (CommunityActivityZiti zr : activityZitiList) {
			if (zr.getId() != null) {
				oldZitiRowIds.add(zr.getId());
			}
		}
		List<Long> candidateDeleteZiti =
				oldZitiRowIds.stream().filter(id -> !keptZitiRowIds.contains(id)).toList();
		if (!input.zitiRows().isEmpty() && !candidateDeleteZiti.isEmpty()) {
			communityActivityZitiMapper.deleteBatchIds(candidateDeleteZiti);
		}

		CommunityActivity refreshed = communityActivityMapper.selectById(activityId);
		if (refreshed == null) {
			throw new ResourceException("无效的活动");
		}
		return toColumnNamesMap(refreshed);
	}

	private List<ResolvedItemRow> resolveItemRowsForCreation(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			throw new BadRequestException("商品参数错误");
		}
		if (itemIds.size() > MAX_CHIEF_ACTIVITY_ITEM_IDS) {
			throw new BadRequestException("商品参数错误");
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getDefaultItemId, itemIds).last("LIMIT 2000");
		List<Items> rows = itemsMapper.selectList(w);
		if (rows.size() != itemIds.size()) {
			throw new BadRequestException("商品参数错误");
		}
		List<Long> skuItemIds = rows.stream().map(Items::getItemId).filter(Objects::nonNull).toList();
		List<ItemRelAttributes> allRel =
				itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, skuItemIds, "item_spec");
		Map<Long, List<ItemRelAttributes>> relByItem =
				allRel.stream().collect(Collectors.groupingBy(ItemRelAttributes::getItemId));

		List<ResolvedItemRow> out = new ArrayList<>();
		for (Items item : rows) {
			long skuId = item.getItemId() == null ? 0L : item.getItemId();
			List<ItemRelAttributes> relForSku = relByItem.getOrDefault(skuId, List.of());
			String specDesc = buildItemSpecDescForItem(companyId, skuId, relForSku);
			out.add(new ResolvedItemRow(item, specDesc));
		}
		return out;
	}

	private String buildItemSpecDescForItem(long companyId, long itemId, List<ItemRelAttributes> relRowsForItem) {
		if (relRowsForItem == null || relRowsForItem.isEmpty()) {
			return "";
		}
		ItemsRelAttrValuesQueryService.ItemDetailAttrData ad =
				itemsRelAttrValuesQueryService.assemble(companyId, relRowsForItem);
		List<Map<String, Object>> specList =
				new ArrayList<>(ad.itemSpecNested.getOrDefault(itemId, Map.of()).values());
		StringBuilder desc = new StringBuilder();
		for (Map<String, Object> s : specList) {
			if (desc.length() > 0) {
				desc.append(',');
			}
			Object n = s.get("spec_name");
			Object v = s.get("spec_value_name");
			desc.append(n != null ? n : "").append(':').append(v != null ? v : "");
		}
		return desc.toString();
	}

	private Map<String, Object> persistChiefActivityInTransaction(
			CreateChiefActivityInput input, List<ResolvedItemRow> resolved) {
		String activityDesc = StringUtils.hasText(input.activityDesc()) ? input.activityDesc() : "";
		String activityPics = StringUtils.hasText(input.activityPics()) ? input.activityPics() : "[]";
		String activityIntro = input.activityIntro();
		String activityStatus = StringUtils.hasText(input.activityStatus()) ? input.activityStatus() : "public";

		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		CommunityActivity entity = new CommunityActivity();
		entity.setCompanyId(input.companyId());
		entity.setDistributorId(input.distributorId());
		entity.setChiefId(input.chiefId());
		entity.setActivityName(input.activityName());
		entity.setStartTime(input.startTime());
		entity.setEndTime(input.endTime());
		entity.setActivityPics(activityPics);
		entity.setActivityDesc(activityDesc);
		entity.setActivityIntro(activityIntro);
		entity.setActivityStatus(activityStatus);
		entity.setCreatedAt(nowSec);
		entity.setUpdatedAt(nowSec);

		int inserted = communityActivityMapper.insert(entity);
		if (inserted <= 0 || entity.getActivityId() == null || entity.getActivityId() <= 0L) {
			throw new ResourceException("活动创建失败");
		}
		long activityId = entity.getActivityId();
		int activityDistributorId = input.distributorId();

		for (ResolvedItemRow row : resolved) {
			Items it = row.item();
			Integer itemDist = it.getDistributorId();
			int itemDistVal = itemDist == null ? 0 : itemDist;
			if (itemDistVal != activityDistributorId) {
				throw new ResourceException("只能选择一个店铺的商品开团");
			}
		}

		for (ResolvedItemRow row : resolved) {
			Items it = row.item();
			CommunityActivityItem line = new CommunityActivityItem();
			line.setActivityId(activityId);
			line.setGoodsId(it.getGoodsId());
			line.setItemId(it.getItemId());
			line.setItemName(it.getItemName());
			line.setItemSpecDesc(row.itemSpecDesc());
			line.setItemBrief(it.getBrief());
			line.setItemPics(it.getPics());
			line.setPrice(it.getPrice());
			line.setCostPrice(it.getCostPrice());
			line.setMarketPrice(it.getMarketPrice());
			line.setStore(it.getStore());
			line.setCreatedAt(nowSec);
			line.setUpdatedAt(nowSec);
			int ir = communityActivityItemMapper.insert(line);
			if (ir <= 0) {
				throw new ResourceException("活动创建失败");
			}
		}

		for (ChiefActivityZitiInput z : input.zitiRows()) {
			if (z.zitiId() <= 0L) {
				throw new BadRequestException("自提点参数错误");
			}
			CommunityActivityZiti zr = new CommunityActivityZiti();
			zr.setActivityId(activityId);
			zr.setZitiId(z.zitiId());
			zr.setConditionNum(z.conditionNum());
			zr.setRemark(z.remark());
			int zins = communityActivityZitiMapper.insert(zr);
			if (zins <= 0) {
				throw new ResourceException("活动创建失败");
			}
		}

		return toColumnNamesMap(entity);
	}

	private record ResolvedItemRow(Items item, String itemSpecDesc) {}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateConfirmStatus(
			long companyId, boolean distributorBranch, Integer distributorIdWhenBranch, long activityId) {
		LambdaQueryWrapper<CommunityActivity> q = new LambdaQueryWrapper<>();
		q.eq(CommunityActivity::getCompanyId, companyId).eq(CommunityActivity::getActivityId, activityId);
		applyDistributorIdFilter(q, distributorBranch, distributorIdWhenBranch);
		CommunityActivity row = communityActivityMapper.selectOne(q);
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}

		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<CommunityActivity> u = new LambdaUpdateWrapper<>();
		u.eq(CommunityActivity::getCompanyId, companyId).eq(CommunityActivity::getActivityId, activityId);
		applyDistributorIdFilter(u, distributorBranch, distributorIdWhenBranch);
		u.set(CommunityActivity::getDeliveryStatus, "DONE").set(CommunityActivity::getDeliveryTime, nowSec);

		communityActivityMapper.update(null, u);

		CommunityActivity updated = communityActivityMapper.selectById(activityId);
		if (updated == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toColumnNamesMap(updated);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> chiefConfirmDelivery(long companyId, long chiefId, long activityId) {
		CommunityActivity row = communityActivityMapper.selectById(activityId);
		if (row == null) {
			throw new ResourceException("无效的活动");
		}
		if (row.getCompanyId() == null || row.getCompanyId().longValue() != companyId) {
			throw new ResourceException("无效的活动");
		}
		long rowChief = row.getChiefId() == null ? 0L : row.getChiefId().longValue();
		if (rowChief != chiefId) {
			throw new ResourceException("没有当前活动的操作权限");
		}
		if (!"success".equals(row.getActivityStatus())) {
			throw new ResourceException("未成团的活动不能确认收货");
		}
		if (!"DONE".equals(row.getDeliveryStatus())) {
			throw new ResourceException("未发货的活动不能确认收货");
		}
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<CommunityActivity> u = new LambdaUpdateWrapper<>();
		u.eq(CommunityActivity::getActivityId, activityId);
		u.set(CommunityActivity::getDeliveryStatus, "SUCCESS");
		u.set(CommunityActivity::getUpdatedAt, nowSec);
		int affected = communityActivityMapper.update(null, u);
		if (affected == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		CommunityActivity updated = communityActivityMapper.selectById(activityId);
		if (updated == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toColumnNamesMap(updated);
	}

	@Transactional(rollbackFor = Exception.class)
	public void deliverChief(int distributorIdParam, long activityId) {
		LambdaQueryWrapper<CommunityActivity> q = new LambdaQueryWrapper<>();
		q.eq(CommunityActivity::getActivityId, activityId);
		CommunityActivity row = communityActivityMapper.selectOne(q);
		if (row == null) {
			throw new ResourceException("无效的活动");
		}

		int rowDist = row.getDistributorId() == null ? 0 : row.getDistributorId();
		if (rowDist != distributorIdParam) {
			throw new ForbiddenException("没有当前活动的操作权限");
		}

		if (!"success".equals(row.getActivityStatus())) {
			throw new ResourceException("未成团的活动不能发货");
		}

		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<CommunityActivity> u = new LambdaUpdateWrapper<>();
		u.eq(CommunityActivity::getActivityId, activityId);
		u.set(CommunityActivity::getDeliveryStatus, "DONE").set(CommunityActivity::getDeliveryTime, nowSec);
		int affected = communityActivityMapper.update(null, u);
		if (affected == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	public Map<String, Object> getActivityList(
			long companyId, CommunityActivityAdminListQuery query, int page, int pageSize) {
		LambdaQueryWrapper<CommunityActivity> w = new LambdaQueryWrapper<>();
		w.eq(CommunityActivity::getCompanyId, companyId);
		applyDistributorIdFilter(w, query.isDistributorOperator(), query.getDistributorIdFilter());

		if (query.getCreatedAtGte() != null) {
			w.ge(CommunityActivity::getCreatedAt, query.getCreatedAtGte().intValue());
		}
		if (query.isFilterCreatedAtLteWithNull()) {
			w.apply("created_at <= NULL");
		} else if (query.getCreatedAtLte() != null) {
			w.le(CommunityActivity::getCreatedAt, query.getCreatedAtLte().intValue());
		}

		String activityStatusParam = query.getActivityStatus();
		if (StringUtils.hasText(activityStatusParam)) {
			int now = (int) (System.currentTimeMillis() / 1000L);
			switch (activityStatusParam.trim()) {
				case "waiting" ->
						w.ge(CommunityActivity::getStartTime, now).ge(CommunityActivity::getEndTime, now);
				case "ongoing" ->
						w.le(CommunityActivity::getStartTime, now).ge(CommunityActivity::getEndTime, now);
				case "end" -> w.le(CommunityActivity::getStartTime, now).le(CommunityActivity::getEndTime, now);
				default -> {
					/* no extra filter */
				}
			}
		}

		if (query.isSuccess()) {
			w.eq(CommunityActivity::getActivityStatus, "success");
		}

		applyActivityNameContains(w, query.getActivityNameContains());

		w.orderByDesc(CommunityActivity::getActivityId);

		Page<CommunityActivity> mpPage = communityActivityMapper.selectPage(new Page<>(page, pageSize), w);
		long total = mpPage.getTotal();
		List<CommunityActivity> records = mpPage.getRecords();

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (records.isEmpty()) {
			out.put("list", List.of());
			return out;
		}
		out.put("list", buildActivityListPayload(companyId, records));
		return out;
	}

	public Map<String, Object> getChiefH5ActivityList(
			long companyId,
			long chiefId,
			String activityStatusOrNull,
			String orderByOrNull,
			int page,
			int pageSize) {
		String activityStatusParam = StringUtils.hasText(activityStatusOrNull) ? activityStatusOrNull.trim() : null;
		boolean orderByOrderNum = StringUtils.hasText(orderByOrNull) && "order_num".equals(orderByOrNull.trim());

		if (orderByOrderNum) {
			long offset = (long) (page - 1) * pageSize;
			long totalCount = communityActivityMapper.countChiefActivitiesByOrderNum(chiefId, activityStatusParam);
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", totalCount);
			if (totalCount == 0L) {
				out.put("list", List.of());
				return out;
			}
			List<Long> orderedActivityIds =
					communityActivityMapper.selectChiefActivityIdsPageByOrderNum(
							chiefId, activityStatusParam, offset, pageSize);
			if (orderedActivityIds == null || orderedActivityIds.isEmpty()) {
				out.put("list", List.of());
				return out;
			}
			List<CommunityActivity> batch = communityActivityMapper.selectBatchIds(orderedActivityIds);
			Map<Long, CommunityActivity> byId = new LinkedHashMap<>();
			for (CommunityActivity ca : batch) {
				if (ca != null && ca.getActivityId() != null) {
					byId.put(ca.getActivityId(), ca);
				}
			}
			List<CommunityActivity> orderedRecords = new ArrayList<>();
			for (Long id : orderedActivityIds) {
				CommunityActivity ca = byId.get(id);
				if (ca != null) {
					orderedRecords.add(ca);
				}
			}
			out.put("list", buildActivityListPayload(companyId, orderedRecords));
			return out;
		}

		LambdaQueryWrapper<CommunityActivity> w = new LambdaQueryWrapper<>();
		w.eq(CommunityActivity::getChiefId, chiefId);
		if (activityStatusParam != null) {
			w.eq(CommunityActivity::getActivityStatus, activityStatusParam);
		}
		w.orderByDesc(CommunityActivity::getCreatedAt);
		long totalCount = communityActivityMapper.selectCount(w);
		Page<CommunityActivity> mpPage =
				communityActivityMapper.selectPage(new Page<>(page, pageSize, false), w);
		List<CommunityActivity> records = mpPage.getRecords();
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		if (records.isEmpty()) {
			out.put("list", List.of());
			return out;
		}
		out.put("list", buildActivityListPayload(companyId, records));
		return out;
	}

	public Map<String, Object> getMemberH5ActivityList(
			long companyId,
			Long optionalChiefIdOrNull,
			MemberH5ActivityListParams params,
			int page,
			int pageSize) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		String tab = params.tabStatusNormalized() == null ? "all" : params.tabStatusNormalized();

		if (params.orderByOrderNum()) {
			MemberH5ActivityListOrderNumQuery q = new MemberH5ActivityListOrderNumQuery();
			q.setCompanyId(companyId);
			q.setChiefId(optionalChiefIdOrNull);
			q.setTabStatus(tab);
			q.setNow(now);
			String nameContains = params.activityNameContainsOrNull();
			if (nameContains != null && !nameContains.isBlank()) {
				q.setActivityNameLikeOrNull("%" + nameContains + "%");
			} else {
				q.setActivityNameLikeOrNull(null);
			}
			long offset = (long) (page - 1) * pageSize;
			long totalCount = communityActivityMapper.countMemberActivitiesByOrderNum(q);
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", totalCount);
			if (totalCount == 0L) {
				out.put("list", List.of());
				return out;
			}
			List<Long> orderedActivityIds =
					communityActivityMapper.selectMemberActivityIdsPageByOrderNum(q, offset, pageSize);
			if (orderedActivityIds == null || orderedActivityIds.isEmpty()) {
				out.put("list", List.of());
				return out;
			}
			List<CommunityActivity> batch = communityActivityMapper.selectBatchIds(orderedActivityIds);
			Map<Long, CommunityActivity> byId = new LinkedHashMap<>();
			for (CommunityActivity ca : batch) {
				if (ca != null && ca.getActivityId() != null) {
					byId.put(ca.getActivityId(), ca);
				}
			}
			List<CommunityActivity> orderedRecords = new ArrayList<>();
			for (Long id : orderedActivityIds) {
				CommunityActivity ca = byId.get(id);
				if (ca != null) {
					orderedRecords.add(ca);
				}
			}
			out.put("list", buildActivityListPayload(companyId, orderedRecords));
			return out;
		}

		LambdaQueryWrapper<CommunityActivity> w = new LambdaQueryWrapper<>();
		w.eq(CommunityActivity::getCompanyId, companyId);
		if (optionalChiefIdOrNull != null) {
			w.eq(CommunityActivity::getChiefId, optionalChiefIdOrNull);
		}
		applyMemberTabStatusFilters(w, tab, now);
		applyActivityNameContains(w, params.activityNameContainsOrNull());
		w.orderByDesc(CommunityActivity::getCreatedAt);
		long total = communityActivityMapper.selectCount(w);
		Page<CommunityActivity> mpPage =
				communityActivityMapper.selectPage(new Page<>(page, pageSize, false), w);
		List<CommunityActivity> records = mpPage.getRecords();
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (records.isEmpty()) {
			out.put("list", List.of());
			return out;
		}
		out.put("list", buildActivityListPayload(companyId, records));
		return out;
	}

	private void applyMemberTabStatusFilters(LambdaQueryWrapper<CommunityActivity> w, String tab, int now) {
		String t = tab == null || tab.isBlank() ? "all" : tab;
		switch (t) {
			case "waiting" ->
					w.eq(CommunityActivity::getActivityStatus, "public")
							.gt(CommunityActivity::getStartTime, now);
			case "end" ->
					w.in(CommunityActivity::getActivityStatus, List.of("public", "success", "fail"))
							.lt(CommunityActivity::getEndTime, now);
			case "running" ->
					w.eq(CommunityActivity::getActivityStatus, "public")
							.le(CommunityActivity::getStartTime, now)
							.ge(CommunityActivity::getEndTime, now);
			case "all" ->
					w.in(CommunityActivity::getActivityStatus, List.of("public", "success", "fail"));
			default ->
					w.in(CommunityActivity::getActivityStatus, List.of("public", "success", "fail"));
		}
	}

	public Map<String, Object> getH5ActivityDetailMap(long companyId, long activityId) {
		CommunityActivity act = communityActivityMapper.selectById(activityId);
		if (act == null) {
			throw new ResourceException("无效的活动");
		}
		if (act.getCompanyId() == null || act.getCompanyId().longValue() != companyId) {
			throw new ResourceException("无效的活动");
		}
		Map<String, Object> activity = new LinkedHashMap<>(toColumnNamesMap(act));
		Long chiefId = act.getChiefId();
		if (chiefId == null || chiefId <= 0L) {
			activity.put("chief_info", null);
		} else {
			CommunityChief chief = communityChiefMapper.selectById(chiefId);
			activity.put("chief_info", chief == null ? null : chiefEntityToSnakeMap(chief));
		}
		Map<String, Object> setting = communitySettingService.getSetting(companyId, true, act.getDistributorId());
		activity.putAll(setting);

		List<CommunityActivityItem> activityItemRows =
				communityActivityItemMapper.selectList(
						new LambdaQueryWrapper<CommunityActivityItem>()
								.eq(CommunityActivityItem::getActivityId, activityId));
		List<ActivityItemNumRow> itemNumRows = communityActivityOrderStatsMapper.staticsActivityItemNums(activityId);
		Map<Long, Long> itemNumByItemId = new LinkedHashMap<>();
		for (ActivityItemNumRow r : itemNumRows) {
			itemNumByItemId.put(r.getItemId(), r.getItemNum());
		}
		Map<String, Map<String, Object>> validItems =
				buildValidItemsByGoods(companyId, activityId, activityItemRows);

		String conditionType = Objects.toString(activity.get("condition_type"), "num");
		if ("money".equals(conditionType)) {
			List<ActivityGoodsBuyRow> buyRows =
					communityActivityOrderStatsMapper.sumCommunityGoodsBuyForActivity(activityId);
			Map<Long, ActivityGoodsBuyRow> buyByGoods = new LinkedHashMap<>();
			for (ActivityGoodsBuyRow r : buyRows) {
				buyByGoods.put(r.getGoodsId(), r);
			}
			for (Map<String, Object> item : validItems.values()) {
				Object gidObj = item.get("goods_id");
				long g = gidObj instanceof Number ? ((Number) gidObj).longValue() : 0L;
				ActivityGoodsBuyRow br = buyByGoods.get(g);
				if (br != null) {
					item.put("buy_num", br.getBuyNum());
					item.put("total_fee", br.getTotalFee() != null ? br.getTotalFee() : BigDecimal.ZERO);
				} else {
					item.put("buy_num", 0L);
					item.put("total_fee", BigDecimal.ZERO);
				}
			}
		} else {
			Long actCompanyId = act.getCompanyId();
			List<Long> goodsIds =
					activityItemRows.stream()
							.map(CommunityActivityItem::getGoodsId)
							.filter(Objects::nonNull)
							.distinct()
							.toList();
			Map<Long, Integer> minDeliveryByGoods = new LinkedHashMap<>();
			if (actCompanyId != null && !goodsIds.isEmpty()) {
				List<CommunityItems> ciList =
						communityItemsMapper.selectList(
								new LambdaQueryWrapper<CommunityItems>()
										.eq(CommunityItems::getCompanyId, actCompanyId)
										.in(CommunityItems::getGoodsId, goodsIds));
				for (CommunityItems ci : ciList) {
					minDeliveryByGoods.put(
							ci.getGoodsId(), ci.getMinDeliveryNum() == null ? 0 : ci.getMinDeliveryNum());
				}
			}
			for (Map<String, Object> item : validItems.values()) {
				Object gidObj = item.get("goods_id");
				long g = gidObj instanceof Number ? ((Number) gidObj).longValue() : 0L;
				item.put("min_delivery_num", minDeliveryByGoods.getOrDefault(g, 0));
				item.put("buy_num", sumBuyNumForMergedItemRow(item, itemNumByItemId));
			}
		}
		activity.put("items", List.copyOf(validItems.values()));

		List<CommunityActivityZiti> activityZitiRows =
				communityActivityZitiMapper.selectList(
						new LambdaQueryWrapper<CommunityActivityZiti>()
								.eq(CommunityActivityZiti::getActivityId, activityId));
		List<Long> zitiIds =
				activityZitiRows.stream()
						.map(CommunityActivityZiti::getZitiId)
						.filter(Objects::nonNull)
						.distinct()
						.toList();
		Map<Long, Map<String, Object>> chiefZitiById = new LinkedHashMap<>();
		if (!zitiIds.isEmpty()) {
			List<CommunityChiefZiti> chiefRows =
					communityChiefZitiMapper.selectList(
							new LambdaQueryWrapper<CommunityChiefZiti>().in(CommunityChiefZiti::getZitiId, zitiIds));
			for (CommunityChiefZiti z : chiefRows) {
				chiefZitiById.put(z.getZitiId(), chiefZitiEntityToMap(z));
			}
		}
		List<Map<String, Object>> mergedZitiList = new ArrayList<>();
		for (CommunityActivityZiti az : activityZitiRows) {
			Map<String, Object> chief = chiefZitiById.get(az.getZitiId());
			if (chief == null || chief.isEmpty()) {
				continue;
			}
			Map<String, Object> zm = activityZitiEntityToMap(az);
			zm.putAll(chief);
			mergedZitiList.add(zm);
		}
		activity.put("ziti", mergedZitiList);

		List<ActivityOrderAggRow> aggRows = communityActivityOrderStatsMapper.sumOrderStats(List.of(activityId));
		ActivityOrderAggRow agg = null;
		for (ActivityOrderAggRow r : aggRows) {
			if (r.getActivityId() != null && r.getActivityId().longValue() == activityId) {
				agg = r;
				break;
			}
		}
		activity.put("total_fee", agg != null && agg.getTotalFee() != null ? agg.getTotalFee() : BigDecimal.ZERO);
		int orderNumVal = agg != null && agg.getOrderNum() != null ? agg.getOrderNum().intValue() : 0;
		int userNumVal = agg != null && agg.getUserNum() != null ? agg.getUserNum().intValue() : 0;
		activity.put("order_num", orderNumVal);
		activity.put("user_num", userNumVal);

		List<ActivityWriteOffRow> woRows = communityActivityOrderStatsMapper.countPayedWritable(List.of(activityId));
		long payedOrderNum = 0L;
		for (ActivityWriteOffRow r : woRows) {
			if (r.getActivityId() != null && r.getActivityId().longValue() == activityId && r.getOrderNum() != null) {
				payedOrderNum = r.getOrderNum();
				break;
			}
		}
		activity.put("payed_order_num", payedOrderNum);
		activity.put("can_writeoff", payedOrderNum);

		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		String actStatus = act.getActivityStatus();
		int endSec = act.getEndTime() == null ? 0 : act.getEndTime();
		if ("public".equals(actStatus)) {
			activity.put("last_second", Math.max(0, endSec - nowSec));
		} else {
			activity.put("last_second", 0);
		}

		Integer createdAt = act.getCreatedAt();
		int ca = createdAt == null ? 0 : createdAt;
		activity.put("save_time", formatRelativeTimeLabel(ca, nowSec));

		List<Map<String, Object>> extraData = new ArrayList<>();
		Map<String, Object> ex0 = new LinkedHashMap<>();
		ex0.put("field_name", "楼号");
		ex0.put("field_type", "text");
		ex0.put("is_numeric", false);
		ex0.put("unit", "楼/栋");
		extraData.add(ex0);
		Map<String, Object> ex1 = new LinkedHashMap<>();
		ex1.put("field_name", "房号");
		ex1.put("field_type", "text");
		ex1.put("is_numeric", false);
		ex1.put("unit", "号/室");
		extraData.add(ex1);
		activity.put("extra_data", extraData);

		activity.put("orders", loadActivityOrderListForDetail(activityId));
		activity.put("is_activity_author", false);

		List<String> buttons = new ArrayList<>();
		if (actStatus != null
				&& ("private".equals(actStatus) || "public".equals(actStatus) || "protected".equals(actStatus))) {
			buttons.add("update");
		}
		if ("public".equals(actStatus)) {
			buttons.add("success");
			buttons.add("fail");
		}
		activity.put("buttons", buttons);

		String statusMsg = ACTIVITY_STATUS_MSG.getOrDefault(actStatus, "数据错误");
		int startSec = act.getStartTime() == null ? 0 : act.getStartTime();
		if ("public".equals(actStatus) && nowSec < startSec) {
			statusMsg = "未开始";
		}
		if ("public".equals(actStatus) && nowSec > endSec) {
			statusMsg = "已结束";
		}
		activity.put("activity_status_msg", statusMsg);

		String deliveryMsg =
				ACTIVITY_DELIVERY_STATUS_MSG.getOrDefault(act.getDeliveryStatus(), "数据错误");
		activity.put("activity_delivery_status_msg", deliveryMsg);

		boolean showBuy =
				"public".equals(actStatus) && nowSec > startSec && nowSec < endSec;
		activity.put("show_buy", showBuy);

		return activity;
	}

	private Map<String, Map<String, Object>> buildValidItemsByGoods(
			long companyId, long activityId, List<CommunityActivityItem> activityItemRows) {
		Map<String, Map<String, Object>> activityItemByActItemId = new LinkedHashMap<>();
		for (CommunityActivityItem row : activityItemRows) {
			if (row.getActivityId() == null || row.getActivityId().longValue() != activityId) {
				continue;
			}
			String k = row.getActivityId() + "_" + row.getItemId();
			activityItemByActItemId.put(k, activityItemEntityToLeftMap(row));
		}
		List<Long> itemIds =
				activityItemRows.stream()
						.filter(r -> r.getActivityId() != null && r.getActivityId().longValue() == activityId)
						.map(CommunityActivityItem::getItemId)
						.filter(Objects::nonNull)
						.distinct()
						.toList();
		Map<Long, Map<String, Object>> itemsByItemId = new LinkedHashMap<>();
		if (!itemIds.isEmpty()) {
			List<Items> itemEntities =
					itemsMapper.selectList(
							new LambdaQueryWrapper<Items>()
									.eq(Items::getCompanyId, companyId)
									.in(Items::getItemId, itemIds));
			for (Items it : itemEntities) {
				itemsByItemId.put(it.getItemId(), itemsEntityToListRowMap(it));
			}
		}
		Map<String, Map<String, Object>> validItems = new LinkedHashMap<>();
		for (Map<String, Object> value : activityItemByActItemId.values()) {
			Object rawItemId = value.get("item_id");
			if (!(rawItemId instanceof Number)) {
				continue;
			}
			long itemId = ((Number) rawItemId).longValue();
			Map<String, Object> itemRow = itemsByItemId.get(itemId);
			if (itemRow == null) {
				continue;
			}
			long actId = ((Number) value.get("activity_id")).longValue();
			Object gidObj = itemRow.get("goods_id");
			long goodsId = gidObj instanceof Number ? ((Number) gidObj).longValue() : 0L;
			String keyGoods = actId + "_" + goodsId;
			boolean isDefault = truthyIsDefault(itemRow.get("is_default"));
			if (isDefault) {
				if (!validItems.containsKey(keyGoods)) {
					validItems.put(keyGoods, mergeItemRows(value, itemRow));
				}
			} else {
				if (validItems.containsKey(keyGoods)) {
					Map<String, Object> cur = validItems.get(keyGoods);
					if (!cur.containsKey("spec_items")) {
						List<Map<String, Object>> specList = new ArrayList<>();
						specList.add(new LinkedHashMap<>(cur));
						cur.put("spec_items", specList);
					}
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> specList = (List<Map<String, Object>>) cur.get("spec_items");
					specList.add(mergeItemRows(value, itemRow));
				} else {
					Object defItemObj = itemRow.get("default_item_id");
					long defaultItemId = defItemObj instanceof Number ? ((Number) defItemObj).longValue() : 0L;
					String actDefaultKey = actId + "_" + defaultItemId;
					Map<String, Object> leftDefault = activityItemByActItemId.get(actDefaultKey);
					Map<String, Object> rightDefault = defaultItemId > 0 ? itemsByItemId.get(defaultItemId) : null;
					if (leftDefault != null && rightDefault != null) {
						Map<String, Object> merged = mergeItemRows(leftDefault, rightDefault);
						validItems.put(keyGoods, merged);
						List<Map<String, Object>> specList = new ArrayList<>();
						specList.add(new LinkedHashMap<>(merged));
						specList.add(mergeItemRows(value, itemRow));
						merged.put("spec_items", specList);
					} else {
						validItems.put(keyGoods, mergeItemRows(value, itemRow));
					}
				}
			}
		}
		return validItems;
	}

	private static long sumBuyNumForMergedItemRow(Map<String, Object> merged, Map<Long, Long> itemNumByItemId) {
		long sum = 0L;
		Object mainId = merged.get("item_id");
		if (mainId instanceof Number) {
			sum += itemNumByItemId.getOrDefault(((Number) mainId).longValue(), 0L);
		}
		Object spec = merged.get("spec_items");
		if (spec instanceof List<?> list) {
			for (Object el : list) {
				if (el instanceof Map<?, ?> m) {
					Object iid = m.get("item_id");
					if (iid instanceof Number) {
						sum += itemNumByItemId.getOrDefault(((Number) iid).longValue(), 0L);
					}
				}
			}
		}
		return sum;
	}

	private List<Map<String, Object>> loadActivityOrderListForDetail(long activityId) {
		List<CommunityOrderRelActivity> rels =
				communityOrderRelActivityMapper.selectList(
						new LambdaQueryWrapper<CommunityOrderRelActivity>()
								.eq(CommunityOrderRelActivity::getActivityId, activityId)
								.isNotNull(CommunityOrderRelActivity::getActivityTradeNo)
								.orderByDesc(CommunityOrderRelActivity::getCreated)
								.last("LIMIT 20"));
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		List<Map<String, Object>> out = new ArrayList<>();
		for (CommunityOrderRelActivity rel : rels) {
			Long orderId = rel.getOrderId();
			if (orderId == null) {
				continue;
			}
			NormalOrders ord = normalOrdersMapper.selectById(orderId);
			if (ord == null) {
				continue;
			}
			Long userId = ord.getUserId();
			MembersInfo mem = userId != null ? membersInfoMapper.selectById(userId) : null;
			String username = "";
			if (mem != null) {
				if (StringUtils.hasText(mem.getUsername())) {
					username = mem.getUsername();
				} else if (StringUtils.hasText(mem.getName())) {
					username = mem.getName();
				}
			}
			String avatar = mem != null && mem.getAvatar() != null ? mem.getAvatar() : "";
			NormalOrdersItems firstItem =
					normalOrdersItemsMapper.selectOne(
							new LambdaQueryWrapper<NormalOrdersItems>()
									.eq(NormalOrdersItems::getOrderId, orderId)
									.last("LIMIT 1"));
			String itemName = firstItem != null && firstItem.getItemName() != null ? firstItem.getItemName() : "";
			int num = firstItem != null && firstItem.getNum() != null ? firstItem.getNum() : 0;
			Integer created = rel.getCreated();
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("activity_trade_no", rel.getActivityTradeNo());
			row.put("order_id", orderId);
			row.put("username", username);
			row.put("avatar", avatar);
			row.put("created", created);
			row.put("item_name", itemName);
			row.put("num", num);
			row.put("save_time", formatRelativeTimeLabel(created == null ? 0 : created, nowSec));
			out.add(row);
		}
		return out;
	}

	private static String formatRelativeTimeLabel(int createdSec, int nowSec) {
		if (createdSec <= 0) {
			return "0小时前";
		}
		int hour = (int) Math.floor((nowSec - createdSec) / 3600.0);
		if (hour <= 24) {
			return hour + "小时前";
		}
		int day = (int) Math.floor(hour / 24.0);
		return day + "天前";
	}

	private static Map<String, Object> chiefEntityToSnakeMap(CommunityChief c) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("chief_id", c.getChiefId());
		m.put("company_id", c.getCompanyId());
		m.put("chief_name", c.getChiefName());
		m.put("chief_avatar", c.getChiefAvatar());
		m.put("chief_mobile", c.getChiefMobile());
		m.put("chief_desc", c.getChiefDesc());
		m.put("chief_intro", c.getChiefIntro());
		m.put("province", c.getProvince());
		m.put("city", c.getCity());
		m.put("area", c.getArea());
		m.put("regions_id", c.getRegionsId());
		m.put("regions", c.getRegions());
		m.put("address", c.getAddress());
		m.put("lng", c.getLng());
		m.put("lat", c.getLat());
		m.put("user_id", c.getUserId());
		m.put("created_at", c.getCreatedAt());
		m.put("updated_at", c.getUpdatedAt());
		m.put("alipay_name", c.getAlipayName());
		m.put("alipay_account", c.getAlipayAccount());
		m.put("bank_name", c.getBankName());
		m.put("bankcard_no", c.getBankcardNo());
		return m;
	}

	private List<Map<String, Object>> buildActivityListPayload(long companyId, List<CommunityActivity> records) {
		List<Long> activityIds = records.stream().map(CommunityActivity::getActivityId).toList();

		List<CommunityActivityItem> activityItemRows =
				communityActivityItemMapper.selectList(
						new LambdaQueryWrapper<CommunityActivityItem>()
								.in(CommunityActivityItem::getActivityId, activityIds));

		Map<String, Map<String, Object>> activityItemByActItemId = new LinkedHashMap<>();
		for (CommunityActivityItem row : activityItemRows) {
			String k = row.getActivityId() + "_" + row.getItemId();
			activityItemByActItemId.put(k, activityItemEntityToLeftMap(row));
		}

		List<Long> itemIds =
				activityItemRows.stream()
						.map(CommunityActivityItem::getItemId)
						.filter(Objects::nonNull)
						.distinct()
						.toList();

		Map<Long, Map<String, Object>> itemsByItemId = new LinkedHashMap<>();
		if (!itemIds.isEmpty()) {
			List<Items> itemEntities =
					itemsMapper.selectList(
							new LambdaQueryWrapper<Items>()
									.eq(Items::getCompanyId, companyId)
									.in(Items::getItemId, itemIds));
			for (Items it : itemEntities) {
				itemsByItemId.put(it.getItemId(), itemsEntityToListRowMap(it));
			}
		}

		Map<String, Map<String, Object>> validItems = new LinkedHashMap<>();
		for (Map<String, Object> value : activityItemByActItemId.values()) {
			Object rawItemId = value.get("item_id");
			if (!(rawItemId instanceof Number)) {
				continue;
			}
			long itemId = ((Number) rawItemId).longValue();
			Map<String, Object> itemRow = itemsByItemId.get(itemId);
			if (itemRow == null) {
				continue;
			}
			long activityId = ((Number) value.get("activity_id")).longValue();
			Object gidObj = itemRow.get("goods_id");
			long goodsId = gidObj instanceof Number ? ((Number) gidObj).longValue() : 0L;
			String keyGoods = activityId + "_" + goodsId;
			boolean isDefault = truthyIsDefault(itemRow.get("is_default"));

			if (isDefault) {
				if (!validItems.containsKey(keyGoods)) {
					validItems.put(keyGoods, mergeItemRows(value, itemRow));
				}
			} else {
				if (validItems.containsKey(keyGoods)) {
					Map<String, Object> cur = validItems.get(keyGoods);
					if (!cur.containsKey("spec_items")) {
						List<Map<String, Object>> specList = new ArrayList<>();
						specList.add(new LinkedHashMap<>(cur));
						cur.put("spec_items", specList);
					}
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> specList = (List<Map<String, Object>>) cur.get("spec_items");
					specList.add(mergeItemRows(value, itemRow));
				} else {
					Object defItemObj = itemRow.get("default_item_id");
					long defaultItemId = defItemObj instanceof Number ? ((Number) defItemObj).longValue() : 0L;
					String actDefaultKey = activityId + "_" + defaultItemId;
					Map<String, Object> leftDefault = activityItemByActItemId.get(actDefaultKey);
					Map<String, Object> rightDefault =
							defaultItemId > 0 ? itemsByItemId.get(defaultItemId) : null;
					if (leftDefault != null && rightDefault != null) {
						Map<String, Object> merged = mergeItemRows(leftDefault, rightDefault);
						validItems.put(keyGoods, merged);
						List<Map<String, Object>> specList = new ArrayList<>();
						specList.add(new LinkedHashMap<>(merged));
						specList.add(mergeItemRows(value, itemRow));
						merged.put("spec_items", specList);
					} else {
						validItems.put(keyGoods, mergeItemRows(value, itemRow));
					}
				}
			}
		}

		List<CommunityActivityZiti> activityZitiRows =
				communityActivityZitiMapper.selectList(
						new LambdaQueryWrapper<CommunityActivityZiti>()
								.in(CommunityActivityZiti::getActivityId, activityIds));

		List<Long> zitiIds =
				activityZitiRows.stream()
						.map(CommunityActivityZiti::getZitiId)
						.filter(Objects::nonNull)
						.distinct()
						.toList();

		Map<Long, Map<String, Object>> chiefZitiById = new LinkedHashMap<>();
		if (!zitiIds.isEmpty()) {
			List<CommunityChiefZiti> chiefRows =
					communityChiefZitiMapper.selectList(
							new LambdaQueryWrapper<CommunityChiefZiti>()
									.in(CommunityChiefZiti::getZitiId, zitiIds));
			for (CommunityChiefZiti z : chiefRows) {
				chiefZitiById.put(z.getZitiId(), chiefZitiEntityToMap(z));
			}
		}

		List<Map<String, Object>> mergedZitiList = new ArrayList<>();
		for (CommunityActivityZiti az : activityZitiRows) {
			Map<String, Object> chief = chiefZitiById.get(az.getZitiId());
			if (chief == null || chief.isEmpty()) {
				continue;
			}
			Map<String, Object> zm = activityZitiEntityToMap(az);
			zm.putAll(chief);
			mergedZitiList.add(zm);
		}

		List<ActivityOrderAggRow> aggRows = communityActivityOrderStatsMapper.sumOrderStats(activityIds);
		Map<Long, ActivityOrderAggRow> aggByAct = new LinkedHashMap<>();
		for (ActivityOrderAggRow r : aggRows) {
			if (r.getActivityId() != null) {
				aggByAct.put(r.getActivityId(), r);
			}
		}

		List<ActivityWriteOffRow> woRows = communityActivityOrderStatsMapper.countPayedWritable(activityIds);
		Map<Long, Long> payedNumByAct = new LinkedHashMap<>();
		for (ActivityWriteOffRow r : woRows) {
			if (r.getActivityId() != null && r.getOrderNum() != null) {
				payedNumByAct.put(r.getActivityId(), r.getOrderNum());
			}
		}

		int timestamp = (int) (System.currentTimeMillis() / 1000L);
		List<Map<String, Object>> listOut = new ArrayList<>();
		for (CommunityActivity act : records) {
			listOut.add(buildActivityListRow(act, validItems, mergedZitiList, aggByAct, payedNumByAct, timestamp));
		}
		return listOut;
	}

	/**
	 * 管理端导出：与 {@link #getActivityList} 相同的活动筛选条件，无分页；可选按活动 ID 精确限定。
	 */
	public List<CommunityActivity> listActivitiesForAdminExport(
			long companyId, CommunityActivityAdminListQuery query, Long activityIdOrNull) {
		LambdaQueryWrapper<CommunityActivity> w = new LambdaQueryWrapper<>();
		w.eq(CommunityActivity::getCompanyId, companyId);
		applyDistributorIdFilter(w, query.isDistributorOperator(), query.getDistributorIdFilter());

		if (query.getCreatedAtGte() != null) {
			w.ge(CommunityActivity::getCreatedAt, query.getCreatedAtGte().intValue());
		}
		if (query.isFilterCreatedAtLteWithNull()) {
			w.apply("created_at <= NULL");
		} else if (query.getCreatedAtLte() != null) {
			w.le(CommunityActivity::getCreatedAt, query.getCreatedAtLte().intValue());
		}

		String activityStatusParam = query.getActivityStatus();
		if (StringUtils.hasText(activityStatusParam)) {
			int now = (int) (System.currentTimeMillis() / 1000L);
			switch (activityStatusParam.trim()) {
				case "waiting" ->
						w.ge(CommunityActivity::getStartTime, now).ge(CommunityActivity::getEndTime, now);
				case "ongoing" ->
						w.le(CommunityActivity::getStartTime, now).ge(CommunityActivity::getEndTime, now);
				case "end" -> w.le(CommunityActivity::getStartTime, now).le(CommunityActivity::getEndTime, now);
				default -> {
					/* no extra filter */
				}
			}
		}

		if (query.isSuccess()) {
			w.eq(CommunityActivity::getActivityStatus, "success");
		}

		applyActivityNameContains(w, query.getActivityNameContains());

		if (activityIdOrNull != null) {
			w.eq(CommunityActivity::getActivityId, activityIdOrNull);
		}

		w.orderByDesc(CommunityActivity::getActivityId);
		return communityActivityMapper.selectList(w);
	}

	public String exportActivityStatusLabel(String activityStatusCode) {
		if (!StringUtils.hasText(activityStatusCode)) {
			return "";
		}
		return ACTIVITY_STATUS_MSG.getOrDefault(activityStatusCode.trim(), "");
	}

	public String exportActivityDeliveryLabel(String deliveryStatusCode) {
		if (!StringUtils.hasText(deliveryStatusCode)) {
			return "";
		}
		return ACTIVITY_DELIVERY_STATUS_MSG.getOrDefault(deliveryStatusCode.trim(), "");
	}

	private Map<String, Object> buildActivityListRow(
			CommunityActivity act,
			Map<String, Map<String, Object>> validItems,
			List<Map<String, Object>> mergedZitiList,
			Map<Long, ActivityOrderAggRow> aggByAct,
			Map<Long, Long> payedNumByAct,
			int timestamp) {

		long activityId = act.getActivityId();
		Integer startRaw = act.getStartTime();
		Integer endRaw = act.getEndTime();
		Integer createdRaw = act.getCreatedAt();

		int st = startRaw == null ? 0 : startRaw;
		int et = endRaw == null ? 0 : endRaw;
		int ca = createdRaw == null ? 0 : createdRaw;

		Map<String, Object> row = toColumnNamesMap(act);

		if (startRaw != null && startRaw != 0) {
			row.put("start_time", formatEpochSeconds(startRaw));
		}
		if (endRaw != null && endRaw != 0) {
			row.put("end_time", formatEpochSeconds(endRaw));
		}

		List<Map<String, Object>> itemsOut = new ArrayList<>();
		int min = 0;
		int max = 0;
		for (Map<String, Object> item : validItems.values()) {
			Object aid = item.get("activity_id");
			if (!(aid instanceof Number) || ((Number) aid).longValue() != activityId) {
				continue;
			}
			itemsOut.add(item);
			Object p = item.get("price");
			int price = p instanceof Number ? ((Number) p).intValue() : 0;
			if (min == 0) {
				min = price;
			} else {
				min = Math.min(min, price);
			}
			max = Math.max(max, price);
		}
		row.put("items", itemsOut);

		String priceRange = "¥ " + BigDecimal.valueOf(min).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		if (max > min) {
			priceRange +=
					" - ¥ "
							+ BigDecimal.valueOf(max).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		}
		row.put("min_price", min);
		row.put("max_price", max);
		row.put("price_range", priceRange);

		List<Map<String, Object>> zitiOut =
				mergedZitiList.stream()
						.filter(z -> z.get("activity_id") instanceof Number)
						.filter(z -> ((Number) z.get("activity_id")).longValue() == activityId)
						.collect(Collectors.toList());
		row.put("ziti", zitiOut);

		String actStatus = act.getActivityStatus();
		String statusMsg = ACTIVITY_STATUS_MSG.getOrDefault(actStatus, "数据错误");
		String deliveryMsg =
				ACTIVITY_DELIVERY_STATUS_MSG.getOrDefault(act.getDeliveryStatus(), "数据错误");
		if ("public".equals(actStatus) && timestamp < st) {
			statusMsg = "未开始";
		}
		if ("public".equals(actStatus) && et < timestamp) {
			statusMsg = "已结束";
		}
		row.put("activity_status_msg", statusMsg);
		row.put("activity_delivery_status_msg", deliveryMsg);

		ActivityOrderAggRow agg = aggByAct.get(activityId);
		row.put("total_fee", agg != null && agg.getTotalFee() != null ? agg.getTotalFee() : BigDecimal.ZERO);
		row.put("order_num", agg != null && agg.getOrderNum() != null ? agg.getOrderNum() : 0L);
		row.put("user_num", agg != null && agg.getUserNum() != null ? agg.getUserNum() : 0L);

		row.put("last_second", 0);
		if ("public".equals(actStatus)) {
			row.put("last_second", timestamp < et ? et - timestamp : 0);
		}

		row.put("created_at_time", ca > 0 ? formatEpochSeconds(ca) : null);
		int hour = (int) Math.floor((timestamp - ca) / 3600.0);
		String saveTime = hour + "小时前";
		if (hour > 24) {
			int day = (int) Math.floor(hour / 24.0);
			saveTime = day + "天前";
		}
		row.put("save_time", saveTime);

		int nowTime = timestamp;
		row.put("activity_process_msg", statusMsg);
		if (nowTime >= et && "public".equals(actStatus)) {
			row.put("activity_process", "end");
			row.put("activity_process_msg", "已结束");
		} else if (nowTime >= st && nowTime < et && "public".equals(actStatus)) {
			row.put("activity_process", "ongoing");
			row.put("activity_process_msg", "进行中");
		} else if (nowTime < st && "public".equals(actStatus)) {
			row.put("activity_process", "waiting");
			row.put("activity_process_msg", "未开始");
		}

		long payedOrderNum = payedNumByAct.getOrDefault(activityId, 0L);
		row.put("payed_order_num", payedOrderNum);
		boolean canWriteoff = false;
		if ("SUCCESS".equals(act.getDeliveryStatus())) {
			canWriteoff = payedOrderNum > 0;
		}
		row.put("can_writeoff", canWriteoff);

		return row;
	}

	private static String formatEpochSeconds(int epochSec) {
		return Instant.ofEpochSecond(epochSec).atZone(ZoneId.systemDefault()).toLocalDateTime().format(LIST_TIME_FMT);
	}

	private void applyActivityNameContains(LambdaQueryWrapper<CommunityActivity> wrapper, String activityNameContains) {
		if (activityNameContains == null || activityNameContains.isBlank()) {
			return;
		}
		String pattern = "%" + activityNameContains + "%";
		wrapper.like(CommunityActivity::getActivityName, pattern);
	}

	private static Map<String, Object> mergeItemRows(Map<String, Object> left, Map<String, Object> right) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.putAll(left);
		m.putAll(right);
		return m;
	}

	private static boolean truthyIsDefault(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = v.toString();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static Map<String, Object> activityItemEntityToLeftMap(CommunityActivityItem e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("activity_id", e.getActivityId());
		m.put("goods_id", e.getGoodsId());
		m.put("item_id", e.getItemId());
		m.put("item_name", e.getItemName());
		m.put("item_spec_desc", e.getItemSpecDesc());
		m.put("item_brief", e.getItemBrief());
		m.put("item_pics", e.getItemPics());
		m.put("price", e.getPrice());
		m.put("cost_price", e.getCostPrice());
		m.put("market_price", e.getMarketPrice());
		m.put("store", e.getStore());
		m.put("created_at", e.getCreatedAt());
		m.put("updated_at", e.getUpdatedAt());
		return m;
	}

	private Map<String, Object> itemsEntityToListRowMap(Items it) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		for (String col : ITEM_REPO_COLS) {
			putItemColumn(it, col, m);
		}
		m.put("itemId", m.get("item_id"));
		m.put("consumeType", nullToEmpty(m.get("consume_type")));
		m.put("itemName", nullToEmpty(m.get("item_name")));
		m.put("itemBn", nullToEmpty(m.get("item_bn")));
		m.put("companyId", m.get("company_id"));
		m.put("item_main_cat_id", m.get("item_category"));
		Object nos = m.get("nospec");
		boolean nospecBool =
				nos != null
						&& ("true".equals(nos)
								|| Boolean.TRUE.equals(nos)
								|| "1".equals(String.valueOf(nos))
								|| (nos instanceof Number n && n.intValue() == 1));
		m.put("nospec", nospecBool);
		if (m.get("is_medicine") == null) {
			m.put("is_medicine", 0);
		}
		m.put("item_cat_id", null);
		String itype = it.getItemType();
		m.put("item_type", StringUtils.hasText(itype) ? itype : "services");
		m.put("type_labels", List.of());
		return m;
	}

	private void putItemColumn(Items it, String col, Map<String, Object> m) {
		Object v =
				switch (col) {
					case "item_id" -> it.getItemId();
					case "item_type" -> it.getItemType();
					case "consume_type" -> it.getConsumeType();
					case "is_show_specimg" -> it.getIsShowSpecimg();
					case "store" -> it.getStore();
					case "barcode" -> it.getBarcode();
					case "sales" -> it.getSales();
					case "approve_status" -> it.getApproveStatus();
					case "rebate" -> it.getRebate();
					case "rebate_conf" -> jsonOrRaw(it.getRebateConf());
					case "cost_price" -> it.getCostPrice();
					case "is_point" -> it.getIsPoint();
					case "point" -> it.getPoint();
					case "item_source" -> it.getItemSource();
					case "goods_id" -> it.getGoodsId();
					case "brand_id" -> it.getBrandId();
					case "is_market" -> it.getIsMarket();
					case "item_name" -> it.getItemName();
					case "item_unit" -> it.getItemUnit();
					case "item_bn" -> it.getItemBn();
					case "brief" -> it.getBrief();
					case "price" -> it.getPrice();
					case "market_price" -> it.getMarketPrice();
					case "special_type" -> it.getSpecialType();
					case "goods_function" -> it.getGoodsFunction();
					case "goods_series" -> it.getGoodsSeries();
					case "volume" -> it.getVolume();
					case "supplier_id" -> it.getSupplierId();
					case "supplier_item_id" -> it.getSupplierItemId();
					case "goods_color" -> it.getGoodsColor();
					case "goods_brand" -> it.getGoodsBrand();
					case "item_address_province" -> it.getItemAddressProvince();
					case "item_address_city" -> it.getItemAddressCity();
					case "regions_id" -> it.getRegionsId();
					case "regions" -> it.getRegions();
					case "brand_logo" -> it.getBrandLogo();
					case "sort" -> it.getSort();
					case "templates_id" -> it.getTemplatesId();
					case "is_default" -> it.getIsDefault();
					case "nospec" -> it.getNospec();
					case "default_item_id" -> it.getDefaultItemId();
					case "pics" -> jsonOrRaw(it.getPics());
					case "pics_create_qrcode" -> jsonOrRaw(it.getPicsCreateQrcode());
					case "distributor_id" -> it.getDistributorId();
					case "company_id" -> it.getCompanyId();
					case "enable_agreement" -> it.getEnableAgreement();
					case "date_type" -> it.getDateType();
					case "item_category" -> it.getItemCategory();
					case "rebate_type" -> it.getRebateType();
					case "weight" -> it.getWeight();
					case "begin_date" -> it.getBeginDate();
					case "end_date" -> it.getEndDate();
					case "fixed_term" -> it.getFixedTerm();
					case "tax_rate" -> it.getTaxRate();
					case "created" -> it.getCreated();
					case "updated" -> it.getUpdated();
					case "video_type" -> it.getVideoType();
					case "videos" -> it.getVideos();
					case "video_pic_url" -> it.getVideoPicUrl();
					case "audit_status" -> it.getAuditStatus();
					case "audit_reason" -> it.getAuditReason();
					case "is_gift" -> it.getIsGift();
					case "is_package" -> it.getIsPackage();
					case "profit_type" -> it.getProfitType();
					case "profit_fee" -> it.getProfitFee();
					case "is_profit" -> it.getIsProfit();
					case "crossborder_tax_rate" -> it.getCrossborderTaxRate();
					case "origincountry_id" -> it.getOrigincountryId();
					case "taxstrategy_id" -> it.getTaxstrategyId();
					case "taxation_num" -> it.getTaxationNum();
					case "type" -> it.getType();
					case "tdk_content" -> it.getTdkContent();
					case "is_epidemic" -> it.getIsEpidemic();
					case "goods_bn" -> it.getGoodsBn();
					case "audit_date" -> it.getAuditDate();
					case "is_medicine" -> it.getIsMedicine();
					case "is_prescription" -> it.getIsPrescription();
					case "start_num" -> it.getStartNum();
					case "delivery_time" -> it.getDeliveryTime();
					case "is_taobao" -> it.getIsTaobao();
					default -> null;
				};
		m.put(col, v);
	}

	private Object jsonOrRaw(String s) {
		if (s == null || s.isBlank()) {
			return s;
		}
		try {
			return objectMapper.readValue(s, Object.class);
		} catch (Exception e) {
			return s;
		}
	}

	private static Object nullToEmpty(Object o) {
		return o == null ? "" : o;
	}

	private static Map<String, Object> activityZitiEntityToMap(CommunityActivityZiti e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("activity_id", e.getActivityId());
		m.put("ziti_id", e.getZitiId());
		m.put("condition_num", e.getConditionNum());
		m.put("remark", e.getRemark());
		return m;
	}

	private static Map<String, Object> chiefZitiEntityToMap(CommunityChiefZiti z) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("ziti_id", z.getZitiId());
		m.put("chief_id", z.getChiefId());
		m.put("ziti_name", z.getZitiName());
		m.put("province", z.getProvince());
		m.put("city", z.getCity());
		m.put("area", z.getArea());
		m.put("regions_id", z.getRegionsId());
		m.put("regions", z.getRegions());
		m.put("address", z.getAddress());
		m.put("lng", z.getLng());
		m.put("lat", z.getLat());
		m.put("ziti_contact_user", z.getZitiContactUser());
		m.put("ziti_contact_mobile", z.getZitiContactMobile());
		m.put("ziti_pics", z.getZitiPics());
		m.put("is_default", z.getIsDefault());
		m.put("ziti_status", z.getZitiStatus());
		m.put("created_at", z.getCreatedAt());
		m.put("updated_at", z.getUpdatedAt());
		return m;
	}

	private static void applyDistributorIdFilter(
			AbstractLambdaWrapper<CommunityActivity, ?> w, boolean distributorBranch, Integer distributorIdWhenBranch) {
		if (!distributorBranch) {
			w.eq(CommunityActivity::getDistributorId, 0);
		} else if (distributorIdWhenBranch != null) {
			w.eq(CommunityActivity::getDistributorId, distributorIdWhenBranch);
		} else {
			w.isNull(CommunityActivity::getDistributorId);
		}
	}

	/**
	 * 团长一键核销：加载活动并校验归属与收货状态。
	 */
	public CommunityActivity loadForChiefBatchWriteoffOrThrow(long companyId, long chiefId, long activityId) {
		CommunityActivity act = communityActivityMapper.selectById(activityId);
		if (act == null || !Objects.equals(act.getCompanyId(), companyId)) {
			throw new ResourceException("活动不存在");
		}
		Long actChief = act.getChiefId();
		if (actChief == null || actChief.longValue() != chiefId) {
			throw new ResourceException("活动不存在");
		}
		if (!"SUCCESS".equals(act.getDeliveryStatus())) {
			throw new ResourceException("未确认收货不能核销");
		}
		return act;
	}

	/**
	 * 扫描到期未结束社区团并按条件置为失败/成功；单活动内多步更新彼此独立事务。
	 */
	public int scheduleFinishActivity() {
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		List<CommunityActivity> due =
				communityActivityMapper.selectList(
						new LambdaQueryWrapper<CommunityActivity>()
								.eq(CommunityActivity::getActivityStatus, "public")
								.le(CommunityActivity::getEndTime, nowSec));
		if (due == null || due.isEmpty()) {
			return 0;
		}
		int processed = 0;
		for (CommunityActivity row : due) {
			if (row.getActivityId() == null) {
				continue;
			}
			processed++;
			long activityId = row.getActivityId();
			long companyId = row.getCompanyId() == null ? 0L : row.getCompanyId();
			Object distributorIdRaw = row.getDistributorId();
			List<ScheduleFinishGoodsRow> itemList =
					communityActivityOrderStatsMapper.listScheduleFinishGoodsForActivity(activityId);
			if (itemList == null) {
				itemList = List.of();
			}
			if (itemList.isEmpty()) {
				updateActivityStatusForSchedule(activityId, "fail");
			}
			boolean fail = false;
			Map<String, Object> setting = communitySettingReadPort.getSetting(companyId, true, distributorIdRaw);
			String conditionType = Objects.toString(setting.get("condition_type"), "num");
			if ("money".equalsIgnoreCase(conditionType)) {
				BigDecimal condMoney = parseConditionMoney(setting.get("condition_money"));
				if (condMoney.compareTo(BigDecimal.ZERO) > 0) {
					long totalFen = 0L;
					for (ScheduleFinishGoodsRow r : itemList) {
						totalFen += (long) r.getTotalFeeCents();
					}
					long thresholdFen =
							condMoney.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
					if (totalFen < thresholdFen) {
						updateActivityStatusForSchedule(activityId, "fail");
						fail = true;
					}
				}
			} else {
				for (ScheduleFinishGoodsRow item : itemList) {
					if (!fail && item.getMinDeliveryNum() > 0 && item.getBuyNum() < item.getMinDeliveryNum()) {
						updateActivityStatusForSchedule(activityId, "fail");
						fail = true;
					}
				}
			}
			if (!fail) {
				updateActivityStatusForSchedule(activityId, "success");
			}
		}
		return processed;
	}

	private void updateActivityStatusForSchedule(long activityId, String newStatus) {
		if (newStatus == null || !ACTIVITY_STATUS_MSG.containsKey(newStatus)) {
			throw new ResourceException("活动状态错误");
		}
		if (communityActivityMapper.selectById(activityId) == null) {
			throw new ResourceException("无效的活动");
		}
		transactionTemplate.executeWithoutResult(
				st -> {
					LambdaUpdateWrapper<CommunityActivity> uw = new LambdaUpdateWrapper<>();
					uw.eq(CommunityActivity::getActivityId, activityId)
							.set(CommunityActivity::getActivityStatus, newStatus);
					communityActivityMapper.update(null, uw);
					if ("fail".equals(newStatus)) {
						cancelActivityOrdersForSchedule(activityId, ActivityOrderCancelScope.FAIL);
					} else if ("success".equals(newStatus)) {
						cancelActivityOrdersForSchedule(activityId, ActivityOrderCancelScope.SUCCESS);
					}
				});
	}

	private void cancelActivityOrdersForSchedule(long activityId, ActivityOrderCancelScope scope) {
		LambdaQueryWrapper<NormalOrders> w = new LambdaQueryWrapper<>();
		w.eq(NormalOrders::getOrderClass, "community");
		w.eq(NormalOrders::getOrderType, "normal");
		w.eq(NormalOrders::getActId, activityId);
		w.in(NormalOrders::getCancelStatus, "NO_APPLY_CANCEL", "FAILS");
		if (scope == ActivityOrderCancelScope.FAIL) {
			w.in(NormalOrders::getOrderStatus, "NOTPAY", "PAYED");
		} else {
			w.eq(NormalOrders::getOrderStatus, "NOTPAY");
		}
		w.orderByAsc(NormalOrders::getCreateTime);
		long count = normalOrdersMapper.selectCount(w);
		if (count == 0) {
			return;
		}
		int pageSize = 50;
		long pages = (count + pageSize - 1) / pageSize;
		String reason =
				scope == ActivityOrderCancelScope.FAIL
						? CANCEL_ON_FAIL_MSG
						: CANCEL_NOTPAY_ON_SUCCESS_MSG;
		for (long p = 1; p <= pages; p++) {
			Page<NormalOrders> page = new Page<>(p, pageSize, false);
			List<NormalOrders> list = normalOrdersMapper.selectPage(page, w).getRecords();
			if (list == null || list.isEmpty()) {
				continue;
			}
			List<CommunityActivityCancelOrderRow> rows = new ArrayList<>();
			for (NormalOrders o : list) {
				CommunityActivityCancelOrderRow r = new CommunityActivityCancelOrderRow();
				r.setCompanyId(o.getCompanyId() == null ? 0L : o.getCompanyId().longValue());
				r.setOrderId(o.getOrderId() == null ? 0L : o.getOrderId().longValue());
				r.setCancelReason(reason);
				r.setUserId(o.getUserId() == null ? 0L : o.getUserId().longValue());
				r.setMobile(o.getMobile() == null ? "" : o.getMobile());
				r.setCancelFrom("system");
				rows.add(r);
			}
			registerPublishCancelBatchAfterCommit(rows);
		}
	}

	private static BigDecimal parseConditionMoney(Object raw) {
		if (raw == null) {
			return BigDecimal.ZERO;
		}
		if (raw instanceof BigDecimal b) {
			return b;
		}
		if (raw instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		try {
			return new BigDecimal(String.valueOf(raw).trim());
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	private enum ActivityOrderCancelScope {
		FAIL,
		SUCCESS
	}

	private static Map<String, Object> toColumnNamesMap(CommunityActivity e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("activity_id", e.getActivityId());
		m.put("company_id", e.getCompanyId());
		m.put("distributor_id", e.getDistributorId());
		m.put("chief_id", e.getChiefId());
		m.put("activity_name", e.getActivityName());
		m.put("activity_pics", e.getActivityPics());
		m.put("activity_desc", e.getActivityDesc());
		m.put("activity_intro", e.getActivityIntro());
		m.put("start_time", e.getStartTime());
		m.put("end_time", e.getEndTime());
		m.put("activity_status", e.getActivityStatus());
		m.put("delivery_status", e.getDeliveryStatus());
		m.put("delivery_time", e.getDeliveryTime());
		m.put("aftersales_setting", e.getAftersalesSetting());
		m.put("created_at", e.getCreatedAt());
		m.put("updated_at", e.getUpdatedAt());
		m.put("share_image_url", e.getShareImageUrl());
		return m;
	}
}
