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
 * 多语言字典库
 */
@Data
@MpTable(value = "multi_lang_config", comment = "多语言字典库", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class MultiLangConfig {

	/** id */
	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
	private Long id;

	/** 公司id */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司id")
	private Long companyId;

	/** 表名 */
	@MpField(value = "table_name", columnType = "string", comment = "表名")
	private String tableName = "";

	/** field,字段名 */
	@MpField(value = "`field`", columnType = "string", comment = "field,字段名")
	private String field = "";

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
