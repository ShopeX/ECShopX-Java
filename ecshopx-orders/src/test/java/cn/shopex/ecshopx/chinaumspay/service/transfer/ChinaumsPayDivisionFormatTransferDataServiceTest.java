package cn.shopex.ecshopx.chinaumspay.service.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivision;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionDetail;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionDetailMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionMapper;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsPaymentSettingLoadPort;
import cn.shopex.ecshopx.orders.domain.DistributionDistributorPeek;
import cn.shopex.ecshopx.orders.domain.OrdersRelChinaumspayDivision;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersRelChinaumspayDivisionMapper;
import cn.shopex.ecshopx.orders.domain.OrdersRelChinaumspayDivision;
import cn.shopex.ecshopx.orders.service.division.NeedTransferOrderRow;
import cn.shopex.ecshopx.orders.service.division.OrderAppliedTotalRefundFenQueryService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * plan §5 + analysis 步骤 4.3.2/§8-2：format 各分支与跳过。
 */
@ExtendWith(MockitoExtension.class)
class ChinaumsPayDivisionFormatTransferDataServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), ChinaumspayDivisionDetail.class);
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), ChinaumspayDivision.class);
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), OrdersRelChinaumspayDivision.class);
	}

	@Mock
	private ChinaumsPaymentSettingLoadPort paymentSettingLoadPort;
	@Mock
	private DistributionDistributorPeekMapper distributionDistributorPeekMapper;
	@Mock
	private OrderAppliedTotalRefundFenQueryService orderAppliedTotalRefundFenQueryService;
	@Mock
	private ChinaumspayDivisionMapper chinaumspayDivisionMapper;
	@Mock
	private ChinaumspayDivisionDetailMapper chinaumspayDivisionDetailMapper;
	@Mock
	private OrdersRelChinaumspayDivisionMapper ordersRelChinaumspayDivisionMapper;

	private ChinaumsPayDivisionFormatTransferDataService service;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		lenient().when(ordersRelChinaumspayDivisionMapper.update(any(), any())).thenReturn(1);
		lenient().when(chinaumspayDivisionDetailMapper.update(any(), any())).thenReturn(1);
		service = new ChinaumsPayDivisionFormatTransferDataService(
				paymentSettingLoadPort,
				distributionDistributorPeekMapper,
				orderAppliedTotalRefundFenQueryService,
				chinaumspayDivisionMapper,
				chinaumspayDivisionDetailMapper,
				ordersRelChinaumspayDivisionMapper,
				objectMapper);
	}

	@Test
	@DisplayName("analysis 4.3.2/4.3.3: 订单行为空，不追加")
	void emptyRows_returnsNull() {
		assertThat(service.formatTransferData(1L, 5L, null)).isNull();
		assertThat(service.formatTransferData(1L, 5L, List.of())).isNull();
	}

	@Test
	@DisplayName("analysis 4.3.2: 无店记录")
	void noDistributor_returnsNull() {
		when(distributionDistributorPeekMapper.selectById(5L)).thenReturn(null);
		assertThat(service.formatTransferData(1L, 5L, List.of(needRow(1L, 1L, "1000", 5L))))
				.isNull();
	}

	@Test
	@DisplayName("analysis 4.3.2/§8-2: 无 split_ledger_info")
	void blankSplitJson_returnsNull() {
		DistributionDistributorPeek d = new DistributionDistributorPeek();
		d.setDistributorId(5L);
		d.setSplitLedgerInfo("  ");
		when(distributionDistributorPeekMapper.selectById(5L)).thenReturn(d);
		assertThat(service.formatTransferData(1L, 5L, List.of(needRow(1L, 1L, "1000", 5L))))
				.isNull();
	}

	@Test
	@DisplayName("analysis 4.3.2/§8-2: JSON 非法，短路为 null、不 NPE")
	void badSplitJson_returnsNull() {
		DistributionDistributorPeek d = new DistributionDistributorPeek();
		d.setDistributorId(5L);
		d.setSplitLedgerInfo("{\"x\":");
		when(distributionDistributorPeekMapper.selectById(5L)).thenReturn(d);
		assertThat(service.formatTransferData(1L, 5L, List.of(needRow(1L, 1L, "1000", 5L))))
				.isNull();
	}

	@Test
	@DisplayName("analysis 4.3.2/§8-2: JSON 非 object")
	void nonObjectJson_returnsNull() {
		DistributionDistributorPeek d = new DistributionDistributorPeek();
		d.setDistributorId(5L);
		d.setSplitLedgerInfo("[]");
		when(distributionDistributorPeekMapper.selectById(5L)).thenReturn(d);
		assertThat(service.formatTransferData(1L, 5L, List.of(needRow(1L, 1L, "1000", 5L))))
				.isNull();
	}

	@Test
	@DisplayName("analysis 4.3.2: 无公司支付配置")
	void noCompanyPay_returnsNull() {
		stubDistributor("{\"headquarters_proportion\":100}");
		when(paymentSettingLoadPort.load(1L, "")).thenReturn(Map.of());
		assertThat(
						service.formatTransferData(
								1L, 5L, List.of(needRow(1L, 1L, "1000", 5L))))
				.isNull();
	}

	@Test
	@DisplayName("analysis 4.3.2: 实付 <=0，orders_rel 标 SKIP 且不建分账主表")
	void actualLeZero_marksRelSkip() {
		stubDistributor("{\"headquarters_proportion\":100}");
		stubPayMaps();
		NeedTransferOrderRow row = needRow(99L, 7L, "1000", 5L);
		when(orderAppliedTotalRefundFenQueryService.sum(1L, 7L)).thenReturn(1000);
		assertThat(service.formatTransferData(1L, 5L, List.of(row))).isNull();
		verify(ordersRelChinaumspayDivisionMapper, atLeastOnce())
				.update(isNull(), any(LambdaUpdateWrapper.class));
		verify(chinaumspayDivisionMapper, never()).insert(any(ChinaumspayDivision.class));
	}

	@Test
	@DisplayName("analysis 4.3.2+4.3.4: 单可划付单，非空结果")
	void happyPath_nonEmpty() {
		stubDistributor("{\"headquarters_proportion\":100}");
		stubPayMaps();
		NeedTransferOrderRow row = needRow(1L, 2L, "10000", 5L);
		when(orderAppliedTotalRefundFenQueryService.sum(1L, 2L)).thenReturn(0);
		doAnswerDetailThenDivision();
		var r = service.formatTransferData(1L, 5L, List.of(row));
		assertThat(r).isNotNull();
		assertThat(r.getDivisionId()).isPositive();
		assertThat(r.getOrderIds()).containsExactly(2L);
		assertThat(r.getTransfer()).isNotEmpty();
		assertThat(r.getDivision()).isNotEmpty();
		verify(chinaumspayDivisionMapper, atLeastOnce()).insert(any(ChinaumspayDivision.class));
	}

	private void doAnswerDetailThenDivision() {
		doAnswer(
						invocation -> {
							ChinaumspayDivisionDetail det = invocation.getArgument(0);
							det.setId(1001L);
							return 1;
						})
				.when(chinaumspayDivisionDetailMapper)
				.insert(any(ChinaumspayDivisionDetail.class));
		doAnswer(
						invocation -> {
							ChinaumspayDivision div = invocation.getArgument(0);
							div.setId(55L);
							return 1;
						})
				.when(chinaumspayDivisionMapper)
				.insert(any(ChinaumspayDivision.class));
		lenient().when(chinaumspayDivisionDetailMapper.update(any(), any())).thenReturn(1);
	}

	private void stubDistributor(String splitJson) {
		DistributionDistributorPeek d = new DistributionDistributorPeek();
		d.setDistributorId(5L);
		d.setDealerId(0L);
		d.setSplitLedgerInfo(splitJson);
		when(distributionDistributorPeekMapper.selectById(5L)).thenReturn(d);
	}

	private void stubPayMaps() {
		Map<String, Object> pay = new HashMap<>();
		pay.put("rate", "0");
		pay.put("enterpriseid", "ENT_P");
		pay.put("bank_name", "b");
		pay.put("bank_code", "c");
		pay.put("bank_account", "a");
		Map<String, Object> dPay = new HashMap<>();
		dPay.put("enterpriseid", "ENT_D");
		when(paymentSettingLoadPort.load(1L, "")).thenReturn(pay);
		when(paymentSettingLoadPort.load(1L, "distributor_5")).thenReturn(dPay);
	}

	private static NeedTransferOrderRow needRow(Long relId, Long orderId, String total, long dist) {
		var n = new NeedTransferOrderRow();
		n.setId(relId);
		n.setOrderId(orderId);
		n.setCompanyId(1L);
		n.setTotalFee(total);
		n.setDistributorId(dist);
		return n;
	}
}
