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

package cn.shopex.ecshopx.orders.service.payment;

import com.github.binarywang.wxpay.bean.request.WxPayMicropayRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.result.WxPayMicropayResult;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderResult;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;

/**
 * Thin wrapper around Binary Wang {@code weixin-java-pay} v2 calls used by order checkout (unifiedorder / micropay).
 * Each invocation uses a fresh {@link WxPayServiceImpl} because {@code WxPayConfig} is per-merchant request state.
 */
final class OrdersExternalWxPayV2NativeClient {

	private OrdersExternalWxPayV2NativeClient() {}

	static WxPayConfig v2Md5Config(String mchKey) {
		WxPayConfig config = new WxPayConfig();
		config.setMchKey(mchKey);
		config.setSignType("MD5");
		return config;
	}

	static WxPayUnifiedOrderResult unifiedOrder(WxPayConfig config, WxPayUnifiedOrderRequest request) throws WxPayException {
		WxPayServiceImpl wx = new WxPayServiceImpl();
		wx.setConfig(config);
		return wx.unifiedOrder(request);
	}

	static WxPayMicropayResult micropay(WxPayConfig config, WxPayMicropayRequest request) throws WxPayException {
		WxPayServiceImpl wx = new WxPayServiceImpl();
		wx.setConfig(config);
		return wx.micropay(request);
	}
}
