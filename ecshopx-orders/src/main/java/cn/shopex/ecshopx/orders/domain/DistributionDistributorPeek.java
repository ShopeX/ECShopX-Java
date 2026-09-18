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

package cn.shopex.ecshopx.orders.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

@Data
@MpTable("distribution_distributor")
public class DistributionDistributorPeek {

	@MpId(value = "distributor_id", type = IdType.INPUT)
	private Long distributorId;

	@MpField("company_id")
	private Long companyId;

	@MpField("dealer_id")
	private Long dealerId;

	@MpField("merchant_id")
	private Long merchantId;

	@MpField("shop_code")
	private String shopCode;

	@MpField("is_valid")
	private String isValid;

	@MpField("name")
	private String name;

	@MpField("distributor_self")
	private Integer distributorSelf;

	@MpField("kuaizhen_store_id")
	private Long kuaizhenStoreId;

	@MpField("payment_subject")
	private Integer paymentSubject;

	@MpField("created")
	private Integer created;

	/** 分账比例 JSON，见 {@code Distributor#splitLedgerInfo} */
	@MpField("split_ledger_info")
	private String splitLedgerInfo;
}
