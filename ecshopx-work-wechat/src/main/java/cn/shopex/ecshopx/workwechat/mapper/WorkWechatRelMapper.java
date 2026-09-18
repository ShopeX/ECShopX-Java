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

package cn.shopex.ecshopx.workwechat.mapper;

import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorkWechatRelMapper extends BaseMapper<WorkWechatRel> {

	@Select("<script>"
			+ "SELECT salesperson_id AS salespersonId, COUNT(1) AS cnt FROM work_wechat_rel WHERE salesperson_id IN "
			+ "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
			+ "GROUP BY salesperson_id"
			+ "</script>")
	List<Map<String, Object>> countRowsGroupBySalespersonId(@Param("ids") List<Long> salespersonIds);
}
