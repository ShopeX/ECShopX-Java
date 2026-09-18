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

import cn.shopex.ecshopx.members.domain.WechatUsers;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WechatUsersMapper extends BaseMapper<WechatUsers> {

	@Select("<script>"
			+ "SELECT a.user_id AS userId, w.nickname AS nickname FROM members_associations a "
			+ "INNER JOIN members_wechatusers w ON a.company_id = w.company_id AND a.unionid = w.unionid "
			+ "WHERE a.company_id = #{companyId} AND a.user_type = 'wechat' AND a.user_id IN "
			+ "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>"
			+ "</script>")
	List<Map<String, Object>> selectWechatNicknameRowsByCompanyAndUserIds(@Param("companyId") long companyId,
			@Param("userIds") List<Long> userIds);
}
