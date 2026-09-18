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

package cn.shopex.ecshopx.goods.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 商品佣金配置表
 */
@Data
@MpTable(value = "items_commission", comment = "商品佣金配置表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_rel_id", columns = {"rel_id"}), @MpIndex(name = "ix_type", columns = {"type"})})
public class ItemsCommission {

	/** id */
	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
	private Long id;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	/** 关联ID */
	@MpField(value = "rel_id", columnType = "bigint", comment = "关联ID")
	private Long relId;

	/** 数据类型 goods:SPU,item:SKU */
	@MpField(value = "type", columnType = "string", nullable = true, comment = "数据类型 goods:SPU,item:SKU", defaultValue = "goods")
	private String type = "goods";

	/** 佣金计算方式 1:按照比例,2:按照填写金额 */
	@MpField(value = "commission_type", columnType = "string", nullable = true, comment = "佣金计算方式 1:按照比例,2:按照填写金额", defaultValue = "1")
	private String commissionType = "1";

	/** 佣金 */
	@MpField(value = "commission_conf", columnType = "json_array", nullable = true, comment = "佣金")
	private String commissionConf;
}
