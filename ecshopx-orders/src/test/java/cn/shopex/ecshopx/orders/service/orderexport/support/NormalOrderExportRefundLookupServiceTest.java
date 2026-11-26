package cn.shopex.ecshopx.orders.service.orderexport.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class NormalOrderExportRefundLookupServiceTest {

	@Mock
	private NamedParameterJdbcTemplate jdbc;

	@Test
	@DisplayName("SQL 先按 aftersales_bn 聚合退款再 JOIN，避免明细字段被退款行数放大")
	void itemRefundSql_preAggregatesRefundsByAftersalesBn() {
		String sql = NormalOrderExportRefundLookupService.itemRefundSql();

		assertThat(sql).containsIgnoringCase("GROUP BY aftersales_bn");
		assertThat(sql).containsIgnoringCase("FROM aftersales_detail");
		assertThat(sql).doesNotContainPattern("(?is)FROM aftersales_detail\\s+A\\s+LEFT JOIN aftersales_refund\\s+C");
	}

	@Test
	@DisplayName("loadItemRefunds 按 order_id+item_id 映射退货数量与退款金额")
	void loadItemRefunds_mapsAggregatedRow() {
		when(jdbc.queryForList(eq(NormalOrderExportRefundLookupService.itemRefundSql()), any(MapSqlParameterSource.class)))
				.thenReturn(List.of(Map.of(
						"order_id", 100L,
						"item_id", 7L,
						"num", 2,
						"refunded_fee", 300,
						"refunded_point", 10,
						"refund_fee", 300,
						"refund_point", 10,
						"refund_success_time", 1_700_000_000)));

		NormalOrderExportRefundLookupService service = new NormalOrderExportRefundLookupService(jdbc);
		Map<String, NormalOrderExportRefundLookupService.RefundInfo> out = service.loadItemRefunds(1L, List.of(100L));

		assertThat(out).containsOnlyKeys("100_7");
		NormalOrderExportRefundLookupService.RefundInfo info = out.get("100_7");
		assertThat(info.num()).isEqualTo(2);
		assertThat(info.refundedFee()).isEqualTo(300);
		assertThat(info.refundedPoint()).isEqualTo(10);
		assertThat(info.refundFee()).isEqualTo(300);
		assertThat(info.refundPoint()).isEqualTo(10);
		assertThat(info.refundSuccessTime()).isEqualTo(1_700_000_000);
	}
}
