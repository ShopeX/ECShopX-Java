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

import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefit;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendBatch;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendItem;
import cn.shopex.ecshopx.shuyun.mapper.ShuyunOfflineBenefitMapper;
import cn.shopex.ecshopx.shuyun.mapper.ShuyunOfflineBenefitSendBatchMapper;
import cn.shopex.ecshopx.shuyun.mapper.ShuyunOfflineBenefitSendItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * C3–C5 线下权益回调业务。发券经 {@link OfflineBenefitSendBatchProcessor}（kaquan / stub）。
 */
@Service
public class OfflineBenefitCallbackService {

	public static final int MAX_BATCH_CUSTOMERS = 1000;
	private static final String KIND_SINGLE = "single";
	private static final String KIND_BATCH = "batch";

	private final ShuyunOfflineBenefitMapper benefitMapper;
	private final ShuyunOfflineBenefitSendBatchMapper batchMapper;
	private final ShuyunOfflineBenefitSendItemMapper itemMapper;
	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;
	private final OfflineBenefitSendBatchProcessor sendBatchProcessor;
	private final OfflineBenefitSendAsyncExecutor sendAsyncExecutor;
	private final TransactionTemplate transactionTemplate;

	public OfflineBenefitCallbackService(
			ShuyunOfflineBenefitMapper benefitMapper,
			ShuyunOfflineBenefitSendBatchMapper batchMapper,
			ShuyunOfflineBenefitSendItemMapper itemMapper,
			JdbcTemplate jdbcTemplate,
			ObjectMapper objectMapper,
			OfflineBenefitSendBatchProcessor sendBatchProcessor,
			OfflineBenefitSendAsyncExecutor sendAsyncExecutor,
			PlatformTransactionManager transactionManager) {
		this.benefitMapper = benefitMapper;
		this.batchMapper = batchMapper;
		this.itemMapper = itemMapper;
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
		this.sendBatchProcessor = sendBatchProcessor;
		this.sendAsyncExecutor = sendAsyncExecutor;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public String create(long companyId, Map<String, Object> body) {
		String benefitName = stringFromBody(body, "benefitName");
		if (!StringUtils.hasText(benefitName)) {
			throw new IllegalArgumentException("benefitName required");
		}
		String title = benefitName.trim();
		List<Long> cardIds =
				jdbcTemplate.query(
						"""
						SELECT card_id FROM kaquan_discount_cards
						WHERE company_id=? AND title=? AND kq_status=0
						""",
						(rs, i) -> rs.getLong(1),
						companyId,
						title);
		if (cardIds.isEmpty()) {
			throw new IllegalArgumentException("NO_MATCHING_COUPON_TEMPLATE");
		}
		if (cardIds.size() > 1) {
			throw new IllegalArgumentException("AMBIGUOUS_COUPON_TEMPLATE_MATCH");
		}
		long cardId = cardIds.get(0);
		String benefitId = String.valueOf(cardId);

		String startTime = stringFromBody(body, "startTime");
		String endTime = stringFromBody(body, "endTime");
		if (!StringUtils.hasText(startTime) || !StringUtils.hasText(endTime)) {
			throw new IllegalArgumentException("startTime and endTime required");
		}
		Integer effectiveStart = parseDateTimeToUnix(startTime);
		Integer effectiveEnd = parseDateTimeToUnix(endTime);
		if (effectiveStart == null || effectiveEnd == null) {
			throw new IllegalArgumentException("invalid startTime or endTime");
		}

		String clientId = stringFromBody(body, "clientId");
		String getStartRaw = stringFromBody(body, "getStartTime");
		String getEndRaw = stringFromBody(body, "getEndTime");
		String[] blobKeys = {
			"conditionGroup",
			"limitShops",
			"limitCustomers",
			"actionType",
			"actionValue",
			"quantity",
			"canGiftGiving",
			"limitExchangeNum"
		};
		Map<String, Object> blob = new LinkedHashMap<>();
		for (String k : blobKeys) {
			if (body.containsKey(k)) {
				blob.put(k, body.get(k));
			}
		}
		String conditionJson;
		try {
			conditionJson = blob.isEmpty() ? null : objectMapper.writeValueAsString(blob);
		} catch (Exception e) {
			throw new IllegalArgumentException("invalid condition blob");
		}

		ShuyunOfflineBenefit existing =
				benefitMapper.selectOne(
						new LambdaQueryWrapper<ShuyunOfflineBenefit>()
								.eq(ShuyunOfflineBenefit::getCompanyId, companyId)
								.eq(ShuyunOfflineBenefit::getBenefitId, benefitId)
								.last("LIMIT 1"));
		int now = (int) (System.currentTimeMillis() / 1000L);
		ShuyunOfflineBenefit entity = existing == null ? new ShuyunOfflineBenefit() : existing;
		entity.setCompanyId(companyId);
		entity.setBenefitId(benefitId);
		entity.setLocalCardId(cardId);
		if (clientId != null) {
			entity.setClientId(clientId);
		}
		entity.setBenefitName(benefitName);
		entity.setEffectiveStart(effectiveStart);
		entity.setEffectiveEnd(effectiveEnd);
		entity.setClaimStart(StringUtils.hasText(getStartRaw) ? parseDateTimeToUnix(getStartRaw) : null);
		entity.setClaimEnd(StringUtils.hasText(getEndRaw) ? parseDateTimeToUnix(getEndRaw) : null);
		entity.setConditionLimitsJson(conditionJson);
		entity.setUpdated(now);
		if (entity.getId() == null) {
			entity.setCreated(now);
			benefitMapper.insert(entity);
		} else {
			benefitMapper.updateById(entity);
		}
		return benefitId;
	}

	public Map<String, String> singleSend(long companyId, Map<String, Object> body) {
		String requestId = require(body, "requestId");
		String benefitId = require(body, "benefitId");
		String customerId = require(body, "customerId");
		if (!benefitExists(companyId, benefitId)) {
			throw new IllegalArgumentException("benefit not found");
		}
		ShuyunOfflineBenefitSendBatch existing = findBatch(companyId, requestId);
		if (existing != null) {
			finishSingleSendBatchIfPending(existing);
			return buildSingleSendHttpPayload(findBatch(companyId, requestId));
		}
		Long batchPk =
				transactionTemplate.execute(
						status -> {
							int now = (int) (System.currentTimeMillis() / 1000L);
							ShuyunOfflineBenefitSendBatch batch =
									newBatch(companyId, requestId, benefitId, KIND_SINGLE, body, now);
							batchMapper.insert(batch);
							itemMapper.insert(newItem(batch.getId(), customerId, now));
							return batch.getId();
						});
		if (batchPk == null) {
			throw new IllegalStateException("single send: batch id missing after persist");
		}
		sendBatchProcessor.process(batchPk);
		ShuyunOfflineBenefitSendBatch after = findBatch(companyId, requestId);
		if (after == null) {
			throw new IllegalStateException("single send: batch missing after process");
		}
		return buildSingleSendHttpPayload(after);
	}

	public Map<String, String> batchSend(long companyId, Map<String, Object> body) {
		String requestId = require(body, "requestId");
		String benefitId = require(body, "benefitId");
		Object customerListObj = body.get("customerList");
		if (!(customerListObj instanceof List<?> customerList)) {
			throw new IllegalArgumentException("customerList must be array");
		}
		if (customerList.isEmpty()) {
			throw new IllegalArgumentException("customerList must not be empty");
		}
		if (customerList.size() > MAX_BATCH_CUSTOMERS) {
			throw new IllegalArgumentException("customerList exceeds " + MAX_BATCH_CUSTOMERS);
		}
		if (!benefitExists(companyId, benefitId)) {
			throw new IllegalArgumentException("benefit not found");
		}
		ShuyunOfflineBenefitSendBatch existing = findBatch(companyId, requestId);
		if (existing != null) {
			return buildBatchSendHttpPayload(existing);
		}
		Long batchPk =
				transactionTemplate.execute(
						status -> {
							int now = (int) (System.currentTimeMillis() / 1000L);
							ShuyunOfflineBenefitSendBatch batch =
									newBatch(companyId, requestId, benefitId, KIND_BATCH, body, now);
							batch.setTotalCount(customerList.size());
							batchMapper.insert(batch);
							for (Object cid : customerList) {
								if (!(cid instanceof String) && !(cid instanceof Number)) {
									throw new IllegalArgumentException("customerList entries must be string or int");
								}
								String cidStr = String.valueOf(cid);
								if (!StringUtils.hasText(cidStr)) {
									throw new IllegalArgumentException("empty customerId in customerList");
								}
								itemMapper.insert(newItem(batch.getId(), cidStr, now));
							}
							return batch.getId();
						});
		if (batchPk == null) {
			throw new IllegalStateException("batch send: batch missing after persist");
		}
		sendAsyncExecutor.processAsync(batchPk);
		ShuyunOfflineBenefitSendBatch after = findBatch(companyId, requestId);
		if (after == null) {
			throw new IllegalStateException("batch send: batch missing after persist");
		}
		return buildBatchSendHttpPayload(after);
	}

	private void finishSingleSendBatchIfPending(ShuyunOfflineBenefitSendBatch batch) {
		if (!"pending".equals(batch.getStatus()) || batch.getId() == null) {
			return;
		}
		sendBatchProcessor.process(batch.getId());
	}

	private Map<String, String> buildSingleSendHttpPayload(ShuyunOfflineBenefitSendBatch batch) {
		if (batch == null) {
			throw new IllegalStateException("batch missing");
		}
		String batchId = batch.getRequestId();
		String status = batch.getStatus();
		if ("pending".equals(status) || "processing".equals(status)) {
			return Map.of(
					"batchId",
					batchId,
					"benefitCode",
					"",
					"message",
					"异步发放处理中，请稍后重试本接口或依赖数云明细推送获取结果");
		}
		String benefitCode = "";
		if ("done".equals(status)) {
			List<ShuyunOfflineBenefitSendItem> items =
					itemMapper.selectList(
							new LambdaQueryWrapper<ShuyunOfflineBenefitSendItem>()
									.eq(ShuyunOfflineBenefitSendItem::getBatchId, batch.getId()));
			for (ShuyunOfflineBenefitSendItem it : items) {
				if (StringUtils.hasText(it.getBenefitCode())) {
					benefitCode = it.getBenefitCode();
					break;
				}
			}
		}
		String message = "发送成功";
		if ("done".equals(status) && !StringUtils.hasText(benefitCode)) {
			message = firstFailureReason(batch.getId());
			if (!StringUtils.hasText(message)) {
				message = "发券失败";
			}
		}
		Map<String, String> out = new LinkedHashMap<>();
		out.put("batchId", batchId);
		out.put("benefitCode", benefitCode);
		out.put("message", message);
		return out;
	}

	private Map<String, String> buildBatchSendHttpPayload(ShuyunOfflineBenefitSendBatch batch) {
		String batchId = batch.getRequestId();
		String status = batch.getStatus();
		if ("pending".equals(status) || "processing".equals(status)) {
			return Map.of(
					"batchId",
					batchId,
					"message",
					"异步批量发放处理中，请稍后重试或依赖数云明细推送获取结果");
		}
		return Map.of("batchId", batchId, "message", "发送成功");
	}

	private String firstFailureReason(Long batchId) {
		List<ShuyunOfflineBenefitSendItem> items =
				itemMapper.selectList(
						new LambdaQueryWrapper<ShuyunOfflineBenefitSendItem>()
								.eq(ShuyunOfflineBenefitSendItem::getBatchId, batchId));
		for (ShuyunOfflineBenefitSendItem it : items) {
			if (StringUtils.hasText(it.getFailReason())) {
				return it.getFailReason();
			}
		}
		return "";
	}

	private ShuyunOfflineBenefitSendBatch newBatch(
			long companyId,
			String requestId,
			String benefitId,
			String kind,
			Map<String, Object> body,
			int now) {
		ShuyunOfflineBenefitSendBatch batch = new ShuyunOfflineBenefitSendBatch();
		batch.setCompanyId(companyId);
		batch.setRequestId(requestId);
		batch.setBenefitId(benefitId);
		batch.setSendKind(kind);
		batch.setStatus("pending");
		batch.setReportRetryCount(0);
		batch.setCreated(now);
		batch.setUpdated(now);
		String sendRemark = stringFromBody(body, "sendRemark");
		if (sendRemark != null) {
			batch.setSendRemark(sendRemark);
		}
		return batch;
	}

	private ShuyunOfflineBenefitSendItem newItem(Long batchId, String customerId, int now) {
		ShuyunOfflineBenefitSendItem item = new ShuyunOfflineBenefitSendItem();
		item.setBatchId(batchId);
		item.setCustomerId(customerId);
		item.setStatus("FAILURE");
		item.setCreated(now);
		item.setUpdated(now);
		return item;
	}

	private boolean benefitExists(long companyId, String benefitId) {
		Long c =
				benefitMapper.selectCount(
						new LambdaQueryWrapper<ShuyunOfflineBenefit>()
								.eq(ShuyunOfflineBenefit::getCompanyId, companyId)
								.eq(ShuyunOfflineBenefit::getBenefitId, benefitId));
		return c != null && c > 0;
	}

	private ShuyunOfflineBenefitSendBatch findBatch(long companyId, String requestId) {
		return batchMapper.selectOne(
				new LambdaQueryWrapper<ShuyunOfflineBenefitSendBatch>()
						.eq(ShuyunOfflineBenefitSendBatch::getCompanyId, companyId)
						.eq(ShuyunOfflineBenefitSendBatch::getRequestId, requestId)
						.last("LIMIT 1"));
	}

	private static String require(Map<String, Object> body, String key) {
		String v = stringFromBody(body, key);
		if (!StringUtils.hasText(v)) {
			throw new IllegalArgumentException(key + " required");
		}
		return v;
	}

	private static String stringFromBody(Map<String, Object> body, String key) {
		if (body == null || !body.containsKey(key) || body.get(key) == null) {
			return null;
		}
		return String.valueOf(body.get(key)).trim();
	}

	private static Integer parseDateTimeToUnix(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String s = raw.trim();
		if (s.chars().allMatch(Character::isDigit)) {
			try {
				return Integer.parseInt(s);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		try {
			LocalDateTime ldt =
					LocalDateTime.parse(s.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			return (int) ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
		} catch (DateTimeParseException e) {
			try {
				LocalDateTime ldt =
						LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
				return (int) ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
			} catch (DateTimeParseException e2) {
				return null;
			}
		}
	}
}
