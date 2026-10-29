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

package cn.shopex.ecshopx.aftersales.service.saaserp;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.service.AftersalesReviewService;
import cn.shopex.ecshopx.common.port.distribution.DistributorDefaultAftersalesAddressReadPort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpAftersaleStatusPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * SaaS ERP {@code aftersaleStatusUpdate}. Approve and reject go through
 * {@link AftersalesReviewService#review}, which already publishes the SaaS ERP update
 * (approve) and cancel (reject) dispatches that PHP {@code AftersalesService::review} emits.
 * This path does not publish {@code SaasErpAftersalesLogi} / {@code SaasErpAftersalesSpringEvent};
 * PHP {@code review} does not emit that logistics publish.
 */
@Service
public class AftersalesSaasErpAftersaleStatusAdapter implements SaasErpAftersaleStatusPort {

	private static final Logger log = LoggerFactory.getLogger(AftersalesSaasErpAftersaleStatusAdapter.class);

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesReviewService reviewService;
	private final DistributorDefaultAftersalesAddressReadPort addressPort;
	private final SaasErpAftersaleSendBackConfirm sendBackConfirm;

	public AftersalesSaasErpAftersaleStatusAdapter(
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			AftersalesReviewService reviewService,
			DistributorDefaultAftersalesAddressReadPort addressPort,
			PlatformTransactionManager transactionManager) {
		this.aftersalesMapper = aftersalesMapper;
		this.reviewService = reviewService;
		this.addressPort = addressPort;
		this.sendBackConfirm =
				new SaasErpAftersaleSendBackConfirm(
						aftersalesMapper, aftersalesDetailMapper, transactionManager);
	}

	@Override
	public List<Object> update(Map<String, String> params) {
		Aftersales row = findByAftersaleAndOrder(params);
		if (row == null) {
			throw new IllegalStateException("未获取到售后单信息");
		}
		List<Object> result = new ArrayList<>();
		try {
			String status = trim(params.get("status"));
			switch (status) {
				case "4" -> {
					if (progressIs(row, 0)) {
						result = asResultList(reviewService.review(approvedParams(params, row), null));
					}
				}
				case "2" -> result = asResultList(reviewService.review(rejectedParams(params, row), null));
				case "1" -> {
					if (progressIs(row, 2)) {
						long aftersalesBn = row.getAftersalesBn() == null ? 0L : row.getAftersalesBn();
						long companyId = row.getCompanyId() == null ? 0L : row.getCompanyId();
						result = asResultList(sendBackConfirm.sendBackConfirm(aftersalesBn, companyId));
					}
				}
				default -> {
					// PHP switch leaves the result as the empty array.
				}
			}
		} catch (Exception ex) {
			log.warn("saaserp aftersaleStatusUpdate failed", ex);
		}
		return result;
	}

	private Aftersales findByAftersaleAndOrder(Map<String, String> params) {
		Long aftersalesBn = parseLong(params.get("aftersale_id"));
		Long orderId = parseLong(params.get("order_id"));
		if (aftersalesBn == null || orderId == null) {
			return null;
		}
		return aftersalesMapper.selectOne(
				new LambdaQueryWrapper<Aftersales>()
						.eq(Aftersales::getAftersalesBn, aftersalesBn)
						.eq(Aftersales::getOrderId, orderId)
						.last("LIMIT 1"));
	}

	private Map<String, Object> approvedParams(Map<String, String> params, Aftersales row) {
		Map<String, Object> data = reviewParams(params, row);
		data.put("is_approved", 1);
		data.put("aftersales_address_id", logisticsReturnAddressId(row));
		return data;
	}

	private Map<String, Object> rejectedParams(Map<String, String> params, Aftersales row) {
		Map<String, Object> data = reviewParams(params, row);
		data.put("is_approved", 0);
		return data;
	}

	private static Map<String, Object> reviewParams(Map<String, String> params, Aftersales row) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("aftersales_bn", row.getAftersalesBn());
		data.put("refuse_reason", refuseReason(params));
		data.put("company_id", row.getCompanyId());
		// PHP review defaults these when the inbound payload omits them.
		data.put("operator_type", "system");
		data.put("operator_id", 0);
		return data;
	}

	private long logisticsReturnAddressId(Aftersales row) {
		if (!"REFUND_GOODS".equals(row.getAftersalesType())) {
			return 0L;
		}
		long companyId = row.getCompanyId() == null ? 0L : row.getCompanyId();
		long distributorId = row.getDistributorId() == null ? 0L : row.getDistributorId();
		return addressPort
				.findDefaultLogisticsReturnAddressId(companyId, distributorId)
				.orElse(0L);
	}

	private static String refuseReason(Map<String, String> params) {
		String addon = params.get("addon");
		if (addon == null || "null".equals(addon)) {
			return "";
		}
		return addon.trim();
	}

	private static boolean progressIs(Aftersales row, int expected) {
		Integer progress = row.getProgress();
		return progress != null && progress.intValue() == expected;
	}

	private static List<Object> asResultList(Object value) {
		if (value == null) {
			return new ArrayList<>();
		}
		if (value instanceof List<?> list) {
			return new ArrayList<>(list);
		}
		List<Object> out = new ArrayList<>();
		out.add(value);
		return out;
	}

	private static Long parseLong(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		try {
			return Long.valueOf(trimmed);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static String trim(String raw) {
		return raw == null ? "" : raw.trim();
	}
}
