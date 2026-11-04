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

package cn.shopex.ecshopx.payment.service.admin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.payment.service.dto.PaymentSettingCommand;
import cn.shopex.ecshopx.payment.service.settings.AdapayPaymentSettingWriter;
import cn.shopex.ecshopx.payment.service.settings.AlipayPaymentSettingWriter;
import cn.shopex.ecshopx.payment.service.settings.BsPayPaymentSettingWriter;
import cn.shopex.ecshopx.payment.service.settings.ChinaumsPaymentSettingWriter;
import cn.shopex.ecshopx.payment.service.settings.DoumenIntlPaymentSettingWriter;
import cn.shopex.ecshopx.payment.service.settings.HfPayPaymentSettingWriter;
import cn.shopex.ecshopx.payment.service.settings.IcbcPayPaymentSettingWriter;
import cn.shopex.ecshopx.payment.service.settings.OfflinePayPaymentSettingWriter;
import cn.shopex.ecshopx.payment.service.settings.PaypalPaymentSettingWriter;
import cn.shopex.ecshopx.payment.service.settings.WxpayPaymentSettingWriter;
import cn.shopex.ecshopx.point.service.PointMemberRuleSaveService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentSettingSaveServiceAccessTest {

	@Mock
	private PaymentSettingDistributorAccessGuard accessGuard;

	@Mock
	private PointMemberRuleSaveService pointMemberRuleSaveService;

	@Mock
	private WxpayPaymentSettingWriter wxpayPaymentSettingWriter;

	@Mock
	private AlipayPaymentSettingWriter alipayPaymentSettingWriter;

	@Mock
	private PaypalPaymentSettingWriter paypalPaymentSettingWriter;

	@Mock
	private HfPayPaymentSettingWriter hfPayPaymentSettingWriter;

	@Mock
	private AdapayPaymentSettingWriter adapayPaymentSettingWriter;

	@Mock
	private OfflinePayPaymentSettingWriter offlinePayPaymentSettingWriter;

	@Mock
	private ChinaumsPaymentSettingWriter chinaumsPaymentSettingWriter;

	@Mock
	private BsPayPaymentSettingWriter bsPayPaymentSettingWriter;

	@Mock
	private IcbcPayPaymentSettingWriter icbcPayPaymentSettingWriter;

	@Mock
	private DoumenIntlPaymentSettingWriter doumenIntlPaymentSettingWriter;

	@Mock
	private DoumenIntlMutualExclusionService doumenIntlMutualExclusionService;

	private PaymentSettingSaveService service;

	@BeforeEach
	void setUp() {
		service =
				new PaymentSettingSaveService(
						accessGuard,
						pointMemberRuleSaveService,
						wxpayPaymentSettingWriter,
						alipayPaymentSettingWriter,
						paypalPaymentSettingWriter,
						hfPayPaymentSettingWriter,
						adapayPaymentSettingWriter,
						offlinePayPaymentSettingWriter,
						chinaumsPaymentSettingWriter,
						bsPayPaymentSettingWriter,
						icbcPayPaymentSettingWriter,
						doumenIntlPaymentSettingWriter,
						doumenIntlMutualExclusionService);
	}

	@Test
	void setPaymentSetting_rejectsUnauthorizedDistributor_beforeWriters() {
		when(accessGuard.mayRead(eq(10L), eq(99L), eq("admin"), eq(0L))).thenReturn(false);
		LinkedHashMap<String, Object> scalar = new LinkedHashMap<>();
		scalar.put("is_open", "true");
		PaymentSettingCommand cmd =
				new PaymentSettingCommand(10L, 99L, "admin", 1L, "wxpay", scalar, Map.of());

		assertThatThrownBy(() -> service.setPaymentSetting(cmd))
				.isInstanceOf(ForbiddenException.class);

		verify(wxpayPaymentSettingWriter, never()).write(any());
		verify(doumenIntlMutualExclusionService, never())
				.validateBeforeSave(anyLong(), anyString(), anyBoolean());
	}
}
