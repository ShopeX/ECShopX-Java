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

package cn.shopex.ecshopx.members.mapper;

import cn.shopex.ecshopx.members.domain.WechatTags;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WechatTagsMapper extends BaseMapper<WechatTags> {

	@Select(
			"""
			SELECT t.tag_id AS tag_id, t.tag_name AS tag_name, COUNT(b.open_id) AS total
			FROM wechat_tags t
			LEFT JOIN wechatfans_bind_wechattag b
			  ON t.tag_id = b.tag_id
			 AND t.company_id = b.company_id
			 AND t.authorizer_appid = b.authorizer_appid
			WHERE t.authorizer_appid = #{authorizerAppid}
			  AND t.company_id = #{companyId}
			GROUP BY t.tag_id
			""")
	List<Map<String, Object>> selectTagsWithBindCount(
			@Param("authorizerAppid") String authorizerAppid, @Param("companyId") Long companyId);
}
