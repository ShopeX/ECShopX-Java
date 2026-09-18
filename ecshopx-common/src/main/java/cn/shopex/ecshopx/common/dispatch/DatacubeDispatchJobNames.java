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

package cn.shopex.ecshopx.common.dispatch;

public final class DatacubeDispatchJobNames {

	public static final String GOODS_DATA_JOB = "job:164:DataCubeBundle\\Jobs\\GoodsDataJob";

	public static final String DISTRIBUTOR_DATA_JOB = "job:160:DataCubeBundle\\Jobs\\DistributorDataJob";

	public static final String GOODS_STATISTIC_JOB = "job:161:DataCubeBundle\\Jobs\\GoodsStatisticJob";

	public static final String MERCHANT_STATISTIC_JOB = "job:162:DataCubeBundle\\Jobs\\MerchantStatisticJob";

	public static final String COMPANY_STATISTIC_JOB = "job:163:DataCubeBundle\\Jobs\\StatisticJob";

	private DatacubeDispatchJobNames() {
	}
}
