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

import cn.shopex.ecshopx.common.exception.BadRequestException;
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
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PaymentSettingSaveService {

	private static final String PT_WXPAY = "wxpay";
	private static final String PT_ALIPAY = "alipay";
	private static final String PT_PAYPAL = "paypal";
	private static final String PT_POINT_PAY = "point_pay";
	private static final String PT_HFPAY = "hfpay";
	private static final String PT_ADAPAY = "adapay";
	private static final String PT_OFFLINE = "offline_pay";
	private static final String PT_CHINAUMS = "chinaumspay";
	private static final String PT_BSPAY = "bspay";
	private static final String PT_ICBC = "icbcpay";
	private static final String PT_DOUMEN_INTL = "doumen_intl";

	private final PointMemberRuleSaveService pointMemberRuleSaveService;
	private final WxpayPaymentSettingWriter wxpayPaymentSettingWriter;
	private final AlipayPaymentSettingWriter alipayPaymentSettingWriter;
	private final PaypalPaymentSettingWriter paypalPaymentSettingWriter;
	private final HfPayPaymentSettingWriter hfPayPaymentSettingWriter;
	private final AdapayPaymentSettingWriter adapayPaymentSettingWriter;
	private final OfflinePayPaymentSettingWriter offlinePayPaymentSettingWriter;
	private final ChinaumsPaymentSettingWriter chinaumsPaymentSettingWriter;
	private final BsPayPaymentSettingWriter bsPayPaymentSettingWriter;
	private final IcbcPayPaymentSettingWriter icbcPayPaymentSettingWriter;
	private final DoumenIntlPaymentSettingWriter doumenIntlPaymentSettingWriter;
	private final DoumenIntlMutualExclusionService doumenIntlMutualExclusionService;

	public PaymentSettingSaveService(
			PointMemberRuleSaveService pointMemberRuleSaveService,
			WxpayPaymentSettingWriter wxpayPaymentSettingWriter,
			AlipayPaymentSettingWriter alipayPaymentSettingWriter,
			PaypalPaymentSettingWriter paypalPaymentSettingWriter,
			HfPayPaymentSettingWriter hfPayPaymentSettingWriter,
			AdapayPaymentSettingWriter adapayPaymentSettingWriter,
			OfflinePayPaymentSettingWriter offlinePayPaymentSettingWriter,
			ChinaumsPaymentSettingWriter chinaumsPaymentSettingWriter,
			BsPayPaymentSettingWriter bsPayPaymentSettingWriter,
			IcbcPayPaymentSettingWriter icbcPayPaymentSettingWriter,
			DoumenIntlPaymentSettingWriter doumenIntlPaymentSettingWriter,
			DoumenIntlMutualExclusionService doumenIntlMutualExclusionService) {
		this.pointMemberRuleSaveService = pointMemberRuleSaveService;
		this.wxpayPaymentSettingWriter = wxpayPaymentSettingWriter;
		this.alipayPaymentSettingWriter = alipayPaymentSettingWriter;
		this.paypalPaymentSettingWriter = paypalPaymentSettingWriter;
		this.hfPayPaymentSettingWriter = hfPayPaymentSettingWriter;
		this.adapayPaymentSettingWriter = adapayPaymentSettingWriter;
		this.offlinePayPaymentSettingWriter = offlinePayPaymentSettingWriter;
		this.chinaumsPaymentSettingWriter = chinaumsPaymentSettingWriter;
		this.bsPayPaymentSettingWriter = bsPayPaymentSettingWriter;
		this.icbcPayPaymentSettingWriter = icbcPayPaymentSettingWriter;
		this.doumenIntlPaymentSettingWriter = doumenIntlPaymentSettingWriter;
		this.doumenIntlMutualExclusionService = doumenIntlMutualExclusionService;
	}

	public Object setPaymentSetting(PaymentSettingCommand cmd) {
		String payType = cmd.payType();
		if (payType == null || payType.trim().isEmpty()) {
			throw new BadRequestException("暂时不支持");
		}
		payType = payType.trim();

		boolean isOpening =
				DoumenIntlMutualExclusionService.resolveIsOpening(
						payType, cmd.scalarFields().get("is_open"));
		doumenIntlMutualExclusionService.validateBeforeSave(cmd.companyId(), payType, isOpening);

		if (PT_POINT_PAY.equals(payType)) {
			Object raw = cmd.scalarFields().get("point_pay_first");
			int pointPayFirst = narrowIntFromScalar(raw);
			return pointMemberRuleSaveService.savePointPayFirstOnly(cmd.companyId(), pointPayFirst);
		}

		if (PT_WXPAY.equals(payType)) {
			wxpayPaymentSettingWriter.write(cmd);
		} else if (PT_ALIPAY.equals(payType)) {
			alipayPaymentSettingWriter.write(cmd);
		} else if (PT_PAYPAL.equals(payType)) {
			paypalPaymentSettingWriter.write(cmd);
		} else if (PT_HFPAY.equals(payType)) {
			hfPayPaymentSettingWriter.write(cmd);
		} else if (PT_ADAPAY.equals(payType)) {
			adapayPaymentSettingWriter.write(cmd);
		} else if (PT_OFFLINE.equals(payType)) {
			offlinePayPaymentSettingWriter.write(cmd);
		} else if (PT_CHINAUMS.equals(payType)) {
			chinaumsPaymentSettingWriter.write(cmd);
		} else if (PT_BSPAY.equals(payType)) {
			bsPayPaymentSettingWriter.write(cmd);
		} else if (PT_ICBC.equals(payType)) {
			icbcPayPaymentSettingWriter.write(cmd);
		} else if (PT_DOUMEN_INTL.equals(payType)) {
			doumenIntlPaymentSettingWriter.write(cmd);
			if (isOpening) {
				doumenIntlMutualExclusionService.closeAllOtherPaymentMethods(cmd.companyId());
			}
		} else {
			throw new BadRequestException("暂时不支持");
		}

		return Map.of("status", Boolean.TRUE);
	}

	private static int narrowIntFromScalar(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (raw instanceof Number n) {
			return (int) n.doubleValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0;
		}
		int i = 0;
		int len = s.length();
		while (i < len && Character.isWhitespace(s.charAt(i))) {
			i++;
		}
		if (i >= len) {
			return 0;
		}
		int sign = 1;
		char c0 = s.charAt(i);
		if (c0 == '+' || c0 == '-') {
			if (c0 == '-') {
				sign = -1;
			}
			i++;
		}
		if (i >= len) {
			return 0;
		}
		long acc = 0;
		boolean any = false;
		while (i < len) {
			char c = s.charAt(i);
			if (c < '0' || c > '9') {
				break;
			}
			any = true;
			acc = acc * 10 + (c - '0');
			i++;
		}
		if (!any) {
			return 0;
		}
		return (int) (sign * acc);
	}
}
