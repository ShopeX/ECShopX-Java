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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import cn.shopex.ecshopx.shuyun.auth.InboundSignedCallbackPreparer;
import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendBatch;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendItem;
import cn.shopex.ecshopx.shuyun.mapper.ShuyunOfflineBenefitSendBatchMapper;
import cn.shopex.ecshopx.shuyun.mapper.ShuyunOfflineBenefitSendItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** D11：支付 USED / 取消 NOT_USED → result.push.v2。 */
@Service
public class OfflineBenefitConsumePushService {

	private static final Logger log = LoggerFactory.getLogger(OfflineBenefitConsumePushService.class);
	private static final DateTimeFormatter DT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final OpenPlatformConfigService openPlatformConfigService;
	private final OfflineBenefitReportService reportService;
	private final ShuyunOfflineBenefitSendItemMapper itemMapper;
	private final ShuyunOfflineBenefitSendBatchMapper batchMapper;
	private final ShuyunOpenPlatformProperties properties;
	private final JdbcTemplate jdbcTemplate;

	public OfflineBenefitConsumePushService(
			OpenPlatformConfigService openPlatformConfigService,
			OfflineBenefitReportService reportService,
			ShuyunOfflineBenefitSendItemMapper itemMapper,
			ShuyunOfflineBenefitSendBatchMapper batchMapper,
			ShuyunOpenPlatformProperties properties,
			JdbcTemplate jdbcTemplate) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.reportService = reportService;
		this.itemMapper = itemMapper;
		this.batchMapper = batchMapper;
		this.properties = properties;
		this.jdbcTemplate = jdbcTemplate;
	}

	public void handlePaySuccess(long companyId, String orderId) {
		pushConsume(companyId, orderId, "USED");
	}

	public void handleOrderCancel(long companyId, String orderId) {
		pushConsume(companyId, orderId, "NOT_USED");
	}

	private void pushConsume(long companyId, String orderId, String status) {
		if (companyId < 1 || !StringUtils.hasText(orderId)) {
			return;
		}
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		if (!InboundSignedCallbackPreparer.isEligible(cfg)) {
			return;
		}
		if (shouldSkipByOrder(companyId, orderId)) {
			return;
		}
		String shopId = resolveShopIdForOrder(companyId, orderId);
		if (shopId == null) {
			return;
		}
		Long localOrderId = parseOrderIdAsLong(orderId);
		if (localOrderId == null || localOrderId < 1) {
			log.info(
					"Shuyun offline_benefit_consume skipped: non-numeric order_id companyId={} orderId={}",
					companyId,
					orderId);
			return;
		}
		List<ShuyunOfflineBenefitSendItem> items =
				itemMapper.selectList(
						new LambdaQueryWrapper<ShuyunOfflineBenefitSendItem>()
								.eq(ShuyunOfflineBenefitSendItem::getLocalOrderId, localOrderId));
		if (items.isEmpty()) {
			return;
		}
		int when = (int) Instant.now().getEpochSecond();
		for (ShuyunOfflineBenefitSendItem item : items) {
			if (status.equals(item.getLastConsumeStatus())) {
				continue;
			}
			if ("NOT_USED".equals(status) && !"USED".equals(item.getLastConsumeStatus())) {
				// 取消仅回滚曾推过 USED 的明细
				continue;
			}
			ShuyunOfflineBenefitSendBatch batch = batchMapper.selectById(item.getBatchId());
			if (batch == null || !companyIdEquals(batch.getCompanyId(), companyId)) {
				continue;
			}
			Map<String, Object> row = buildResultRow(batch, item, orderId, status, when, shopId);
			if (reportService.pushResultV2(companyId, "offline", List.of(row))) {
				item.setLastConsumeStatus(status);
				item.setLastConsumePushAt(when);
				item.setUpdated(when);
				itemMapper.updateById(item);
			}
		}
	}

	private boolean shouldSkipByOrder(long companyId, String orderId) {
		List<Map<String, Object>> rows =
				jdbcTemplate.queryForList(
						"""
						SELECT user_id, total_fee FROM orders_normal_orders
						WHERE company_id=? AND order_id=? LIMIT 1
						""",
						companyId,
						orderId);
		if (rows.isEmpty()) {
			return false;
		}
		Map<String, Object> order = rows.get(0);
		long userId = toLong(order.get("user_id"));
		int totalFee = toInt(order.get("total_fee"));
		if (userId <= 0 || totalFee <= 0) {
			log.info(
					"Shuyun offline_benefit_consume skipped: user_id/total_fee invalid companyId={} orderId={}",
					companyId,
					orderId);
			return true;
		}
		return false;
	}

	private String resolveShopIdForOrder(long companyId, String orderId) {
		Long distributorId =
				jdbcTemplate.query(
						"""
						SELECT distributor_id FROM orders_normal_orders
						WHERE company_id=? AND order_id=? LIMIT 1
						""",
						rs -> rs.next() ? rs.getLong(1) : null,
						companyId,
						orderId);
		if (distributorId == null || distributorId <= 0) {
			return null;
		}
		Integer exists =
				jdbcTemplate.query(
						"""
						SELECT 1 FROM distribution_distributor
						WHERE company_id=? AND distributor_id=? LIMIT 1
						""",
						rs -> rs.next() ? 1 : null,
						companyId,
						distributorId);
		if (exists == null) {
			return null;
		}
		String suffix = properties.getOfflinePlatIdSuffix() == null ? "-off" : properties.getOfflinePlatIdSuffix();
		String id = String.valueOf(distributorId);
		if (!StringUtils.hasText(suffix) || id.endsWith(suffix)) {
			return id;
		}
		return id + suffix;
	}

	private static Map<String, Object> buildResultRow(
			ShuyunOfflineBenefitSendBatch batch,
			ShuyunOfflineBenefitSendItem item,
			String orderId,
			String status,
			int whenUnix,
			String shopId) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("benefitId", batch.getBenefitId());
		row.put("requestId", batch.getRequestId());
		row.put("benefitCode", item.getBenefitCode() == null ? "" : item.getBenefitCode());
		row.put("platCode", "OFFLINE");
		row.put("shopId", shopId);
		row.put("customerId", item.getCustomerId());
		row.put("status", status);
		row.put("orderId", orderId);
		row.put("useTime", DT.format(Instant.ofEpochSecond(whenUnix)));
		row.put("remark", "");
		return row;
	}

	private static boolean companyIdEquals(Long batchCompanyId, long companyId) {
		return batchCompanyId != null && batchCompanyId == companyId;
	}

	private static Long parseOrderIdAsLong(String orderId) {
		try {
			return Long.parseLong(orderId.trim());
		} catch (Exception e) {
			return null;
		}
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private static int toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (Exception e) {
			return 0;
		}
	}
}
