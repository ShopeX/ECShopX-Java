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

package cn.shopex.ecshopx.common.port.digios;

/**
 * POS 门店账号新建前同步 DigiOS {@code account.upsert}。
 *
 * <p>DigiOS 未开启：no-op。开启时失败须抛 {@link cn.shopex.ecshopx.common.exception.BadRequestException}，
 * 由调用方阻断本地创建。
 */
public interface PosStoreAccountDigiOsCreatePort {

	void ensureSyncedOnCreate(
			long companyId, String employeeNo, String name, String mobile, String email);
}
