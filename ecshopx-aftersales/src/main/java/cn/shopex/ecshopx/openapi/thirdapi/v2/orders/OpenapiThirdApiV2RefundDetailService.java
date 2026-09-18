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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.openapi.OpenapiAftersalesV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiCancelOrderReasonPort;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2RefundDetailService {

	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final OpenapiThirdApiV2RefundListFormatSupport refundListFormatSupport;
	private final OpenapiCancelOrderReasonPort cancelOrderReasonPort;

	public OpenapiThirdApiV2RefundDetailService(
			AftersalesRefundMapper aftersalesRefundMapper,
			OpenapiThirdApiV2RefundListFormatSupport refundListFormatSupport,
			OpenapiCancelOrderReasonPort cancelOrderReasonPort) {
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.refundListFormatSupport = refundListFormatSupport;
		this.cancelOrderReasonPort = cancelOrderReasonPort;
	}

	public Map<String, Object> getRefundDetail(long companyId, String refundBnRaw) {
		if (!StringUtils.hasText(refundBnRaw)) {
			throw missingParams("请填写退款单号");
		}

		try {
			AftersalesRefund refund;
			try {
				refund =
						aftersalesRefundMapper.selectOne(
								new LambdaQueryWrapper<AftersalesRefund>()
										.eq(AftersalesRefund::getCompanyId, companyId)
										.apply("refund_bn = {0}", refundBnRaw.trim()));
			} catch (TooManyResultsException e) {
				throw refundHandleError(e.getMessage());
			}
			if (refund == null) {
				throw refundNotFound();
			}

			String cancelReason = "";
			if (isEmptyAftersalesBn(refund.getAftersalesBn())) {
				cancelReason =
						cancelOrderReasonPort.findCancelReason(companyId, refund.getOrderId());
			}

			return refundListFormatSupport.formatOpenApiRefundDetail(refund, cancelReason);
		} catch (OpenapiAftersalesV2FailException e) {
			throw e;
		} catch (Exception e) {
			throw refundHandleError(e.getMessage());
		}
	}

	private static boolean isEmptyAftersalesBn(Long aftersalesBn) {
		return aftersalesBn == null || aftersalesBn == 0L;
	}

	private static OpenapiAftersalesV2FailException missingParams(String message) {
		return new OpenapiAftersalesV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiAftersalesV2FailException refundNotFound() {
		return new OpenapiAftersalesV2FailException(
				OpenapiErrorCode.ORDER_REFUND_HANDLE_ERROR,
				"Undefined array key \"refund_channel\"");
	}

	private static OpenapiAftersalesV2FailException refundHandleError(String message) {
		return new OpenapiAftersalesV2FailException(OpenapiErrorCode.ORDER_REFUND_HANDLE_ERROR, message);
	}
}
