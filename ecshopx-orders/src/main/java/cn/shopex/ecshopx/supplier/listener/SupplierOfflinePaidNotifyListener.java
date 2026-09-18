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

package cn.shopex.ecshopx.supplier.listener;

import cn.shopex.ecshopx.common.supplier.SupplierOfflinePaidTemplateNotifyPort;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.event.SupplierOfflinePaidNotifyEvent;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SupplierOfflinePaidNotifyListener {

	private static final Logger log = LoggerFactory.getLogger(SupplierOfflinePaidNotifyListener.class);

	private static final DateTimeFormatter TEMPLATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final StringRedisTemplate stringRedisTemplate;
	private final SupplierOrderMapper supplierOrderMapper;
	private final SupplierMapper supplierMapper;
	private final SupplierOfflinePaidTemplateNotifyPort supplierOfflinePaidTemplateNotifyPort;
	private final MessageSource messageSource;

	public SupplierOfflinePaidNotifyListener(
			org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate,
			cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper supplierOrderMapper,
			cn.shopex.ecshopx.supplier.mapper.SupplierMapper supplierMapper,
			cn.shopex.ecshopx.common.supplier.SupplierOfflinePaidTemplateNotifyPort supplierOfflinePaidTemplateNotifyPort,
			MessageSource messageSource) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.supplierOrderMapper = supplierOrderMapper;
		this.supplierMapper = supplierMapper;
		this.supplierOfflinePaidTemplateNotifyPort = supplierOfflinePaidTemplateNotifyPort;
		this.messageSource = messageSource;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onSupplierOfflinePaidNotify(SupplierOfflinePaidNotifyEvent event) {
		try {
			Locale locale = LocaleContextHolder.getLocale();
			long companyId = event.getCompanyId();
			long orderId = event.getOrderId();
			long userId = event.getUserId();

			String wxaKey = "wxa_msg:" + orderId;
			if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(wxaKey))) {
				return;
			}
			stringRedisTemplate.opsForValue().set(wxaKey, "1", Duration.ofSeconds(10));

			List<SupplierOrder> lines = supplierOrderMapper.selectList(new LambdaQueryWrapper<SupplierOrder>()
					.eq(SupplierOrder::getCompanyId, companyId)
					.eq(SupplierOrder::getOrderId, orderId)
					.eq(SupplierOrder::getUserId, userId));
			if (lines == null || lines.isEmpty()) {
				return;
			}

			Set<Long> supplierIdSet = new LinkedHashSet<>();
			for (SupplierOrder line : lines) {
				if (line.getSupplierId() != null) {
					supplierIdSet.add(line.getSupplierId().longValue());
				}
			}
			List<Long> supplierIds = new ArrayList<>(supplierIdSet);
			if (supplierIds.isEmpty()) {
				return;
			}

			List<Supplier> suppliers = supplierMapper.selectList(new LambdaQueryWrapper<Supplier>()
					.in(Supplier::getOperatorId, supplierIds));
			Map<Long, Supplier> byOperatorId = new LinkedHashMap<>();
			if (suppliers != null) {
				for (Supplier s : suppliers) {
					if (s.getOperatorId() != null && !byOperatorId.containsKey(s.getOperatorId())) {
						byOperatorId.put(s.getOperatorId(), s);
					}
				}
			}

			for (SupplierOrder line : lines) {
				if (line.getSupplierId() == null) {
					continue;
				}
				long sid = line.getSupplierId().longValue();
				Supplier sup = byOperatorId.get(sid);
				if (sup == null) {
					continue;
				}
				String wxOpenid = sup.getWxOpenid();
				if (wxOpenid == null || wxOpenid.isBlank()) {
					continue;
				}
				String[] parts = wxOpenid.split("\\R");
				for (String part : parts) {
					String openid = part.trim();
					if (openid.isEmpty()) {
						continue;
					}
					Map<String, Object> msgData = buildOfflinePaidTemplateMsgData(line, sup, locale);
					supplierOfflinePaidTemplateNotifyPort.notifySingle(companyId, openid, msgData);
				}
			}
		} catch (RuntimeException ex) {
			log.warn("Supplier offline paid notify listener failed: {}", ex.getMessage(), ex);
		}
	}

	private Map<String, Object> buildOfflinePaidTemplateMsgData(
			SupplierOrder line, Supplier sup, Locale locale) {
		String bankAccount = sup.getBankAccount();
		if (bankAccount == null || bankAccount.isBlank()) {
			bankAccount = messageSource.getMessage(
					"supplier.wxapp.get_offline_pay_info.unknown_account", null, locale);
		}
		String amountYuan = centsStringToYuan(line.getTotalFee());
		String timeStr = TEMPLATE_TIME.format(LocalDateTime.now());
		Map<String, Object> msgData = new LinkedHashMap<>();
		msgData.put("number4", bankAccount);
		msgData.put("time5", timeStr);
		msgData.put("phrase2", "线下转账");
		msgData.put("amount6", amountYuan);
		msgData.put("const7", "线下付款");
		return msgData;
	}

	private static String centsStringToYuan(String totalFeeCents) {
		if (totalFeeCents == null || totalFeeCents.isBlank()) {
			return "0.00";
		}
		try {
			return new BigDecimal(totalFeeCents.trim())
					.movePointLeft(2)
					.setScale(2, RoundingMode.HALF_UP)
					.toPlainString();
		} catch (RuntimeException e) {
			return "0.00";
		}
	}
}
