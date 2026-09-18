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

/**
 * 活动转发统计块（等价 PHP {@code SalespersonActiveArticleRecordStatisticsJob::handle}），由
 * promotions 模块实现以打破 companys↔promotions Maven 依赖环。
 */
public interface SalespersonActiveArticleRecordRunner {

	/**
	 * @param yesterdayYmd 昨日 {@code Ymd}（Asia/Shanghai）
	 * @return 导购行数 + 实际 INSERT {@code salesperson_active_article_statistics} 条数之和
	 */
	int runActiveArticleBlock(int yesterdayYmd);
}
