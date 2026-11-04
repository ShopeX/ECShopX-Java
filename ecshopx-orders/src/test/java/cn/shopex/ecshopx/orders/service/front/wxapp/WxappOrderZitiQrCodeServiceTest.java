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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.service.setting.PickupcodeSettingRedisService;
import cn.shopex.ecshopx.members.service.wxapp.MemberBarcodeGenerateService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.normal.OrderZitiQrCodeRedisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class WxappOrderZitiQrCodeServiceTest {

	@Mock private NormalOrdersMapper normalOrdersMapper;
	@Mock private OrderZitiQrCodeRedisService orderZitiQrCodeRedisService;
	@Mock private MemberBarcodeGenerateService memberBarcodeGenerateService;
	@Mock private PickupcodeSettingRedisService pickupcodeSettingRedisService;
	@Mock private AdminNormalOrderDetailService adminNormalOrderDetailService;
	@Mock private StringRedisTemplate companysRedisTemplate;
	@Mock private ValueOperations<String, String> valueOps;

	private WxappOrderZitiQrCodeService service;

	@BeforeEach
	void setUp() {
		service =
				new WxappOrderZitiQrCodeService(
						normalOrdersMapper,
						orderZitiQrCodeRedisService,
						memberBarcodeGenerateService,
						pickupcodeSettingRedisService,
						adminNormalOrderDetailService,
						companysRedisTemplate);
	}

	@Test
	@DisplayName("开启提货码时 pickup_code 回填 12 位 code，扫码 Redis TTL 为 1800s")
	void getZitiQRCode_pickupOn_mirrorsTwelveDigitCodeAndUses1800Ttl() {
		NormalOrders order = new NormalOrders();
		order.setOrderId(5420567000120261L);
		order.setUserId(261L);
		order.setZitiCode(760773L);
		order.setZitiStatus("PENDING");
		when(normalOrdersMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
		LinkedHashMap<String, String> images = new LinkedHashMap<>();
		images.put("barcode_url", "b");
		images.put("qrcode_url", "q");
		when(memberBarcodeGenerateService.generateBarcodeAndQrcodeDataUrlsForPlainContent(anyString()))
				.thenReturn(images);
		when(pickupcodeSettingRedisService.handle(eq(38L), eq(null)))
				.thenReturn(Map.of("pickupcode_status", true));

		Map<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("order_id", 5420567000120261L);
		orderInfo.put("mobile", "13761924721");
		when(adminNormalOrderDetailService.buildOrderBundle(eq(38L), eq("5420567000120261"), eq(false), eq("api")))
				.thenReturn(Map.of("orderInfo", orderInfo));
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOps);

		WxappOrderZitiQrCodeResult r = service.getZitiQRCode(38L, 261L, "normal", "5420567000120261");
		Map<String, Object> data = assertInstanceOf(Map.class, r.dataIfShown());
		String code = String.valueOf(data.get("code"));
		assertEquals(code, String.valueOf(data.get("pickup_code")));
		assertEquals(12, code.length());

		ArgumentCaptor<Duration> ttlCap = ArgumentCaptor.forClass(Duration.class);
		verify(orderZitiQrCodeRedisService)
				.saveZitiCodeOrderMapping(eq(code), eq(5420567000120261L), ttlCap.capture());
		assertEquals(Duration.ofSeconds(1800), ttlCap.getValue());
		verify(valueOps)
				.set(eq("admin-pickupcode:5420567000120261|13761924721"), eq(code), eq(Duration.ofSeconds(1800)));
	}

	@Test
	@DisplayName("关闭提货码时 pickup_code 为 null，仍签发 12 位扫码码")
	void getZitiQRCode_pickupOff_pickupCodeNull() {
		NormalOrders order = new NormalOrders();
		order.setOrderId(100L);
		order.setUserId(1L);
		order.setZitiCode(123456L);
		order.setZitiStatus("PENDING");
		when(normalOrdersMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
		LinkedHashMap<String, String> images = new LinkedHashMap<>();
		images.put("barcode_url", "b");
		images.put("qrcode_url", "q");
		when(memberBarcodeGenerateService.generateBarcodeAndQrcodeDataUrlsForPlainContent(anyString()))
				.thenReturn(images);
		when(pickupcodeSettingRedisService.handle(eq(1L), eq(null)))
				.thenReturn(Map.of("pickupcode_status", false));

		WxappOrderZitiQrCodeResult r = service.getZitiQRCode(1L, 1L, "normal", "100");
		Map<String, Object> data = assertInstanceOf(Map.class, r.dataIfShown());
		assertNull(data.get("pickup_code"));
		verify(orderZitiQrCodeRedisService).saveZitiCodeOrderMapping(anyString(), eq(100L), eq(Duration.ofSeconds(1800)));
	}
}
