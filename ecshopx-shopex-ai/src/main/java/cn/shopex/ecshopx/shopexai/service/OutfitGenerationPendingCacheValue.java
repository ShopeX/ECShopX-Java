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

package cn.shopex.ecshopx.shopexai.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OutfitGenerationPendingCacheValue {

	private static final DateTimeFormatter PHP_STYLE =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@JsonProperty("status")
	private String status;

	@JsonProperty("created_at")
	private String createdAt;

	@JsonProperty("job_completed")
	private boolean jobCompleted;

	public static OutfitGenerationPendingCacheValue pendingNow() {
		OutfitGenerationPendingCacheValue v = new OutfitGenerationPendingCacheValue();
		v.setStatus("pending");
		v.setCreatedAt(formatNow());
		v.setJobCompleted(false);
		return v;
	}

	public static String formatNow() {
		return LocalDateTime.now().format(PHP_STYLE);
	}
}
