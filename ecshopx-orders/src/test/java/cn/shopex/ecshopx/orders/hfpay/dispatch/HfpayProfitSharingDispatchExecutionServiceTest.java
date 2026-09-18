package cn.shopex.ecshopx.orders.hfpay.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.hfpay.service.profit.HfpayProfitSplitConfirmService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderProfitSharing;
import cn.shopex.ecshopx.orders.domain.OrderProfitSharingDetails;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitSharingDetailsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitSharingMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HfpayProfitSharingDispatchExecutionServiceTest {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderProfitSharing.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderProfitSharingDetails.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
	}

	@Mock
	private OrderProfitSharingMapper orderProfitSharingMapper;
	@Mock
	private OrderProfitSharingDetailsMapper orderProfitSharingDetailsMapper;
	@Mock
	private NormalOrdersMapper normalOrdersMapper;
	@Mock
	private TradeMapper tradeMapper;
	@Mock
	private HfpayProfitSplitConfirmService hfpayProfitSplitConfirmService;

	private HfpayProfitSharingDispatchExecutionService executionService;

	@BeforeEach
	void setUp() {
		executionService = new HfpayProfitSharingDispatchExecutionService(
				orderProfitSharingMapper,
				orderProfitSharingDetailsMapper,
				normalOrdersMapper,
				tradeMapper,
				hfpayProfitSplitConfirmService,
				OBJECT_MAPPER);
	}

	@Test
	@DisplayName("E3.2：分账主表 status==1 已成功则跳过，不调用 pay006")
	void e3_2_skips_when_already_success() {
		OrderProfitSharing row = new OrderProfitSharing();
		row.setOrderProfitSharingId(9L);
		row.setStatus(1);
		row.setCompanyId(1L);
		row.setOrderId(100L);
		when(orderProfitSharingMapper.selectById(9L)).thenReturn(row);

		executionService.executeProfitSharing(100L, List.of(9L));

		verify(hfpayProfitSplitConfirmService, never())
				.pay006(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("E3.6+E3.7：pay006 返回 C00000 时回写 status=1 与汇付单号（mock HfpayProfitSplitConfirmService）")
	void e3_6_e3_7_pay006_success_updates_sharing() {
		OrderProfitSharing data = new OrderProfitSharing();
		data.setOrderProfitSharingId(11L);
		data.setCompanyId(7L);
		data.setOrderId(200L);
		data.setStatus(0);
		data.setTotalFee(500);
		when(orderProfitSharingMapper.selectById(11L)).thenReturn(data);

		OrderProfitSharingDetails det = new OrderProfitSharingDetails();
		det.setSharingId(11L);
		det.setTotalFee(500);
		det.setChannelId("u1");
		det.setChannelAcctId("a1");
		when(orderProfitSharingDetailsMapper.selectList(any())).thenReturn(List.of(det));

		NormalOrders ord = new NormalOrders();
		ord.setOrderId(200L);
		ord.setCreateTime(1700000000);
		when(normalOrdersMapper.selectById(200L)).thenReturn(ord);

		Trade tr = new Trade();
		tr.setTradeId("T-1");
		tr.setTradeState("SUCCESS");
		when(tradeMapper.selectOne(any())).thenReturn(tr);

		Map<String, Object> payRes = new LinkedHashMap<>();
		payRes.put("resp_code", "C00000");
		payRes.put("order_id", "OID-9");
		payRes.put("order_date", "20240101");
		payRes.put("resp_desc", "ok");
		when(hfpayProfitSplitConfirmService.pay006(anyLong(), anyString(), anyString(), eq("27"), anyString(), anyString()))
				.thenReturn(payRes);
		when(hfpayProfitSplitConfirmService.extractRespCode(any())).thenReturn("C00000");

		executionService.executeProfitSharing(200L, List.of(11L));

		verify(orderProfitSharingMapper).update(isNull(), any());
		verify(hfpayProfitSplitConfirmService).pay006(anyLong(), anyString(), anyString(), eq("27"), anyString(), anyString());
	}

	@Test
	@DisplayName("E1：order_profit_sharing_ids 空列表入口早退，不查主表、不调 pay006")
	void e1_empty_ids_returns_before_mapper() {
		executionService.executeProfitSharing(1L, List.of());

		verify(orderProfitSharingMapper, never()).selectById(anyLong());
		verify(hfpayProfitSplitConfirmService, never())
				.pay006(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("E2：order_profit_sharing_id≤0 跳过，不查主表、不调 pay006")
	void e2_non_positive_id_skipped() {
		executionService.executeProfitSharing(1L, List.of(0L, -3L));

		verify(orderProfitSharingMapper, never()).selectById(anyLong());
		verify(hfpayProfitSplitConfirmService, never())
				.pay006(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("E3.1：主表 selectById 无记录则跳过，不调 pay006")
	void e3_1_missing_sharing_row_skips() {
		when(orderProfitSharingMapper.selectById(21L)).thenReturn(null);

		executionService.executeProfitSharing(300L, List.of(21L));

		verify(hfpayProfitSplitConfirmService, never())
				.pay006(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("E3.3：无 SUCCESS 交易则 patch NO_TRADE，不调 pay006")
	void e3_3_no_success_trade_patches_failed() {
		OrderProfitSharing data = baseSharingRow(31L, 300L, 0);
		when(orderProfitSharingMapper.selectById(31L)).thenReturn(data);
		when(orderProfitSharingDetailsMapper.selectList(any())).thenReturn(List.of(detailLine(31L, 100)));
		when(normalOrdersMapper.selectById(300L)).thenReturn(orderRow(300L));
		when(tradeMapper.selectOne(any())).thenReturn(null);

		executionService.executeProfitSharing(300L, List.of(31L));

		verify(hfpayProfitSplitConfirmService, never())
				.pay006(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
		verify(orderProfitSharingMapper, times(1)).update(isNull(), any());
	}

	@Test
	@DisplayName("E3.4：分账明细无有效金额 NO_DIV，不调 pay006")
	void e3_4_no_valid_div_lines_patches_failed() {
		OrderProfitSharing data = baseSharingRow(41L, 400L, 500);
		when(orderProfitSharingMapper.selectById(41L)).thenReturn(data);
		OrderProfitSharingDetails z = detailLine(41L, 0);
		when(orderProfitSharingDetailsMapper.selectList(any())).thenReturn(List.of(z));
		when(normalOrdersMapper.selectById(400L)).thenReturn(orderRow(400L));
		when(tradeMapper.selectOne(any())).thenReturn(successTrade());

		executionService.executeProfitSharing(400L, List.of(41L));

		verify(hfpayProfitSplitConfirmService, never())
				.pay006(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
		verify(orderProfitSharingMapper, times(1)).update(isNull(), any());
	}

	@Test
	@DisplayName("E3.5：div JSON 序列化失败走 patchSharingFailed(JSON)，不调 pay006")
	void e3_5_json_error_patches_failed() throws JsonProcessingException {
		ObjectMapper badOm = mock(ObjectMapper.class);
		when(badOm.writeValueAsString(any()))
				.thenThrow(
						new JsonProcessingException("e3.5") {
							private static final long serialVersionUID = 1L;
						});
		HfpayProfitSharingDispatchExecutionService svc =
				new HfpayProfitSharingDispatchExecutionService(
						orderProfitSharingMapper,
						orderProfitSharingDetailsMapper,
						normalOrdersMapper,
						tradeMapper,
						hfpayProfitSplitConfirmService,
						badOm);

		OrderProfitSharing data = baseSharingRow(51L, 500L, 500);
		when(orderProfitSharingMapper.selectById(51L)).thenReturn(data);
		when(orderProfitSharingDetailsMapper.selectList(any())).thenReturn(List.of(detailLine(51L, 100)));
		when(normalOrdersMapper.selectById(500L)).thenReturn(orderRow(500L));
		when(tradeMapper.selectOne(any())).thenReturn(successTrade());

		svc.executeProfitSharing(500L, List.of(51L));

		verify(hfpayProfitSplitConfirmService, never())
				.pay006(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
		verify(orderProfitSharingMapper, atLeastOnce()).update(isNull(), any());
	}

	@Test
	@DisplayName("E3.8：pay006 返回非 C00000 回写 status=2 且仍调用 update")
	void e3_8_pay006_non_success_code_sets_status_failed() {
		OrderProfitSharing data = baseSharingRow(61L, 600L, 500);
		when(orderProfitSharingMapper.selectById(61L)).thenReturn(data);
		when(orderProfitSharingDetailsMapper.selectList(any())).thenReturn(List.of(detailLine(61L, 500)));
		when(normalOrdersMapper.selectById(600L)).thenReturn(orderRow(600L));
		when(tradeMapper.selectOne(any())).thenReturn(successTrade());

		Map<String, Object> payRes = new LinkedHashMap<>();
		payRes.put("resp_code", "E00001");
		payRes.put("resp_desc", "fail");
		when(hfpayProfitSplitConfirmService.pay006(anyLong(), anyString(), anyString(), eq("27"), anyString(), anyString()))
				.thenReturn(payRes);
		when(hfpayProfitSplitConfirmService.extractRespCode(any())).thenReturn("E00001");

		executionService.executeProfitSharing(600L, List.of(61L));

		verify(hfpayProfitSplitConfirmService).pay006(anyLong(), anyString(), anyString(), eq("27"), anyString(), anyString());
		verify(orderProfitSharingMapper).update(isNull(), any());
	}

	@Test
	@DisplayName("payload.entities 解析后委托 executeProfitSharing")
	void executeFromDispatchPayload_delegates() {
		when(orderProfitSharingMapper.selectById(21L)).thenReturn(null);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", 300L);
		entities.put("order_profit_sharing_id", List.of(21L));
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		executionService.executeFromDispatchPayload(payload);

		verify(orderProfitSharingMapper).selectById(21L);
	}

	private static OrderProfitSharing baseSharingRow(long sharingId, long orderId, int totalFee) {
		OrderProfitSharing data = new OrderProfitSharing();
		data.setOrderProfitSharingId(sharingId);
		data.setCompanyId(9L);
		data.setOrderId(orderId);
		data.setStatus(0);
		data.setTotalFee(totalFee);
		return data;
	}

	private static OrderProfitSharingDetails detailLine(long sharingId, int fen) {
		OrderProfitSharingDetails det = new OrderProfitSharingDetails();
		det.setSharingId(sharingId);
		det.setTotalFee(fen);
		det.setChannelId("u1");
		det.setChannelAcctId("a1");
		return det;
	}

	private static NormalOrders orderRow(long orderId) {
		NormalOrders ord = new NormalOrders();
		ord.setOrderId(orderId);
		ord.setCreateTime(1700000000);
		return ord;
	}

	private static Trade successTrade() {
		Trade tr = new Trade();
		tr.setTradeId("T-ok");
		tr.setTradeState("SUCCESS");
		return tr;
	}
}
