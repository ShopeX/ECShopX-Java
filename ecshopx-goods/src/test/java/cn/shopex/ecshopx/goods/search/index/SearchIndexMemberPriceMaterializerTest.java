/**
 * Copyright 2019-2026 ShopeX
 */

package cn.shopex.ecshopx.goods.search.index;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.promotions.support.MemberPriceColumnCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class SearchIndexMemberPriceMaterializerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void parsesDoubleEncodedMemberPriceJson() throws Exception {
		ObjectNode root = objectMapper.createObjectNode();
		ObjectNode grade = objectMapper.createObjectNode();
		grade.put("3", "1500");
		grade.put("4", "0");
		ObjectNode vip = objectMapper.createObjectNode();
		vip.put("8", "1400");
		root.set("grade", grade);
		root.set("vipGrade", vip);
		String stored = MemberPriceColumnCodec.encodeForStorage(objectMapper, root);

		SearchIndexMemberPriceMaterializer.Buckets buckets =
				SearchIndexMemberPriceMaterializer.fromStorageJson(objectMapper, stored);

		assertThat(buckets.grade()).containsEntry(3L, 1500L).doesNotContainKey(4L);
		assertThat(buckets.vipGrade()).containsEntry(8L, 1400L);
	}
}
