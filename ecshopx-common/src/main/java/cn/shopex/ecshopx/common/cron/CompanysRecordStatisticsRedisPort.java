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

package cn.shopex.ecshopx.common.cron;

import java.util.Map;

/**
 * 与 PHP 定时任务 {@code recordStatistics} / {@code recordSalespersonStatistics} 中业务 Redis
 * 键（scard / hgetall / expireat）一致的最小端口；生产实现由 ecshopx-companys 提供。
 */
public interface CompanysRecordStatisticsRedisPort {

	/**
	 * 等价 app('redis')-&gt;scard。
	 */
	long scard(String key);

	/**
	 * 按秒级 Unix 时间设置过期；等价 app('redis')-&gt;expireat。
	 */
	void expireAt(String key, long epochSeconds);

	/**
	 * 等价 app('redis')-&gt;hgetall；无 hash 时返回空 Map。
	 */
	Map<String, String> hgetall(String key);

	/**
	 * 字符串 GET；与 PHP 单连接默认行为一致，键不存在时返回 {@code null}。
	 */
	String get(String key);
}
