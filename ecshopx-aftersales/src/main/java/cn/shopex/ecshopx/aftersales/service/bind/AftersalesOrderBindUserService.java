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

package cn.shopex.ecshopx.aftersales.service.bind;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;

@Service
public class AftersalesOrderBindUserService {

	private final AftersalesDetailMapper aftersalesDetailMapper;

	private final AftersalesMapper aftersalesMapper;

	private final AftersalesRefundMapper aftersalesRefundMapper;

	public AftersalesOrderBindUserService(
			AftersalesDetailMapper aftersalesDetailMapper,
			AftersalesMapper aftersalesMapper,
			AftersalesRefundMapper aftersalesRefundMapper) {
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesRefundMapper = aftersalesRefundMapper;
	}

	public void bindUserAftersales(long companyId, long orderId, long boundUserId, String mobile) {
		aftersalesMapper.update(
				null,
				new LambdaUpdateWrapper<Aftersales>()
						.eq(Aftersales::getCompanyId, companyId)
						.eq(Aftersales::getOrderId, orderId)
						.set(Aftersales::getUserId, boundUserId)
						.set(Aftersales::getMobile, mobile));
		aftersalesDetailMapper.update(
				null,
				new LambdaUpdateWrapper<AftersalesDetail>()
						.eq(AftersalesDetail::getCompanyId, companyId)
						.eq(AftersalesDetail::getOrderId, String.valueOf(orderId))
						.set(AftersalesDetail::getUserId, boundUserId));
		aftersalesRefundMapper.update(
				null,
				new LambdaUpdateWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getOrderId, orderId)
						.set(AftersalesRefund::getUserId, boundUserId));
	}
}
