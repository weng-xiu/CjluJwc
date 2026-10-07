package com.yu.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.yu.system.domain.SysDataClass;
import com.yu.system.mapper.SysDataClassMapper;

/**
 * 数据分类分级台账服务层测试（K1 等保数据新规）。
 *
 * <p>覆盖唯一性校验（表名+字段名重复判定）、新增/修改自动补时间戳三类核心行为。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysDataClassServiceTest {

    @Mock
    private SysDataClassMapper dataClassMapper;

    @InjectMocks
    private SysDataClassServiceImpl service;

    private SysDataClass row(long id, String table, String column) {
        SysDataClass c = new SysDataClass();
        c.setClassId(id);
        c.setTableName(table);
        c.setColumnName(column);
        return c;
    }

    @Test
    @DisplayName("新增：库中无同表字段时判定为唯一")
    void add_uniqueWhenNoExisting() {
        when(dataClassMapper.selectDataClassByColumn(any())).thenReturn(null);
        assertTrue(service.checkColumnUnique(row(0L, "sam_student", "id_card")));
    }

    @Test
    @DisplayName("新增：命中既有登记（非自身）判定为不唯一")
    void add_conflictWhenExistingOther() {
        when(dataClassMapper.selectDataClassByColumn(any())).thenReturn(row(9L, "sam_student", "id_card"));
        assertFalse(service.checkColumnUnique(row(0L, "sam_student", "id_card")));
    }

    @Test
    @DisplayName("修改：命中的即为自身时仍判定唯一")
    void edit_selfIsNotConflict() {
        when(dataClassMapper.selectDataClassByColumn(any())).thenReturn(row(9L, "brm_teacher", "phone"));
        assertTrue(service.checkColumnUnique(row(9L, "brm_teacher", "phone")));
    }

    @Test
    @DisplayName("新增自动补创建时间，修改自动补更新时间")
    void insertAndUpdate_setTimestamps() {
        when(dataClassMapper.insertDataClass(any())).thenReturn(1);
        when(dataClassMapper.updateDataClass(any())).thenReturn(1);

        SysDataClass add = row(0L, "sys_user", "phonenumber");
        service.insertDataClass(add);
        assertNotNull(add.getCreateTime());

        SysDataClass upd = row(1L, "sys_user", "email");
        service.updateDataClass(upd);
        assertNotNull(upd.getUpdateTime());
    }
}
