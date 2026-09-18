package cn.shopex.ecshopx.orders.service.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.StatementDetailsExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.orders.domain.Statements;
import cn.shopex.ecshopx.orders.mapper.StatementsMapper;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatementsAdminExportDetailServiceDispatchTest {

	@Mock
	private StatementsMapper statementsMapper;

	@Mock
	private SupplierMapper supplierMapper;

	@Mock
	private StatementDetailsExportFileJobDispatchPublisher statementDetailsExportFileJobDispatchPublisher;

	@InjectMocks
	private StatementsAdminExportDetailService statementsAdminExportDetailService;

	@Test
	void exportDetail_enqueuesOnceViaPublisher() {
		long companyId = 11L;
		long operatorId = 22L;
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("statement_id", 100L);

		Statements row = new Statements();
		row.setMerchantType("merchant");
		when(statementsMapper.selectOne(any())).thenReturn(row);

		statementsAdminExportDetailService.exportDetail(
				companyId, operatorId, "distributor", 33L, null, merged);

		verify(statementDetailsExportFileJobDispatchPublisher)
				.enqueueDetailExport(
						eq(companyId),
						eq(operatorId),
						argThat(
								f ->
										f != null
												&& companyId == asLong(f.get("company_id"))
												&& 100L == asLong(f.get("statement_id"))
												&& "merchant".equals(String.valueOf(f.get("merchant_type")))
												&& 33L == asLong(f.get("distributor_id"))));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
