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

package cn.shopex.ecshopx.goods.service.recommend;

import java.util.List;
import java.util.Map;

/** 主商品占用冲突（SSOT §4.3） */
public class GoodsRecommendMainItemConflictException extends RuntimeException {

	private final String errorCode;

	private final List<Map<String, Object>> conflicts;

	public GoodsRecommendMainItemConflictException(
			String message, String errorCode, List<Map<String, Object>> conflicts) {
		super(message);
		this.errorCode = errorCode;
		this.conflicts = conflicts;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public List<Map<String, Object>> getConflicts() {
		return conflicts;
	}
}
