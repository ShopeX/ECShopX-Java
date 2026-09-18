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

package cn.shopex.ecshopx.hfpay.service.enterapply;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouFile01Client;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HfpayHfFileFacadeService {

	private static final Set<String> OK_RESP = Set.of("C00000", "C00001", "C00002");

	private final HfpayAttachNoGenerator attachNoGenerator;
	private final HfPayPaymentSettingService paymentSettingService;
	private final HfPayAcouFile01Client acouFile01Client;

	public HfpayHfFileFacadeService(
			HfpayAttachNoGenerator attachNoGenerator,
			HfPayPaymentSettingService paymentSettingService,
			HfPayAcouFile01Client acouFile01Client) {
		this.attachNoGenerator = attachNoGenerator;
		this.paymentSettingService = paymentSettingService;
		this.acouFile01Client = acouFile01Client;
	}

	public Map<String, Object> upload(long companyId, Map<String, Object> mergedParams, MultipartFile file) {
		String attachNo = attachNoGenerator.nextAttachNo();
		Map<String, Object> setting = paymentSettingService.loadForCompany(companyId);
		Map<String, Object> apiResult = acouFile01Client.file01(
				setting,
				attachNo,
				mergedParams.get("trans_type") == null ? "" : String.valueOf(mergedParams.get("trans_type")).trim(),
				mergedParams.get("attach_type") == null ? "" : String.valueOf(mergedParams.get("attach_type")).trim(),
				file);

		Object codeObj = apiResult.get("resp_code");
		String respCode = codeObj == null ? "" : String.valueOf(codeObj).trim();
		if (!OK_RESP.contains(respCode)) {
			Object desc = apiResult.get("resp_desc");
			String msg = desc == null ? "汇付接口返回失败" : String.valueOf(desc);
			throw new ResourceException(msg);
		}
		Map<String, Object> out = new LinkedHashMap<>(apiResult);
		out.put("attach_no", attachNo);
		return out;
	}
}
