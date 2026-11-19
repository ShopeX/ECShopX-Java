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
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * PHP {@code AftersalesService::sendBackConfirm}: merchant confirms the returned goods.
 * Sets {@code progress=8} on the aftersales row and its details. Does not publish SaaS ERP,
 * Jushuitan, or WDT events; PHP {@code review} and this method do not emit those.
 */
class SaasErpAftersaleSendBackConfirm {

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final TransactionTemplate transactionTemplate;

	SaasErpAftersaleSendBackConfirm(
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			PlatformTransactionManager transactionManager) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	Map<String, Object> sendBackConfirm(long aftersalesBn, long companyId) {
		Aftersales existing =
				aftersalesMapper.selectOne(
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getAftersalesBn, aftersalesBn)
								.last("LIMIT 1"));
		if (existing == null) {
			throw new ResourceException("售后单不存在");
		}
		return transactionTemplate.execute(status -> applyConfirm(existing, aftersalesBn, companyId));
	}

	private Map<String, Object> applyConfirm(Aftersales existing, long aftersalesBn, long companyId) {
		int updated =
				aftersalesMapper.update(
						null,
						new LambdaUpdateWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getAftersalesBn, aftersalesBn)
								.set(Aftersales::getProgress, 8));
		if (updated == 0) {
			throw new ResourceException("售后单号为" + aftersalesBn + "的售后单不存在");
		}
		long detailCount =
				aftersalesDetailMapper.selectCount(
						new LambdaQueryWrapper<AftersalesDetail>()
								.eq(AftersalesDetail::getCompanyId, companyId)
								.eq(AftersalesDetail::getAftersalesBn, aftersalesBn));
		if (detailCount == 0L) {
			throw new ResourceException("未查询到更新数据");
		}
		int detailUpdated =
				aftersalesDetailMapper.update(
						null,
						new LambdaUpdateWrapper<AftersalesDetail>()
								.eq(AftersalesDetail::getCompanyId, companyId)
								.eq(AftersalesDetail::getAftersalesBn, aftersalesBn)
								.set(AftersalesDetail::getProgress, 8));
		if (detailUpdated == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		existing.setProgress(8);
		return toAftersalesData(existing);
	}

	private static Map<String, Object> toAftersalesData(Aftersales a) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("aftersales_bn", a.getAftersalesBn());
		m.put("order_id", a.getOrderId());
		m.put("company_id", a.getCompanyId());
		m.put("user_id", a.getUserId());
		m.put("salesman_id", a.getSalesmanId());
		m.put("shop_id", a.getShopId());
		m.put("distributor_id", a.getDistributorId());
		m.put("supplier_id", a.getSupplierId());
		m.put("aftersales_type", a.getAftersalesType());
		m.put("aftersales_status", a.getAftersalesStatus());
		m.put("progress", a.getProgress());
		m.put("refund_fee", a.getRefundFee());
		m.put("refund_point", a.getRefundPoint());
		m.put("reason", a.getReason());
		m.put("description", a.getDescription());
		m.put("evidence_pic", a.getEvidencePic());
		m.put("refuse_reason", a.getRefuseReason());
		m.put("memo", a.getMemo());
		m.put("sendback_data", a.getSendbackData());
		m.put("sendconfirm_data", a.getSendconfirmData());
		m.put("create_time", a.getCreateTime());
		m.put("update_time", a.getUpdateTime());
		m.put("item_bn", a.getItemBn());
		m.put("third_data", a.getThirdData());
		m.put("aftersales_address", a.getAftersalesAddress());
		m.put("distributor_remark", a.getDistributorRemark());
		m.put("contact", a.getContact());
		m.put("mobile", a.getMobile());
		m.put("merchant_id", a.getMerchantId());
		m.put("is_partial_cancel", a.getIsPartialCancel());
		m.put("return_type", a.getReturnType());
		m.put("return_distributor_id", a.getReturnDistributorId());
		m.put("self_delivery_operator_id", a.getSelfDeliveryOperatorId());
		m.put("freight", a.getFreight());
		m.put("freight_type", a.getFreightType());
		return m;
	}
}
