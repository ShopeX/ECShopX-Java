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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.adapay.domain.dto.AdapayMemberListRowDto;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberRelCorpListFilter;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberRelCorpListMapper;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class AdapayMemberListService {

	private final AdapayMemberRelCorpListMapper adapayMemberRelCorpListMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public AdapayMemberListService(
			AdapayMemberRelCorpListMapper adapayMemberRelCorpListMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.adapayMemberRelCorpListMapper = adapayMemberRelCorpListMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> lists(long companyId, AdapayMemberListQuery query, int datapassBlock) {
		AdapayMemberRelCorpListFilter f = new AdapayMemberRelCorpListFilter();
		f.setCompanyId(companyId);
		if (query.memberType() != null) {
			f.setMemberType(query.memberType());
		}
		if (query.operatorType() != null) {
			f.setOperatorType(query.operatorType());
		}
		if (query.keywords() != null) {
			f.setKeywordsPlain(query.keywords());
			f.setKeywordsEnc(sensitiveFieldEncryptor.encrypt(query.keywords()));
		}

		long total = adapayMemberRelCorpListMapper.countByFilter(f);
		int offset = (query.page() - 1) * query.pageSize();
		List<Map<String, Object>> rows =
				adapayMemberRelCorpListMapper.selectPageByFilter(f, offset, query.pageSize());

		if (!CollectionUtils.isEmpty(rows)) {
			for (Map<String, Object> row : rows) {
				Object un = row.get("user_name");
				if (un instanceof String us) {
					row.put("user_name", sensitiveFieldEncryptor.decrypt(us));
				}
				Object lp = row.get("legal_person");
				if (lp instanceof String lps) {
					row.put("legal_person", sensitiveFieldEncryptor.decrypt(lps));
				}
			}
		}

		if (datapassBlock == 1 && !CollectionUtils.isEmpty(rows)) {
			for (int i = 0; i < rows.size(); i++) {
				Map<String, Object> row = rows.get(i);
				Object lp = row.get("legal_person");
				if (lp instanceof String s && StringUtils.hasText(s)) {
					row.put("legal_person", DataMasking.maskTruename(s));
				}
				if (Objects.equals("person", row.get("member_type"))) {
					Object un = row.get("user_name");
					if (un instanceof String us && StringUtils.hasText(us)) {
						row.put("user_name", DataMasking.maskTruename(us));
					}
					row.put("legal_person", row.get("user_name"));
				}
			}
		}

		List<AdapayMemberListRowDto> listOut = new ArrayList<>(rows.size());
		for (Map<String, Object> row : rows) {
			listOut.add(AdapayMemberListRowDto.fromRow(row));
		}

		Map<String, Object> out = new LinkedHashMap<>(2);
		out.put("total_count", total);
		out.put("list", listOut);
		return out;
	}
}
