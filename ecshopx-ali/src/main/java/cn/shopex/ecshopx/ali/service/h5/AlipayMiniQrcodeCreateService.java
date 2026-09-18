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

package cn.shopex.ecshopx.ali.service.h5;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.alipay.easysdk.base.qrcode.models.AlipayOpenAppQrcodeCreateResponse;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import org.springframework.stereotype.Service;

@Service
public class AlipayMiniQrcodeCreateService {

	private static final Object SDK_LOCK = new Object();

	public AlipayOpenAppQrcodeCreateResponse create(String page, String scene, Config config) {
		synchronized (SDK_LOCK) {
			try {
				Factory.setOptions(config);
				return Factory.Base.Qrcode().create(page, scene, "支付宝小程序码");
			} catch (Exception e) {
				String msg = e.getMessage();
				throw new BadRequestException(msg != null && !msg.isEmpty() ? msg : "支付宝小程序码创建失败");
			}
		}
	}
}
