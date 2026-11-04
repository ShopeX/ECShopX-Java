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

package cn.shopex.ecshopx.aftersales.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.auth.PosOrderDocAccessPort;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesBrokeragePort;
import cn.shopex.ecshopx.common.port.order.JushuitanSettingReadPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderLeftAftersalesWritePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AftersalesRefundConfirmServiceScopeTest {

	@Mock
	private AftersalesMapper aftersalesMapper;

	@Mock
	private AftersalesDetailMapper aftersalesDetailMapper;

	@Mock
	private AftersalesRefundService aftersalesRefundService;

	@Mock
	private AftersalesRefundAsyncPort aftersalesRefundAsyncPort;

	@Mock
	private JushuitanSettingReadPort jushuitanSettingReadPort;

	@Mock
	private AftersalesBrokeragePort aftersalesBrokeragePort;

	@Mock
	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	@Mock
	private NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	@Mock
	private PlatformTransactionManager platformTransactionManager;

	@Mock
	private JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher;

	@Mock
	private JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder;

	@Mock
	private WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher;

	@Mock
	private WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder;

	@Mock
	private ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher
			thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;

	@Mock
	private ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher;

	@Mock
	private PosOrderDocAccessPort posOrderDocAccessPort;

	@Mock
	private AftersalesRefuseWxaNotifyService aftersalesRefuseWxaNotifyService;

	@Mock
	private HttpServletRequest request;

	private AftersalesRefundConfirmService service;

	@BeforeEach
	void setUp() {
		service =
				new AftersalesRefundConfirmService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						jushuitanSettingReadPort,
						aftersalesBrokeragePort,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						applicationEventPublisher,
						new ObjectMapper(),
						platformTransactionManager,
						jushuitanTradeAftersalesDispatchPublisher,
						jushuitanTradeAftersalesBusPayloadBuilder,
						wdtErpTradeAfterSaleDispatchPublisher,
						wdtErpTradeAfterSaleBusPayloadBuilder,
						thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						posOrderDocAccessPort,
						aftersalesRefuseWxaNotifyService);
	}

	@Test
	void refundCheck_rejectsCrossShopBeforeConfirm() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", 1L);
		jwt.put("operator_id", 7L);
		jwt.put("operator_type", "distributor");
		jwt.put("distributor_id", 100L);
		lenient().when(request.getAttribute(anyString())).thenAnswer(invocation -> {
			String key = invocation.getArgument(0);
			if (OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA.equals(key)) {
				return jwt;
			}
			return null;
		});

		Aftersales row = new Aftersales();
		row.setCompanyId(1L);
		row.setAftersalesBn(9001L);
		row.setDistributorId(200L);
		row.setOrderId(55L);
		lenient()
				.when(aftersalesMapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<Aftersales>>any()))
				.thenReturn(row);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("aftersales_bn", 9001L);
		merged.put("check_refund", true);

		assertThrows(ForbiddenException.class, () -> service.refundCheck(merged, request));
		verify(posOrderDocAccessPort, never()).assertWriteAccess(any(), anyLong(), anyLong());
	}
}
