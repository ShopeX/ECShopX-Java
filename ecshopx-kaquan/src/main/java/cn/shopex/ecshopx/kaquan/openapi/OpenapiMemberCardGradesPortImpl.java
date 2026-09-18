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

package cn.shopex.ecshopx.kaquan.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardGradesFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardGradesPort;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeSimpleListQueryService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpenapiMemberCardGradesPortImpl implements OpenapiMemberCardGradesPort {

	private static final Logger log = LoggerFactory.getLogger(OpenapiMemberCardGradesPortImpl.class);

	private final MemberCardGradeSimpleListQueryService memberCardGradeSimpleListQueryService;

	public OpenapiMemberCardGradesPortImpl(
			MemberCardGradeSimpleListQueryService memberCardGradeSimpleListQueryService) {
		this.memberCardGradeSimpleListQueryService = memberCardGradeSimpleListQueryService;
	}

	@Override
	public List<Map<String, Object>> getCompanyGradeSimpleList(long companyId, String lang) {
		try {
			return memberCardGradeSimpleListQueryService.getCompanyGradeSimpleList(companyId, lang);
		} catch (Exception e) {
			log.error("获取会员卡等级列表失败::{}::companyId={}", e.getMessage(), companyId, e);
			throw new OpenapiMemberCardGradesFailException(
					"获取会员卡等级列表失败：" + e.getMessage());
		}
	}
}
