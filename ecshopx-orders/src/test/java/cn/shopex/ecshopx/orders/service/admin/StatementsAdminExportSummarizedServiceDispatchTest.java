package cn.shopex.ecshopx.orders.service.admin;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.StatementsSummarizedExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.orders.service.statement.StatementsSummarizedAdminFilterAssembler;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatementsAdminExportSummarizedServiceDispatchTest {

	@Mock
	private StatementsSummarizedAdminFilterAssembler filterAssembler;

	@Mock
	private StatementsSummarizedExportFileJobDispatchPublisher statementsSummarizedExportFileJobDispatchPublisher;

	@InjectMocks
	private StatementsAdminExportSummarizedService statementsAdminExportSummarizedService;

	@Test
	void exportSummarized_enqueuesOnceViaPublisher() {
		long companyId = 11L;
		long operatorId = 22L;
		Map<String, Object> merged = Map.of();
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("k", "v");
		when(filterAssembler.assembleListFilter(
						eq(companyId), eq(operatorId), eq("admin"), eq(33L), eq(44L), eq(merged)))
				.thenReturn(filter);

		statementsAdminExportSummarizedService.exportSummarized(
				companyId, operatorId, "admin", 33L, 44L, merged);

		verify(statementsSummarizedExportFileJobDispatchPublisher)
				.enqueueSummarizedExport(
						eq(companyId),
						eq(operatorId),
						argThat(
								f ->
										f != null
												&& f.size() == 2
												&& companyId == asLong(f.get("company_id"))
												&& "v".equals(String.valueOf(f.get("k")))));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
