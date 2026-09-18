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

package cn.shopex.ecshopx.kaquan.service.vipgrade;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.kaquan.domain.VipGradeOrder;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VipGradeOrderListQueryService {

	private static final Logger log = LoggerFactory.getLogger(VipGradeOrderListQueryService.class);

	private static final String ORDER_STATUS_DONE = "DONE";

	private final VipGradeOrderMapper vipGradeOrderMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;

	public VipGradeOrderListQueryService(
			VipGradeOrderMapper vipGradeOrderMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper) {
		this.vipGradeOrderMapper = vipGradeOrderMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> listDoneOrders(long companyId, Long userIdFilterOrNull, int page, int pageSize) {
		LambdaQueryWrapper<VipGradeOrder> base = buildDoneOrderWrapper(companyId, userIdFilterOrNull);
		Long totalLong = vipGradeOrderMapper.selectCount(base);
		int total = totalLong == null ? 0 : totalLong.intValue();
		List<Map<String, Object>> list = new ArrayList<>();
		if (total > 0) {
			int offset = pageSize * (page - 1);
			LambdaQueryWrapper<VipGradeOrder> pageWrapper = buildDoneOrderWrapper(companyId, userIdFilterOrNull);
			pageWrapper.orderByDesc(VipGradeOrder::getCreated);
			pageWrapper.last("LIMIT " + offset + "," + pageSize);
			List<VipGradeOrder> records = vipGradeOrderMapper.selectList(pageWrapper);
			if (records != null) {
				for (VipGradeOrder e : records) {
					list.add(entityToRow(e));
				}
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", list);
		return out;
	}

	private static LambdaQueryWrapper<VipGradeOrder> buildDoneOrderWrapper(
			long companyId, Long userIdFilterOrNull) {
		LambdaQueryWrapper<VipGradeOrder> w = new LambdaQueryWrapper<VipGradeOrder>()
				.eq(VipGradeOrder::getCompanyId, (int) companyId)
				.eq(VipGradeOrder::getOrderStatus, ORDER_STATUS_DONE);
		if (userIdFilterOrNull != null) {
			w.eq(VipGradeOrder::getUserId, userIdFilterOrNull);
		}
		return w;
	}

	private Map<String, Object> entityToRow(VipGradeOrder e) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("order_id", e.getOrderId());
		row.put("vip_grade_id", e.getVipGradeId());
		row.put("lv_type", e.getLvType());
		row.put("company_id", e.getCompanyId());
		row.put("user_id", e.getUserId());
		row.put("mobile", resolveMobilePlain(e.getMobile()));
		row.put("title", e.getTitle());
		row.put("price", e.getPrice());
		row.put("card_type", parseJsonColumn(e.getCardType()));
		row.put("discount", e.getDiscount());
		row.put("shop_id", e.getShopId());
		row.put("distributor_id", e.getDistributorId());
		row.put("source_id", e.getSourceId());
		row.put("source_type", e.getSourceType());
		row.put("monitor_id", e.getMonitorId());
		row.put("order_status", e.getOrderStatus());
		row.put("created", e.getCreated());
		row.put("updated", e.getUpdated());
		row.put("fee_type", e.getFeeType());
		row.put("fee_rate", e.getFeeRate());
		row.put("fee_symbol", e.getFeeSymbol());
		return row;
	}

	private String resolveMobilePlain(String stored) {
		if (!StringUtils.hasText(stored)) {
			return stored;
		}
		String decrypted = sensitiveFieldEncryptor.decrypt(stored);
		if (decrypted != null && !decrypted.isBlank()) {
			return decrypted;
		}
		return stored;
	}

	private Object parseJsonColumn(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		try {
			JsonNode node = objectMapper.readTree(trimmed);
			return objectMapper.convertValue(node, Object.class);
		} catch (Exception ex) {
			log.warn("invalid JSON in vip grade order card_type: {}", ex.getMessage());
			return null;
		}
	}
}
