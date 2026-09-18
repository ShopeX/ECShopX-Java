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

/** C 端推荐场景（SSOT §3.4） */
public enum GoodsRecommendScene {
	DETAIL("detail"),
	CART("cart"),
	CHECKOUT("checkout"),
	ORDER_DETAIL("order_detail");

	private final String code;

	GoodsRecommendScene(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

	public static GoodsRecommendScene parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String normalized = raw.trim().toLowerCase();
		for (GoodsRecommendScene scene : values()) {
			if (scene.code.equals(normalized)) {
				return scene;
			}
		}
		return null;
	}
}
