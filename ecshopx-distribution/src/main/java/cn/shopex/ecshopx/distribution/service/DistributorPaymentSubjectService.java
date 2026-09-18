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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.aftersales.repository.AftersalesRefundQueryRepository;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import cn.shopex.ecshopx.payment.dto.PaymentOpenStatusSnapshot;
import cn.shopex.ecshopx.payment.service.AlipayPaymentConfigValidationService;
import cn.shopex.ecshopx.payment.service.CompanyPaymentOpenStatusReadService;
import cn.shopex.ecshopx.payment.service.WxpayPaymentConfigValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DistributorPaymentSubjectService {

	private static final String MSG_NOT_FOUND = "店铺不存在或无权限";
	private static final String MSG_UNFINISHED_REFUND = "有未完成的退款单，不允许切换收款主体";
	private static final String MSG_NEED_ONE_PAYMENT =
			"切换为店铺收款主体时，至少需要开启微信支付或支付宝支付中的一种";

	private final DistributorWriteRepository distributorWriteRepository;
	private final AftersalesRefundQueryRepository aftersalesRefundQueryRepository;
	private final CompanyPaymentOpenStatusReadService companyPaymentOpenStatusReadService;
	private final WxpayPaymentConfigValidationService wxpayPaymentConfigValidationService;
	private final AlipayPaymentConfigValidationService alipayPaymentConfigValidationService;

	public DistributorPaymentSubjectService(
			DistributorWriteRepository distributorWriteRepository,
			AftersalesRefundQueryRepository aftersalesRefundQueryRepository,
			CompanyPaymentOpenStatusReadService companyPaymentOpenStatusReadService,
			WxpayPaymentConfigValidationService wxpayPaymentConfigValidationService,
			AlipayPaymentConfigValidationService alipayPaymentConfigValidationService) {
		this.distributorWriteRepository = distributorWriteRepository;
		this.aftersalesRefundQueryRepository = aftersalesRefundQueryRepository;
		this.companyPaymentOpenStatusReadService = companyPaymentOpenStatusReadService;
		this.wxpayPaymentConfigValidationService = wxpayPaymentConfigValidationService;
		this.alipayPaymentConfigValidationService = alipayPaymentConfigValidationService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void setPaymentSubject(long companyId, long distributorId, int paymentSubject) {
		if (distributorWriteRepository.selectSimpleByCompanyAndId(companyId, distributorId).isEmpty()) {
			throw new ResourceException(MSG_NOT_FOUND);
		}

		if (aftersalesRefundQueryRepository.countUnfinishedRefunds(companyId, distributorId) > 0) {
			throw new ResourceException(MSG_UNFINISHED_REFUND);
		}

		PaymentOpenStatusSnapshot open = companyPaymentOpenStatusReadService.getOpenStatus(companyId);

		if (paymentSubject == 1) {
			if (!open.isWxpayOpen() && !open.isAlipayOpen()) {
				throw new ResourceException(MSG_NEED_ONE_PAYMENT);
			}
			if (open.isWxpayOpen()) {
				wxpayPaymentConfigValidationService.assertConfigComplete(companyId, distributorId);
			}
			if (open.isAlipayOpen()) {
				alipayPaymentConfigValidationService.assertConfigComplete(companyId, distributorId);
			}
		} else {
			if (open.isWxpayOpen()) {
				wxpayPaymentConfigValidationService.assertConfigComplete(companyId, 0L);
			}
			if (open.isAlipayOpen()) {
				alipayPaymentConfigValidationService.assertConfigComplete(companyId, 0L);
			}
		}

		int rows = distributorWriteRepository.updatePaymentSubjectOnly(companyId, distributorId, paymentSubject);
		if (rows == 0) {
			throw new ResourceException(MSG_NOT_FOUND);
		}
	}
}
