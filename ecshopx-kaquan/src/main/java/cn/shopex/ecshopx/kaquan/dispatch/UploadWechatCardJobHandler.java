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

package cn.shopex.ecshopx.kaquan.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.kaquan.service.discount.UploadWechatCardWorker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UploadWechatCardJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(UploadWechatCardJobHandler.class);

	private final UploadWechatCardWorker uploadWechatCardWorker;

	public UploadWechatCardJobHandler(UploadWechatCardWorker uploadWechatCardWorker) {
		this.uploadWechatCardWorker = uploadWechatCardWorker;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		try {
			String authorizerAppId = extractAuthorizerAppId(payload);
			long companyId = extractCompanyId(payload);
			List<Long> cardIds = extractCardIds(payload);
			uploadWechatCardWorker.execute(authorizerAppId, companyId, cardIds);
		} catch (RuntimeException e) {
			log.debug("UploadWechatCardJob failed: {}", e.toString());
		}
	}

	private static String extractAuthorizerAppId(Map<String, Object> payload) {
		Object raw = payload.get("authorizer_appid");
		return raw == null ? null : String.valueOf(raw);
	}

	private static long extractCompanyId(Map<String, Object> payload) {
		Object raw = payload.get("company_id");
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static List<Long> extractCardIds(Map<String, Object> payload) {
		Object raw = payload.get("card_ids");
		if (raw == null) {
			return List.of();
		}
		if (!(raw instanceof List<?> list)) {
			throw new BadRequestException("card_ids must be a list");
		}
		List<Long> out = new ArrayList<>(list.size());
		for (Object o : list) {
			if (o instanceof Number n) {
				out.add(n.longValue());
			} else if (o != null) {
				out.add(Long.parseLong(String.valueOf(o).trim()));
			}
		}
		return out;
	}
}
