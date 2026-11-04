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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.service.operator.sms.CompanySceneSmsSendPort;
import cn.shopex.ecshopx.companys.service.setting.PickupcodeSettingRedisService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminEntityOrderDetailTypePolicy;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationEffectiveTypeService;
import cn.shopex.ecshopx.orders.service.normal.OrderZitiQrCodeRedisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class WxappOrderPickupCodeServiceTest {

	@Mock private OrderAssociationsMapper orderAssociationsMapper;
	@Mock private NormalOrdersMapper normalOrdersMapper;
	@Mock private OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;
	@Mock private AdminEntityOrderDetailTypePolicy adminEntityOrderDetailTypePolicy;
	@Mock private PickupcodeSettingRedisService pickupcodeSettingRedisService;
	@Mock private AdminNormalOrderDetailService adminNormalOrderDetailService;
	@Mock private CompanySceneSmsSendPort companySceneSmsSendPort;
	@Mock private OrderZitiQrCodeRedisService orderZitiQrCodeRedisService;
	@Mock private StringRedisTemplate companysRedisTemplate;
	@Mock private ValueOperations<String, String> valueOps;

	@Test
	@DisplayName("短信提货码发送 12 位长码并写入扫码 Redis / admin-pickupcode")
	void getOrderPickupCode_sendsTwelveDigitAndStoresBothMappings() {
		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(38L);
		assoc.setOrderId(100L);
		assoc.setUserId(261L);
		assoc.setOrderClass("normal");
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(assoc);
		when(orderAssociationEffectiveTypeService.effectiveOrderType(assoc)).thenReturn("normal");
		when(adminEntityOrderDetailTypePolicy.supportsNormalPipeline("normal")).thenReturn(true);
		when(pickupcodeSettingRedisService.handle(eq(38L), eq(null)))
				.thenReturn(Map.of("pickupcode_status", true));

		Map<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("order_id", 100L);
		orderInfo.put("order_status", "PAYED");
		orderInfo.put("ziti_status", "PENDING");
		orderInfo.put("mobile", "13761924721");
		when(adminNormalOrderDetailService.buildOrderBundle(eq(38L), eq("100"), eq(false), eq("api")))
				.thenReturn(Map.of("orderInfo", orderInfo));
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOps);
		when(companysRedisTemplate.getExpire(eq("admin-pickupcode:100|13761924721"), any()))
				.thenReturn(-2L);

		NormalOrders normal = new NormalOrders();
		normal.setOrderId(100L);
		normal.setZitiCode(760773L);
		when(normalOrdersMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(normal);

		WxappOrderPickupCodeService svc =
				new WxappOrderPickupCodeService(
						orderAssociationsMapper,
						normalOrdersMapper,
						orderAssociationEffectiveTypeService,
						adminEntityOrderDetailTypePolicy,
						pickupcodeSettingRedisService,
						adminNormalOrderDetailService,
						companySceneSmsSendPort,
						orderZitiQrCodeRedisService,
						companysRedisTemplate);

		assertTrue(svc.getOrderPickupCode(38L, 261L, "100"));

		ArgumentCaptor<String> codeCap = ArgumentCaptor.forClass(String.class);
		verify(orderZitiQrCodeRedisService)
				.saveZitiCodeOrderMapping(codeCap.capture(), eq(100L), eq(Duration.ofSeconds(1800)));
		String twelve = codeCap.getValue();
		assertTrue(twelve.startsWith("760773"));
		assertTrue(twelve.length() >= 12);

		verify(valueOps).set(eq("admin-pickupcode:100|13761924721"), eq(twelve), eq(Duration.ofSeconds(1800)));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, String>> varsCap = ArgumentCaptor.forClass(Map.class);
		verify(companySceneSmsSendPort)
				.sendSceneTemplatedSms(eq(38L), eq("13761924721"), eq("order_pickup"), varsCap.capture());
		assertTrue(twelve.equals(varsCap.getValue().get("提货码")));
	}
}
