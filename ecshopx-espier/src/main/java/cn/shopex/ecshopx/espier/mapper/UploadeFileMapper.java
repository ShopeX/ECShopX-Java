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

package cn.shopex.ecshopx.espier.mapper;

import cn.shopex.ecshopx.espier.domain.UploadeFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UploadeFileMapper extends BaseMapper<UploadeFile> {

	@Update("UPDATE espier_uploadefile SET relation_id = #{relationId} WHERE id = #{id}")
	int updateRelationId(@Param("id") long id, @Param("relationId") long relationId);

	@Select("SELECT relation_id FROM espier_uploadefile WHERE id = #{id} LIMIT 1")
	Long selectRelationId(@Param("id") long id);
}
