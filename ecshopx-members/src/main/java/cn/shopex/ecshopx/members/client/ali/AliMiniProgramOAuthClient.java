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

package cn.shopex.ecshopx.members.client.ali;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.config.H5AliProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 支付宝小程序 OAuth（授权码换 user_id）。完整实现需 RSA 签名与支付宝网关协议，当前在缺少密钥配置时按业务失败处理。
 */
@Component
public class AliMiniProgramOAuthClient {

	private final H5AliProperties h5AliProperties;

	public AliMiniProgramOAuthClient(H5AliProperties h5AliProperties) {
		this.h5AliProperties = h5AliProperties;
	}

	public String exchangeCodeForUserId(long companyId, String code) {
		if (!StringUtils.hasText(code)) {
			throw new ResourceException("缺少参数！");
		}
		String appId = h5AliProperties.getMiniAppIdByCompanyId().get(String.valueOf(companyId));
		String pk = h5AliProperties.getAppPrivateKeyByCompanyId().get(String.valueOf(companyId));
		if (!StringUtils.hasText(appId) || !StringUtils.hasText(pk)) {
			throw new ResourceException("小程序授权信息错误，请联系服务商！");
		}
		throw new ResourceException("小程序授权信息错误，请联系服务商！");
	}
}
