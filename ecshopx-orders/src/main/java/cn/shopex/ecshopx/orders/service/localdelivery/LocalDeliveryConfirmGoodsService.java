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

package cn.shopex.ecshopx.orders.service.localdelivery;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.localdelivery.DadaLocalDeliveryConfirmGoodsPort;
import cn.shopex.ecshopx.common.port.localdelivery.ShansongLocalDeliveryConfirmGoodsPort;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelDada;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalDeliveryConfirmGoodsService {

	private final NormalOrdersRelDadaMapper normalOrdersRelDadaMapper;
	private final DadaLocalDeliveryConfirmGoodsPort dadaLocalDeliveryConfirmGoodsPort;
	private final ShansongLocalDeliveryConfirmGoodsPort shansongLocalDeliveryConfirmGoodsPort;
	private final String localDeliveryDriver;

	public LocalDeliveryConfirmGoodsService(
			NormalOrdersRelDadaMapper normalOrdersRelDadaMapper,
			DadaLocalDeliveryConfirmGoodsPort dadaLocalDeliveryConfirmGoodsPort,
			ShansongLocalDeliveryConfirmGoodsPort shansongLocalDeliveryConfirmGoodsPort,
			@Value("${common.local-delivery-dirver:dada}") String localDeliveryDriver) {
		this.normalOrdersRelDadaMapper = normalOrdersRelDadaMapper;
		this.dadaLocalDeliveryConfirmGoodsPort = dadaLocalDeliveryConfirmGoodsPort;
		this.shansongLocalDeliveryConfirmGoodsPort = shansongLocalDeliveryConfirmGoodsPort;
		this.localDeliveryDriver = localDeliveryDriver;
	}

	public void confirmGoods(long companyId, String orderIdParam) {
		String oid = orderIdParam == null ? "" : orderIdParam.trim();
		String driver =
				localDeliveryDriver == null
						? "dada"
						: localDeliveryDriver.trim().toLowerCase(Locale.ROOT);
		if (!"dada".equals(driver) && !"shansong".equals(driver)) {
			throw new ResourceException("同城配仅支持达达和闪送");
		}

		NormalOrdersRelDada row =
				normalOrdersRelDadaMapper.selectOne(
						new LambdaQueryWrapper<NormalOrdersRelDada>()
								.eq(NormalOrdersRelDada::getCompanyId, companyId)
								.apply("CAST(order_id AS CHAR) = {0}", oid)
								.last("LIMIT 1"));
		if (row == null) {
			if ("shansong".equals(driver)) {
				throw new ResourceException("未查询到闪送订单");
			}
			throw new ResourceException("未查询到达达相关数据");
		}
		if (row.getOrderId() == null) {
			if ("shansong".equals(driver)) {
				throw new ResourceException("未查询到闪送订单");
			}
			throw new ResourceException("未查询到达达相关数据");
		}

		Integer st = row.getDadaStatus();
		if (st == null || st.intValue() != 9) {
			throw new ResourceException("订单状态不正确，无需此操作");
		}

		if ("dada".equals(driver)) {
			dadaLocalDeliveryConfirmGoodsPort.confirmGoods(companyId, oid);
		} else {
			String no = row.getDadaDeliveryNo() == null ? "" : row.getDadaDeliveryNo().trim();
			if (no.isEmpty()) {
				throw new ResourceException("闪送确认退回缺少运单号");
			}
			shansongLocalDeliveryConfirmGoodsPort.confirmGoodsReturn(companyId, no);
		}
	}
}
