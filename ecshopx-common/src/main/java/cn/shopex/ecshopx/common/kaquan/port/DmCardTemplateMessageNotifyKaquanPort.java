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

package cn.shopex.ecshopx.common.kaquan.port;

import java.util.Map;

/**
 * Kaquan-side entry for DM card-template webhook payloads (merged query + body).
 */
public interface DmCardTemplateMessageNotifyKaquanPort {

	/**
	 * Builds standard-card {@code postdata} from the merged webhook input, then runs create or update for
	 * card-template sync topics (envelope {@code topic} is normalized upstream).
	 *
	 * @param companyId tenant from URL path (authoritative over body)
	 * @param mergedRequestInput merged query string + JSON body (outer envelope may include {@code topic},
	 *     {@code content}, etc.)
	 * @param authorizerAppid optional app id for downstream integrations; may be blank when not configured
	 * @return snake_case card row plus operational fields (must expose numeric {@code card_id} on success)
	 */
	Map<String, Object> handle(long companyId, Map<String, Object> mergedRequestInput, String authorizerAppid);
}
