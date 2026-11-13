package cn.shopex.ecshopx.popularize.integration.members;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import cn.shopex.ecshopx.popularize.service.PromoterGradeUpgradeTriggerService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class H5WxappMemberDeleteDisablePromoterPortImplTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Promoter.class);
	}

	@Mock
	private PromoterMapper promoterMapper;

	@Mock
	private PromoterGradeUpgradeTriggerService promoterGradeUpgradeTriggerService;

	@Test
	void whenNoPromoterRow_thenNoOp() {
		H5WxappMemberDeleteDisablePromoterPortImpl port =
				new H5WxappMemberDeleteDisablePromoterPortImpl(promoterMapper, promoterGradeUpgradeTriggerService);
		when(promoterMapper.selectOne(any())).thenReturn(null);

		port.disablePromoter(1L, 100L);

		verify(promoterMapper, never()).update(any(), any());
		verify(promoterGradeUpgradeTriggerService, never()).upgradeGrade(any(Long.class), any(Long.class));
	}

	@Test
	void whenMemberLogout_thenUnbindDisableAndEnqueueParentGrade() {
		H5WxappMemberDeleteDisablePromoterPortImpl port =
				new H5WxappMemberDeleteDisablePromoterPortImpl(promoterMapper, promoterGradeUpgradeTriggerService);

		Promoter row = new Promoter();
		row.setId(10L);
		row.setCompanyId(1L);
		row.setUserId(100L);
		row.setPid(50L);

		Promoter parent = new Promoter();
		parent.setId(50L);
		parent.setUserId(200L);

		when(promoterMapper.selectOne(any())).thenReturn(row);
		when(promoterMapper.selectById(50L)).thenReturn(parent);
		when(promoterMapper.update(any(), any())).thenReturn(1);

		port.disablePromoter(1L, 100L);

		verify(promoterMapper).update(any(), any());
		verify(promoterGradeUpgradeTriggerService).upgradeGrade(1L, 100L);
		verify(promoterGradeUpgradeTriggerService).upgradeGrade(1L, 200L);
	}
}
