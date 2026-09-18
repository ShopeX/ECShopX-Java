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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.dispatch.SalespersonItemsShelvesJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.PackageItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackagePromotions;
import cn.shopex.ecshopx.promotions.mapper.PackageItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackagePromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class PackagePromotionCancelService {

	private static final Logger log = LoggerFactory.getLogger(PackagePromotionCancelService.class);

	private final PackagePromotionsMapper packagePromotionsMapper;
	private final PackageItemPromotionsMapper packageItemPromotionsMapper;
	private final SalespersonItemsShelvesJobDispatchPublisher salespersonItemsShelvesJobDispatchPublisher;
	private final MessageSource messageSource;

	public PackagePromotionCancelService(
			PackagePromotionsMapper packagePromotionsMapper,
			PackageItemPromotionsMapper packageItemPromotionsMapper,
			SalespersonItemsShelvesJobDispatchPublisher salespersonItemsShelvesJobDispatchPublisher,
			MessageSource messageSource) {
		this.packagePromotionsMapper = packagePromotionsMapper;
		this.packageItemPromotionsMapper = packageItemPromotionsMapper;
		this.salespersonItemsShelvesJobDispatchPublisher = salespersonItemsShelvesJobDispatchPublisher;
		this.messageSource = messageSource;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> cancel(long packageId, long companyId) {
		Locale locale = Locale.SIMPLIFIED_CHINESE;
		try {
			locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
			int nowEpoch = (int) Instant.now().getEpochSecond();
			int endEpoch = nowEpoch - 1;

			PackagePromotions row =
					packagePromotionsMapper.selectOne(
							new LambdaQueryWrapper<PackagePromotions>()
									.eq(PackagePromotions::getPackageId, packageId)
									.eq(PackagePromotions::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (row == null) {
				throw new IllegalStateException("package promotion cancel precondition not met");
			}

			LambdaUpdateWrapper<PackagePromotions> mainUw =
					new LambdaUpdateWrapper<PackagePromotions>()
							.eq(PackagePromotions::getPackageId, packageId)
							.eq(PackagePromotions::getCompanyId, companyId)
							.set(PackagePromotions::getEndTime, endEpoch)
							.set(PackagePromotions::getUpdated, nowEpoch);
			int mainRows = packagePromotionsMapper.update(null, mainUw);
			if (mainRows == 0) {
				throw new IllegalStateException("package promotion cancel main update affected zero rows");
			}

			List<PackageItemPromotions> items =
					packageItemPromotionsMapper.selectList(
							new LambdaQueryWrapper<PackageItemPromotions>()
									.eq(PackageItemPromotions::getCompanyId, companyId)
									.eq(PackageItemPromotions::getPackageId, packageId));
			if (items == null || items.isEmpty()) {
				throw new IllegalStateException("package item rows missing for cancel");
			}
			for (PackageItemPromotions item : items) {
				LambdaUpdateWrapper<PackageItemPromotions> itemUw =
						new LambdaUpdateWrapper<PackageItemPromotions>()
								.eq(PackageItemPromotions::getPackageId, packageId)
								.eq(PackageItemPromotions::getCompanyId, companyId)
								.eq(PackageItemPromotions::getItemId, item.getItemId())
								.set(PackageItemPromotions::getEndTime, endEpoch)
								.set(PackageItemPromotions::getUpdated, nowEpoch);
				int itemRows = packageItemPromotionsMapper.update(null, itemUw);
				if (itemRows == 0) {
					throw new IllegalStateException("package item row update affected zero rows");
				}
			}

			final long shelvesCompanyId = companyId;
			final long shelvesPackageId = packageId;
			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				TransactionSynchronizationManager.registerSynchronization(
						new TransactionSynchronization() {
							@Override
							public void afterCommit() {
								salespersonItemsShelvesJobDispatchPublisher.publish(
										shelvesCompanyId, shelvesPackageId, "package");
							}
						});
			} else {
				salespersonItemsShelvesJobDispatchPublisher.publish(companyId, packageId, "package");
			}

			PackagePromotions fresh = packagePromotionsMapper.selectById(packageId);
			if (fresh == null || !Objects.equals(fresh.getCompanyId(), companyId)) {
				throw new IllegalStateException("package promotion cancel reload failed");
			}

			String vg = fresh.getValidGrade();
			List<String> validGradeParts;
			if (vg == null || !StringUtils.hasText(vg.trim())) {
				validGradeParts = List.of();
			} else {
				validGradeParts =
						Arrays.stream(vg.split(","))
								.map(String::trim)
								.filter(StringUtils::hasText)
								.collect(Collectors.toList());
			}

			return buildCancelWireMap(fresh, validGradeParts);
		} catch (Exception e) {
			log.error("packagePromotionCancel failed, packageId={}, companyId={}", packageId, companyId, e);
			throw new ResourceException(messageSource.getMessage("promotions.limit.cancel_failed", null, locale));
		}
	}

	private static Map<String, Object> buildCancelWireMap(PackagePromotions e, List<String> validGradeParts) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("package_id", wireBigint(e.getPackageId()));
		m.put("company_id", wireBigint(e.getCompanyId()));
		m.put("goods_id", wireBigint(e.getGoodsId()));
		m.put("main_item_id", wireBigint(e.getMainItemId()));
		m.put("main_item_price", e.getMainItemPrice());
		m.put("package_name", e.getPackageName());
		m.put("valid_grade", validGradeParts);
		m.put("used_platform", e.getUsedPlatform());
		m.put("free_postage", e.getFreePostage());
		m.put("package_total_price", e.getPackageTotalPrice());
		m.put("start_time", e.getStartTime());
		m.put("end_time", e.getEndTime());
		m.put("package_status", e.getPackageStatus());
		m.put("reason", e.getReason() == null ? "" : e.getReason());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("source_type", e.getSourceType());
		m.put("source_id", wireBigint(e.getSourceId()));
		return m;
	}

	private static String wireBigint(Long v) {
		return v == null ? null : Long.toString(v.longValue());
	}
}
