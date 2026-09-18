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

package cn.shopex.ecshopx.goods.service.items;

import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ItemsDetailVideoPicService {

	private static final Pattern HTTP = Pattern.compile("(https?://)", Pattern.CASE_INSENSITIVE);

	public void applyVideoUrl(Map<String, Object> detail, String authorizerAppId) {
		if (detail == null) {
			return;
		}
		Object v = detail.get("videos");
		String vs = v != null ? v.toString() : "";
		if (!vs.isEmpty() && HTTP.matcher(vs).find()) {
			detail.put("videos_url", vs);
			return;
		}
		if (!vs.isEmpty() && authorizerAppId != null && !authorizerAppId.isEmpty()) {
			detail.put("videos_url", "");
			detail.put("videos", "");
		} else {
			detail.put("videos_url", "");
		}
	}
}
