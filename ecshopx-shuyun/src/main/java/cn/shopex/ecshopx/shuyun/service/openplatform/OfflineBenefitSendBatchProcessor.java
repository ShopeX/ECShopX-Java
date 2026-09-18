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

import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendBatch;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendItem;
import cn.shopex.ecshopx.shuyun.mapper.ShuyunOfflineBenefitSendBatchMapper;
import cn.shopex.ecshopx.shuyun.mapper.ShuyunOfflineBenefitSendItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 批次履约：Issuer → 汇总 → report/detail v2。对齐 PHP {@code ShuyunOfflineBenefitSendBatchProcessor}。
 */
@Service
public class OfflineBenefitSendBatchProcessor {

	private static final Logger log = LoggerFactory.getLogger(OfflineBenefitSendBatchProcessor.class);
	private static final DateTimeFormatter SEND_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final ShuyunOfflineBenefitSendBatchMapper batchMapper;
	private final ShuyunOfflineBenefitSendItemMapper itemMapper;
	private final OfflineBenefitItemIssuer issuer;
	private final OfflineBenefitReportService reportService;
	private final ShuyunOpenPlatformProperties properties;

	public OfflineBenefitSendBatchProcessor(
			ShuyunOfflineBenefitSendBatchMapper batchMapper,
			ShuyunOfflineBenefitSendItemMapper itemMapper,
			OfflineBenefitItemIssuer issuer,
			OfflineBenefitReportService reportService,
			ShuyunOpenPlatformProperties properties) {
		this.batchMapper = batchMapper;
		this.itemMapper = itemMapper;
		this.issuer = issuer;
		this.reportService = reportService;
		this.properties = properties;
	}

	public void process(long batchId) {
		ShuyunOfflineBenefitSendBatch batch = batchMapper.selectById(batchId);
		if (batch == null) {
			log.warn("Shuyun offline benefit send: batch not found. batchId={}", batchId);
			return;
		}
		if (!"pending".equals(batch.getStatus())) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		batch.setStatus("processing");
		batch.setUpdated(now);
		batchMapper.updateById(batch);

		List<ShuyunOfflineBenefitSendItem> items =
				itemMapper.selectList(
						new LambdaQueryWrapper<ShuyunOfflineBenefitSendItem>()
								.eq(ShuyunOfflineBenefitSendItem::getBatchId, batchId));
		int success = 0;
		int failure = 0;
		for (ShuyunOfflineBenefitSendItem item : items) {
			OfflineBenefitIssueResult result = issuer.issue(batch, item);
			item.setSendTime(now);
			if (result.isSuccess()) {
				item.setStatus("SUCCESS");
				item.setBenefitCode(result.getBenefitCode());
				item.setFailReason(null);
				if (result.getMemberUserId() != null) {
					item.setMemberUserId(result.getMemberUserId());
				}
				success++;
			} else {
				item.setStatus("FAILURE");
				item.setBenefitCode(null);
				item.setFailReason(
						StringUtils.hasText(result.getFailReason()) ? result.getFailReason() : "ISSUE_FAILED");
				failure++;
			}
			if (!StringUtils.hasText(item.getSendReason()) && StringUtils.hasText(batch.getSendRemark())) {
				item.setSendReason(batch.getSendRemark());
			}
			item.setUpdated(now);
			itemMapper.updateById(item);
		}

		batch.setTotalCount(items.size());
		batch.setSuccessCount(success);
		batch.setFailureCount(failure);
		batch.setStatus("done");
		batch.setUpdated(now);
		batchMapper.updateById(batch);

		String platform = properties.getOfflineBenefitGatewayPlatform();
		if (!StringUtils.hasText(platform)) {
			platform = "offline";
		}
		platform = platform.trim().toLowerCase();

		List<Map<String, Object>> detailRows = buildDetailRows(batch, items, now);
		int maxCycles = Math.max(1, properties.getOfflineBenefitReportPushMaxCycles());
		boolean okSummary = false;
		boolean okDetail = false;
		long companyId = batch.getCompanyId() == null ? 0L : batch.getCompanyId();

		for (int cycle = 0; cycle < maxCycles; cycle++) {
			if (!okSummary) {
				okSummary =
						reportService.pushSendReportV2(
								companyId,
								platform,
								batch.getBenefitId(),
								batch.getRequestId(),
								items.size(),
								success,
								failure);
			}
			if (!okDetail) {
				okDetail = reportService.pushSendResultDetailV2(companyId, platform, detailRows);
			}
			if (okSummary && okDetail) {
				batch.setReportPushedAt((int) (System.currentTimeMillis() / 1000L));
				batch.setReportLastError(null);
				batchMapper.updateById(batch);
				return;
			}
			int retry = batch.getReportRetryCount() == null ? 0 : batch.getReportRetryCount();
			batch.setReportRetryCount(retry + 1);
			batch.setReportLastError(
					String.format(
							"offline_benefit_report_push_failed cycle=%d/%d summary=%s detail=%s",
							cycle + 1,
							maxCycles,
							okSummary ? "1" : "0",
							okDetail ? "1" : "0"));
			batchMapper.updateById(batch);
		}
	}

	private List<Map<String, Object>> buildDetailRows(
			ShuyunOfflineBenefitSendBatch batch, List<ShuyunOfflineBenefitSendItem> items, int fallbackTime) {
		List<Map<String, Object>> rows = new ArrayList<>();
		for (ShuyunOfflineBenefitSendItem item : items) {
			int ts = item.getSendTime() != null ? item.getSendTime() : fallbackTime;
			String sendTimeStr = SEND_TIME_FMT.format(Instant.ofEpochSecond(ts));
			String sendReason =
					StringUtils.hasText(item.getSendReason())
							? item.getSendReason()
							: (batch.getSendRemark() == null ? "" : batch.getSendRemark());
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("requestId", batch.getRequestId());
			row.put("benefitId", batch.getBenefitId());
			row.put("customerId", item.getCustomerId());
			row.put("benefitCode", item.getBenefitCode() == null ? "" : item.getBenefitCode());
			row.put("sendTime", sendTimeStr);
			row.put("sendReason", sendReason);
			row.put("status", item.getStatus());
			if ("FAILURE".equals(item.getStatus())) {
				row.put("failReason", item.getFailReason() == null ? "" : item.getFailReason());
			}
			rows.add(row);
		}
		return rows;
	}
}
