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

package cn.shopex.ecshopx.common.operatorcart;

/**
 * 运营端扫条码：按企业、店铺与条码解析 item_id，并校验商品属于该企业。
 */
public interface OperatorCartScanBarcodeFacade {

	/**
	 * 按企业、店铺与条码解析 item_id，并校验商品属于该企业。
	 *
	 * @param companyId JWT 企业 ID
	 * @param distributorId 店铺 ID，0 表示平台池
	 * @param barcode 条码字符串；调用方已做缺省与数字 0 等弱类型归一
	 * @return 可售校验前的 item_id（加购侧库存/店铺 SKU 仍由 OperatorCartAddDataService 内 Facade 完成）
	 */
	long resolveItemIdForOperatorScan(long companyId, long distributorId, String barcode);
}
