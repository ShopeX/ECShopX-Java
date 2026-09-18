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

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.openapi.OpenapiAftersalesV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2AftersalesDetailService {

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final OpenapiThirdApiV2AftersalesListFormatSupport formatSupport;

	public OpenapiThirdApiV2AftersalesDetailService(
			AftersalesMapper aftersalesMapper,
			AftersalesRefundMapper aftersalesRefundMapper,
			OpenapiThirdApiV2AftersalesListFormatSupport formatSupport) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.formatSupport = formatSupport;
	}

	public Map<String, Object> getAftersalesDetail(long companyId, String aftersalesBnRaw) {
		if (!StringUtils.hasText(aftersalesBnRaw)) {
			throw missingParams("请填写售后单号");
		}

		Aftersales aftersales;
		try {
			aftersales =
					aftersalesMapper.selectOne(
							new LambdaQueryWrapper<Aftersales>()
									.eq(Aftersales::getCompanyId, companyId)
									.apply("aftersales_bn = {0}", aftersalesBnRaw.trim()));
		} catch (TooManyResultsException e) {
			throw aftersalesNotFound();
		}
		if (aftersales == null) {
			throw aftersalesNotFound();
		}

		List<AftersalesDetail> detailRows =
				formatSupport.loadDetailRowsForOpenApiDetail(companyId, aftersales.getAftersalesBn());
		String refundBn = resolveRefundBn(companyId, aftersales.getAftersalesBn());
		return formatSupport.formatOpenApiAftersalesDetailRow(aftersales, detailRows, refundBn);
	}

	private String resolveRefundBn(long companyId, long aftersalesBn) {
		AftersalesRefund refund;
		try {
			refund =
					aftersalesRefundMapper.selectOne(
							new LambdaQueryWrapper<AftersalesRefund>()
									.eq(AftersalesRefund::getCompanyId, companyId)
									.eq(AftersalesRefund::getAftersalesBn, aftersalesBn));
		} catch (TooManyResultsException e) {
			return "";
		}
		if (refund == null || refund.getRefundBn() == null) {
			return "";
		}
		return String.valueOf(refund.getRefundBn());
	}

	private static OpenapiAftersalesV2FailException missingParams(String message) {
		return new OpenapiAftersalesV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiAftersalesV2FailException aftersalesNotFound() {
		return new OpenapiAftersalesV2FailException(
				OpenapiErrorCode.ORDER_AFTERSALES_HANDLE_ERROR, "没有售后信息");
	}
}
