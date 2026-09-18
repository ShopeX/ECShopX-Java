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

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WechatAuthorizerLookupMapper {

	@Select("SELECT company_id FROM wechat_authorization WHERE authorizer_appid = #{appid} LIMIT 1")
	Long selectCompanyIdByAuthorizerAppid(@Param("appid") String appid);

	@Select("SELECT authorizer_appsecret FROM wechat_authorization WHERE authorizer_appid = #{appid} LIMIT 1")
	String selectAuthorizerSecretByAppid(@Param("appid") String appid);

	@Select("SELECT authorizer_appid FROM wechat_authorization WHERE company_id = #{companyId} AND service_type_info = 2 LIMIT 1")
	String selectServiceAccountAppidByCompanyId(@Param("companyId") Long companyId);
}
