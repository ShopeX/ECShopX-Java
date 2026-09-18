package cn.shopex.ecshopx.espier.service.offline;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.espier.domain.OfflineBankAccount;
import cn.shopex.ecshopx.espier.mapper.OfflineBankAccountMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OfflineBankAccountApplicationServiceCreateTest {

	private static final long COMPANY_ID = 1L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, OfflineBankAccount.class);
	}

	@Mock
	private OfflineBankAccountMapper offlineBankAccountMapper;

	@Mock
	private OfflineBankAccountMultiLangWriteService offlineBankAccountMultiLangWriteService;

	@Mock
	private OfflineBankAccountMultiLangReadService offlineBankAccountMultiLangReadService;

	@InjectMocks
	private OfflineBankAccountApplicationService service;

	@BeforeEach
	void stubInsertAssignsId() {
		doAnswer(invocation -> {
					OfflineBankAccount entity = invocation.getArgument(0);
					entity.setId(100L);
					return 1;
				})
				.when(offlineBankAccountMapper)
				.insert(any(OfflineBankAccount.class));
	}

	@Test
	@DisplayName("创建首个默认账户：无已有默认时不应报错")
	void createFirstDefaultAccount_whenNoExistingDefault_succeeds() {
		Map<String, Object> params = baseParams();
		params.put("is_default", 1);

		assertDoesNotThrow(() -> service.create(COMPANY_ID, params, "zh-CN"));

		verify(offlineBankAccountMapper, never()).selectCount(any());
		verify(offlineBankAccountMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
		verify(offlineBankAccountMapper, times(1)).insert(any(OfflineBankAccount.class));
		verify(offlineBankAccountMultiLangWriteService, times(1))
				.syncAfterInsert(eq(100L), any(Map.class), eq("zh-CN"));
	}

	@Test
	@DisplayName("创建非默认账户：不触发取消旧默认")
	void createNonDefaultAccount_skipsClearDefault() {
		Map<String, Object> params = baseParams();
		params.put("is_default", 0);

		assertDoesNotThrow(() -> service.create(COMPANY_ID, params, "zh-CN"));

		verify(offlineBankAccountMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
		verify(offlineBankAccountMapper, times(1)).insert(any(OfflineBankAccount.class));
	}

	private static Map<String, Object> baseParams() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("bank_account_name", "户名1");
		params.put("bank_account_no", "12313123123");
		params.put("bank_name", "上海工商银行");
		params.put("china_ums_no", "12313");
		params.put("pic", "https://example.com/pic.png");
		params.put("remark", "");
		return params;
	}
}
