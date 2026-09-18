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

package cn.shopex.ecshopx.supplier.service.front;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelSupplier;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelSupplierMapper;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import cn.shopex.ecshopx.supplier.service.front.dto.OfflinePayInfoEntry;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SupplierOrderOfflinePayInfoServiceImpl implements SupplierOrderOfflinePayInfoService {

	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final NormalOrdersRelSupplierMapper normalOrdersRelSupplierMapper;
	private final SupplierMapper supplierMapper;
	private final MessageSource messageSource;

	public SupplierOrderOfflinePayInfoServiceImpl(
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			NormalOrdersRelSupplierMapper normalOrdersRelSupplierMapper,
			SupplierMapper supplierMapper,
			MessageSource messageSource) {
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.normalOrdersRelSupplierMapper = normalOrdersRelSupplierMapper;
		this.supplierMapper = supplierMapper;
		this.messageSource = messageSource;
	}

	@Override
	public List<OfflinePayInfoEntry> getOfflinePayInfo(long companyId, long userId, String orderIdRaw) {
		Locale locale = LocaleContextHolder.getLocale();
		if (orderIdRaw == null || orderIdRaw.trim().isEmpty()) {
			throw new ResourceException(messageSource.getMessage(
					"supplier.wxapp.get_offline_pay_info.order_id_required", null, locale));
		}
		String trimmed = orderIdRaw.trim();
		long orderId;
		try {
			orderId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException(messageSource.getMessage(
					"supplier.wxapp.get_offline_pay_info.order_not_exist_or_no_permission", null, locale));
		}

		LambdaQueryWrapper<NormalOrdersItems> w = new LambdaQueryWrapper<>();
		w.eq(NormalOrdersItems::getCompanyId, companyId)
				.eq(NormalOrdersItems::getUserId, userId)
				.eq(NormalOrdersItems::getOrderId, orderId)
				.orderByAsc(NormalOrdersItems::getId);
		List<NormalOrdersItems> lines = normalOrdersItemsMapper.selectList(w);
		if (lines.isEmpty()) {
			throw new ResourceException(messageSource.getMessage(
					"supplier.wxapp.get_offline_pay_info.order_not_exist_or_no_permission", null, locale));
		}

		Set<Integer> operatorIds = lines.stream()
				.map(NormalOrdersItems::getSupplierId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, Supplier> supplierByOperatorId = Collections.emptyMap();
		if (!operatorIds.isEmpty()) {
			LambdaQueryWrapper<Supplier> sw = new LambdaQueryWrapper<Supplier>().in(Supplier::getOperatorId, operatorIds);
			List<Supplier> suppliers = supplierMapper.selectList(sw);
			Map<Long, Supplier> built = new LinkedHashMap<>();
			for (Supplier s : suppliers) {
				if (s.getOperatorId() != null) {
					built.put(s.getOperatorId(), s);
				}
			}
			supplierByOperatorId = built;
		}

		Map<Integer, OfflinePayInfoEntry> acc = new LinkedHashMap<>();
		for (NormalOrdersItems line : lines) {
			int sid = line.getSupplierId() == null ? 0 : line.getSupplierId();
			BigDecimal lineYuan = centsToYuan(line.getTotalFee());
			if (acc.containsKey(sid)) {
				OfflinePayInfoEntry entry = acc.get(sid);
				entry.setTotalFee(entry.getTotalFee().add(lineYuan));
			} else {
				NormalOrdersRelSupplier rel = normalOrdersRelSupplierMapper.selectOne(
						new LambdaQueryWrapper<NormalOrdersRelSupplier>()
								.eq(NormalOrdersRelSupplier::getCompanyId, companyId)
								.eq(NormalOrdersRelSupplier::getOrderId, orderId)
								.eq(NormalOrdersRelSupplier::getSupplierId, sid)
								.last("LIMIT 1"));
				BigDecimal total = lineYuan;
				if (rel != null && rel.getFreightFee() != null && rel.getFreightFee() > 0) {
					total = total.add(centsToYuan(rel.getFreightFee()));
				}
				Supplier sup = supplierByOperatorId.get((long) sid);
				String bankName = sup != null && sup.getBankName() != null && !sup.getBankName().isBlank()
						? sup.getBankName()
						: messageSource.getMessage("supplier.wxapp.get_offline_pay_info.unknown_bank", null, locale);
				String bankAccount = sup != null && sup.getBankAccount() != null && !sup.getBankAccount().isBlank()
						? sup.getBankAccount()
						: messageSource.getMessage("supplier.wxapp.get_offline_pay_info.unknown_account", null, locale);
				acc.put(sid, new OfflinePayInfoEntry(total, sid, bankName, bankAccount));
			}
		}
		return new ArrayList<>(acc.values());
	}

	private static BigDecimal centsToYuan(Integer cents) {
		if (cents == null) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		return BigDecimal.valueOf(cents.longValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
	}
}
