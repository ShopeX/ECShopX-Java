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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.kaquan.port.UploadWechatCardDispatchPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DiscountCardUploadWechatFacadeService {

	private static final ObjectMapper OM = new ObjectMapper();

	private final UploadWechatCardDispatchPublisher uploadWechatCardDispatchPublisher;

	public DiscountCardUploadWechatFacadeService(
			UploadWechatCardDispatchPublisher uploadWechatCardDispatchPublisher) {
		this.uploadWechatCardDispatchPublisher = uploadWechatCardDispatchPublisher;
	}

	/**
	 * 提交卡券同步微信的异步任务。
	 *
	 * @param authorizerAppId 公众号授权 appid，可为 null 或空白（操作员 JWT 中可能未携带）；异步执行时若缺有效授权，微信侧创建会失败或仅落库。
	 * @param companyId 企业 ID
	 * @param cardIds 待同步的卡券 ID 列表（调用方已校验非空）
	 */
	public void submitUploadToWechat(String authorizerAppId, long companyId, List<Long> cardIds) {
		uploadWechatCardDispatchPublisher.publish(authorizerAppId, companyId, cardIds);
	}

	public static List<Long> parseCardIdsFromMerged(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				long id = DiscountCardParamNormalize.longFromObject(o, 0L);
				if (id > 0L) {
					out.add(id);
				}
			}
			return out;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return List.of();
			}
			if (t.startsWith("[")) {
				try {
					List<Object> arr = OM.readValue(t, new TypeReference<List<Object>>() {});
					return parseCardIdsFromMerged(arr);
				} catch (Exception e) {
					return List.of();
				}
			}
			List<Long> out = new ArrayList<>();
			for (String p : t.split(",")) {
				long id = DiscountCardParamNormalize.longFromObject(p.trim(), 0L);
				if (id > 0L) {
					out.add(id);
				}
			}
			return out;
		}
		if (raw instanceof Number n) {
			long id = n.longValue();
			return id > 0L ? List.of(id) : List.of();
		}
		return List.of();
	}
}
