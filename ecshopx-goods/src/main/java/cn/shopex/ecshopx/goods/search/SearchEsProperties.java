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

package cn.shopex.ecshopx.goods.search;

import cn.shopex.ecshopx.goods.search.index.GoodsSearchIndexNames;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 前台商品搜索 Elasticsearch 连接与排序权重配置（{@code ecshopx.search.es.*}）。 */
@Data
@ConfigurationProperties(prefix = "ecshopx.search.es")
public class SearchEsProperties {

	private boolean enabled = false;

	private boolean likeFallback = false;

	private List<String> hosts = defaultHosts();

	private String alias = GoodsSearchIndexNames.DEFAULT_ALIAS;

	private int connectTimeoutMs = 1000;

	private int socketTimeoutMs = 1500;

	private Rank rank = new Rank();

	public String getAliasName() {
		return alias;
	}

	private static List<String> defaultHosts() {
		List<String> hosts = new ArrayList<>();
		hosts.add("http://127.0.0.1:9200");
		return hosts;
	}

	/**
	 * 历史权重配置；默认综合排序已改为分层字段 sort（有货 → 相关度 → 后台 sort → 销量 → 上架时间），
	 * 这些权重不再参与打分，保留仅为兼容既有配置项。
	 */
	@Data
	public static class Rank {

		private double hasStockWeight = 10.0;

		private double salesWeight = 1.0;

		private double listedAtWeight = 1.0;
	}
}
