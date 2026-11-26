package cn.shopex.ecshopx.aftersales.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeliveryStaffAftersalesAggregatesSqlTest {

	private static final Pattern SELECT_BLOCK = Pattern.compile(
			"<select\\s+id=\"selectDeliveryStaffAftersalesAggregates\"[\\s\\S]*?</select>",
			Pattern.CASE_INSENSITIVE);

	@Test
	@DisplayName("配送员售后聚合：退款按 aftersales_bn 预聚合且售后单去重计数")
	void selectDeliveryStaffAftersalesAggregates_avoidsRefundJoinAmplification() throws IOException {
		String xml = readMapperXml();
		Matcher m = SELECT_BLOCK.matcher(xml);
		assertThat(m.find()).as("selectDeliveryStaffAftersalesAggregates 应存在").isTrue();
		String block = m.group();

		assertThat(block).containsIgnoringCase("GROUP BY aftersales_bn");
		assertThat(block).containsIgnoringCase("COUNT(DISTINCT");
		assertThat(block).containsIgnoringCase("aftersales_bn");
		assertThat(block)
				.doesNotContainPattern(
						"(?is)LEFT\\s+JOIN\\s+aftersales_refund\\s+ON\\s+aftersales\\.aftersales_bn\\s*=\\s*aftersales_refund\\.aftersales_bn");
	}

	private static String readMapperXml() throws IOException {
		try (InputStream in =
				DeliveryStaffAftersalesAggregatesSqlTest.class.getClassLoader()
						.getResourceAsStream("mapper/AftersalesMapper.xml")) {
			assertThat(in).as("classpath:mapper/AftersalesMapper.xml").isNotNull();
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
