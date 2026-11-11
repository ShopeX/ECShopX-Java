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

/** 商品搜索索引全量重建 Dispatch 入队。 */
public interface SearchRebuildJobDispatchPublisher {

	/** 入队全公司蓝绿重建（payload {@code all_companies=true}）。 */
	void enqueueAllCompanies();

	/** 入队单公司重建（payload {@code company_id}）。 */
	void enqueueCompany(long companyId);
}
