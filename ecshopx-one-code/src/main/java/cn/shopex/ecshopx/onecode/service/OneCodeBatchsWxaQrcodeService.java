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

package cn.shopex.ecshopx.onecode.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import org.springframework.stereotype.Service;

@Service
public class OneCodeBatchsWxaQrcodeService {

	private static final String TEMPLATE_YYKWEISHOP = "yykweishop";

	private static final String PAGE_ONECODE = "pages/onecode";

	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;

	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;

	public OneCodeBatchsWxaQrcodeService(
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient) {
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
	}

	public byte[] buildJpegBytes(long companyId, String batchId, String num) {
		String appid =
				weappAuthorizerAppidRepository
						.findAuthorizerAppid(companyId, TEMPLATE_YYKWEISHOP)
						.orElseThrow(() -> new ResourceException("没有开通此小程序，不能下载."));
		String scene = "id=" + batchId + "&num=" + num;
		return wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(appid, scene, PAGE_ONECODE);
	}
}
