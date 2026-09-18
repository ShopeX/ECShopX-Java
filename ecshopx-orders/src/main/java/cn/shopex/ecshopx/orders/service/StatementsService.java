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

package cn.shopex.ecshopx.orders.service;

import cn.shopex.ecshopx.common.cron.statement.StatementSettlementCursorRedisPort;
import cn.shopex.ecshopx.orders.domain.DistributionDistributorPeek;
import cn.shopex.ecshopx.orders.domain.StatementPeriodSetting;
import cn.shopex.ecshopx.orders.dto.ScheduleGenerateStatementsResult;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.StatementPeriodSettingMapper;
import cn.shopex.ecshopx.orders.statement.StatementPeriodValue;
import cn.shopex.ecshopx.orders.statement.StatementTimeWindowCalculator;
import cn.shopex.ecshopx.common.dispatch.GenerateStatementsJobDispatchPublisher;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StatementsService {

	private static final int PAGE = 1000;
	private static final String MT_DIST = "distributor";
	private static final String MT_SUP = "supplier";

	private final StatementSettlementCursorRedisPort cursorRedisPort;
	private final ShopMenuService shopMenuService;
	private final StatementPeriodSettingMapper statementPeriodSettingMapper;
	private final DistributionDistributorPeekMapper distributionDistributorPeekMapper;
	private final SupplierMapper supplierMapper;
	private final GenerateStatementsJobDispatchPublisher generateStatementsJobDispatchPublisher;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public StatementsService(
			StatementSettlementCursorRedisPort cursorRedisPort,
			ShopMenuService shopMenuService,
			StatementPeriodSettingMapper statementPeriodSettingMapper,
			DistributionDistributorPeekMapper distributionDistributorPeekMapper,
			SupplierMapper supplierMapper,
			GenerateStatementsJobDispatchPublisher generateStatementsJobDispatchPublisher,
			ObjectMapper objectMapper,
			@Autowired(required = false) Clock clock) {
		this.cursorRedisPort = cursorRedisPort;
		this.shopMenuService = shopMenuService;
		this.statementPeriodSettingMapper = statementPeriodSettingMapper;
		this.distributionDistributorPeekMapper = distributionDistributorPeekMapper;
		this.supplierMapper = supplierMapper;
		this.generateStatementsJobDispatchPublisher = generateStatementsJobDispatchPublisher;
		this.objectMapper = objectMapper;
		this.clock = clock == null ? Clock.system(StatementTimeWindowCalculator.SHANGHAI) : clock;
	}

	public ScheduleGenerateStatementsResult scheduleGenerateStatements() {
		int[] doGenBox = {0};
		int distRows = doStatementsForDistributor(doGenBox);
		int supRows = doStatementsForSupplier(doGenBox);
		return new ScheduleGenerateStatementsResult(doGenBox[0], distRows, supRows);
	}

	private int doStatementsForDistributor(int[] doGenBox) {
		int totalRows = 0;
		int off = 0;
		final Map<Long, String> productModel = new HashMap<>();
		while (true) {
			List<DistributionDistributorPeek> list =
					distributionDistributorPeekMapper.selectPageForStatementSchedule(off, PAGE);
			if (list == null) {
				list = List.of();
			}
			if (list.isEmpty()) {
				break;
			}
			Map<Long, StatementPeriodValue> byDist = new HashMap<>();
			Map<Long, StatementPeriodValue> defaultByCompany = new HashMap<>();
			loadDistributorSettings(list, byDist, defaultByCompany);
			for (DistributionDistributorPeek row : list) {
				totalRows++;
				try {
					String pm = productModel.computeIfAbsent(
							row.getCompanyId(), shopMenuService::resolveProductModelKeyForCompany);
					if (!"platform".equals(pm)) {
						continue;
					}
				} catch (Exception e) {
					continue;
				}
				StatementPeriodValue period = selectDistributorPeriod(row, byDist, defaultByCompany);
				if (period == null) {
					continue;
				}
				long last =
						toEpoch(
								cursorRedisPort
										.getDistributorLastEnd(row.getCompanyId(), row.getDistributorId())
										.orElse(null),
								row.getCreated());
				long end = StatementTimeWindowCalculator.computeScheduleWindowEndForLastLine(last, period.n(), period.unit());
				if (end > clock.instant().getEpochSecond()) {
					continue;
				}
				generateStatementsJobDispatchPublisher.publish(
						row.getCompanyId(),
						row.getDistributorId(),
						0L,
						period.n(),
						period.unit(),
						last,
						"distributor");
				doGenBox[0] += 1;
			}
			off += PAGE;
			if (list.size() < PAGE) {
				break;
			}
		}
		return totalRows;
	}

	private int doStatementsForSupplier(int[] doGenBox) {
		int totalRows = 0;
		int off = 0;
		final Map<Long, String> productModel = new HashMap<>();
		while (true) {
			List<Supplier> list = supplierMapper.selectPageForStatementSchedule(off, PAGE);
			if (list == null) {
				list = List.of();
			}
			if (list.isEmpty()) {
				break;
			}
			Map<Long, StatementPeriodValue> settleById = new HashMap<>();
			Map<Long, StatementPeriodValue> defaultByCompany = new HashMap<>();
			loadSupplierSettings(list, settleById, defaultByCompany);
			for (Supplier row : list) {
				totalRows++;
				try {
					productModel.computeIfAbsent(
							row.getCompanyId(), shopMenuService::resolveProductModelKeyForCompany);
				} catch (Exception e) {
					continue;
				}
				StatementPeriodValue period = selectSupplierPeriod(row, settleById, defaultByCompany);
				if (period == null) {
					continue;
				}
				long now = clock.instant().getEpochSecond();
				Optional<String> fromRedis = cursorRedisPort.getSupplierLastEnd(row.getCompanyId(), row.getId());
				long last;
				if (fromRedis.isPresent()) {
					try {
						last = Long.parseLong(fromRedis.get().trim());
					} catch (Exception e) {
						last = StatementTimeWindowCalculator.supplierInferredLastEndIfRedisMissing(period, now);
					}
				} else {
					last = StatementTimeWindowCalculator.supplierInferredLastEndIfRedisMissing(period, now);
				}
				long end = StatementTimeWindowCalculator.computeScheduleWindowEndForLastLine(last, period.n(), period.unit());
				if (end > now) {
					continue;
				}
				generateStatementsJobDispatchPublisher.publish(
						row.getCompanyId(),
						0L,
						row.getId(),
						period.n(),
						period.unit(),
						last,
						"supplier");
				doGenBox[0] += 1;
			}
			off += PAGE;
			if (list.size() < PAGE) {
				break;
			}
		}
		return totalRows;
	}

	private void loadDistributorSettings(
			List<DistributionDistributorPeek> list,
			Map<Long, StatementPeriodValue> byDist,
			Map<Long, StatementPeriodValue> defaultByCompany) {
		if (list.isEmpty()) {
			return;
		}
		List<Long> companyIds = list.stream().map(DistributionDistributorPeek::getCompanyId).distinct().toList();
		List<Long> dIds = list.stream().map(DistributionDistributorPeek::getDistributorId).toList();
		if (!companyIds.isEmpty()) {
			LambdaQueryWrapper<StatementPeriodSetting> defW =
					new LambdaQueryWrapper<StatementPeriodSetting>()
							.in(StatementPeriodSetting::getCompanyId, companyIds)
							.eq(StatementPeriodSetting::getDistributorId, 0L)
							.eq(StatementPeriodSetting::getMerchantType, MT_DIST);
			for (StatementPeriodSetting s : statementPeriodSettingMapper.selectList(defW)) {
				StatementPeriodValue v = StatementPeriodValue.tryParse(s.getPeriod(), objectMapper);
				if (v != null) {
					defaultByCompany.put(s.getCompanyId(), v);
				}
			}
		}
		if (!dIds.isEmpty()) {
			LambdaQueryWrapper<StatementPeriodSetting> w =
					new LambdaQueryWrapper<StatementPeriodSetting>()
							.in(StatementPeriodSetting::getDistributorId, dIds)
							.eq(StatementPeriodSetting::getMerchantType, MT_DIST);
			for (StatementPeriodSetting s : statementPeriodSettingMapper.selectList(w)) {
				StatementPeriodValue v = StatementPeriodValue.tryParse(s.getPeriod(), objectMapper);
				if (v != null) {
					byDist.put(s.getDistributorId(), v);
				}
			}
		}
	}

	private void loadSupplierSettings(
			List<Supplier> list,
			Map<Long, StatementPeriodValue> settleById,
			Map<Long, StatementPeriodValue> defaultByCompany) {
		if (list.isEmpty()) {
			return;
		}
		List<Long> companyIds = list.stream().map(Supplier::getCompanyId).distinct().toList();
		List<Long> sIds = list.stream().map(Supplier::getId).toList();
		if (!companyIds.isEmpty()) {
			LambdaQueryWrapper<StatementPeriodSetting> defW =
					new LambdaQueryWrapper<StatementPeriodSetting>()
							.in(StatementPeriodSetting::getCompanyId, companyIds)
							.eq(StatementPeriodSetting::getSupplierId, 0L)
							.eq(StatementPeriodSetting::getMerchantType, MT_SUP);
			for (StatementPeriodSetting s : statementPeriodSettingMapper.selectList(defW)) {
				StatementPeriodValue v = StatementPeriodValue.tryParse(s.getPeriod(), objectMapper);
				if (v != null) {
					defaultByCompany.put(s.getCompanyId(), v);
				}
			}
		}
		if (!sIds.isEmpty()) {
			LambdaQueryWrapper<StatementPeriodSetting> w =
					new LambdaQueryWrapper<StatementPeriodSetting>()
							.in(StatementPeriodSetting::getSupplierId, sIds)
							.eq(StatementPeriodSetting::getMerchantType, MT_SUP);
			for (StatementPeriodSetting s : statementPeriodSettingMapper.selectList(w)) {
				StatementPeriodValue v = StatementPeriodValue.tryParse(s.getPeriod(), objectMapper);
				if (v != null) {
					settleById.put(s.getSupplierId(), v);
				}
			}
		}
	}

	private static StatementPeriodValue selectDistributorPeriod(
			DistributionDistributorPeek row,
			Map<Long, StatementPeriodValue> byDist,
			Map<Long, StatementPeriodValue> defaultByCompany) {
		StatementPeriodValue p = byDist.get(row.getDistributorId());
		if (p != null) {
			return p;
		}
		return defaultByCompany.get(row.getCompanyId());
	}

	private static StatementPeriodValue selectSupplierPeriod(
			Supplier row, Map<Long, StatementPeriodValue> settle, Map<Long, StatementPeriodValue> def) {
		StatementPeriodValue p = settle.get(row.getId());
		if (p != null) {
			return p;
		}
		return def.get(row.getCompanyId());
	}

	private static long toEpoch(String redis, Integer created) {
		if (redis != null && !redis.isEmpty()) {
			try {
				return Long.parseLong(redis.trim());
			} catch (Exception ignored) {
			}
		}
		return created == null ? 0L : created.longValue();
	}
}
