package com.yu.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yu.brm.service.IBrmDepartmentService;
import com.yu.common.core.domain.TreeSelect;
import com.yu.common.core.domain.entity.SysDept;
import com.yu.common.core.domain.entity.SysRole;
import com.yu.common.exception.ServiceException;
import com.yu.system.mapper.SysDeptMapper;
import com.yu.system.mapper.SysRoleMapper;

/**
 * 部门管理服务测试（G5 覆盖率爬坡 · yu-system）。
 *
 * <p>部门树构建、祖先链（ancestors）重算、删除前置校验是组织架构的骨架逻辑：
 * 祖先链算错会让整棵子树的部门数据权限瞬间失效，而这一步没有任何报错，只是查出来的数据变少或多出来。
 * 因此这里逐条钉住祖先链的拼接、子部门重写与「部门新增后同步到院系」的联动。</p>
 *
 * <p>说明：{@code selectDeptTreeList} 与 {@code checkDeptDataScope} 依赖
 * {@code SpringUtils.getAopProxy(this)}，在无 Spring 容器的纯单测里不可达，故未覆盖。</p>
 *
 * @author G5
 */
@ExtendWith(MockitoExtension.class)
class SysDeptServiceImplTest
{
    @Mock
    private SysDeptMapper deptMapper;

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private IBrmDepartmentService brmDepartmentService;

    @InjectMocks
    private SysDeptServiceImpl deptService;

    private static SysDept dept(Long deptId, Long parentId, String ancestors, String deptName)
    {
        SysDept d = new SysDept();
        d.setDeptId(deptId);
        d.setParentId(parentId);
        d.setAncestors(ancestors);
        d.setDeptName(deptName);
        d.setStatus("0");
        return d;
    }

    @Test
    @DisplayName("buildDeptTree：按 parentId 组装出多级树")
    void buildDeptTree_nestsChildren()
    {
        SysDept root = dept(100L, 0L, "0", "教务处");
        SysDept mid = dept(101L, 100L, "0,100", "教学运行科");
        SysDept leaf = dept(102L, 101L, "0,100,101", "选课管理岗");

        List<SysDept> tree = deptService.buildDeptTree(new ArrayList<>(List.of(root, mid, leaf)));

        assertEquals(1, tree.size(), "只有一个顶级节点，其余都要挂到父节点上");
        assertEquals("教务处", tree.get(0).getDeptName());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals("教学运行科", tree.get(0).getChildren().get(0).getDeptName());
        assertEquals("选课管理岗", tree.get(0).getChildren().get(0).getChildren().get(0).getDeptName(),
                "递归要一直挂到叶子，不能只处理一层");
    }

    @Test
    @DisplayName("buildDeptTree：找不到顶级节点时原样返回，不能返回空列表")
    void buildDeptTree_fallsBackWhenNoRoot()
    {
        // 两个节点互为父级：parentId 都能在入参里找到，故没有顶级节点
        SysDept a = dept(1L, 2L, "0", "甲");
        SysDept b = dept(2L, 1L, "0", "乙");

        List<SysDept> tree = deptService.buildDeptTree(new ArrayList<>(List.of(a, b)));

        assertEquals(2, tree.size(), "查不到顶级节点时必须原样返回，否则页面会显示空树");
    }

    @Test
    @DisplayName("buildDeptTreeSelect：树节点转换为 id/label 结构")
    void buildDeptTreeSelect_mapsToTreeSelect()
    {
        SysDept root = dept(100L, 0L, "0", "教务处");

        List<TreeSelect> options = deptService.buildDeptTreeSelect(new ArrayList<>(List.of(root)));

        assertEquals(1, options.size());
        assertEquals(100L, options.get(0).getId());
        assertEquals("教务处", options.get(0).getLabel());
    }

    @Test
    @DisplayName("selectDeptListByRoleId：把角色的部门严格模式传给 Mapper")
    void selectDeptListByRoleId_passesStrictlyFlag()
    {
        SysRole role = new SysRole();
        role.setRoleId(9L);
        role.setDeptCheckStrictly(true);
        when(roleMapper.selectRoleById(9L)).thenReturn(role);
        when(deptMapper.selectDeptListByRoleId(9L, true)).thenReturn(List.of(100L, 101L));

        List<Long> deptIds = deptService.selectDeptListByRoleId(9L);

        assertEquals(List.of(100L, 101L), deptIds);
        verify(roleMapper).selectRoleById(9L);
        verify(deptMapper).selectDeptListByRoleId(9L, true);
    }

    @Test
    @DisplayName("hasChildByDeptId / checkDeptExistUser：计数大于 0 才算存在")
    void existenceChecks_useGreaterThanZero()
    {
        when(deptMapper.hasChildByDeptId(100L)).thenReturn(1);
        when(deptMapper.hasChildByDeptId(101L)).thenReturn(0);
        when(deptMapper.checkDeptExistUser(100L)).thenReturn(2);
        when(deptMapper.checkDeptExistUser(101L)).thenReturn(0);

        assertTrue(deptService.hasChildByDeptId(100L));
        assertFalse(deptService.hasChildByDeptId(101L), "计数为 0 表示没有子部门");
        assertTrue(deptService.checkDeptExistUser(100L));
        assertFalse(deptService.checkDeptExistUser(101L), "计数为 0 表示没有在岗用户");
    }

    @Test
    @DisplayName("checkDeptNameUnique：同父级下重名判为非唯一，改名自身除外")
    void checkDeptNameUnique_detectsDuplicate()
    {
        SysDept exist = dept(100L, 0L, "0", "教务处");

        when(deptMapper.checkDeptNameUnique("教务处", 0L)).thenReturn(exist);

        SysDept creating = dept(null, 0L, "0", "教务处");
        assertFalse(deptService.checkDeptNameUnique(creating),
                "新增时 deptId 为 null 按 -1 处理，查到同名即冲突");

        SysDept renamingSelf = dept(100L, 0L, "0", "教务处");
        assertTrue(deptService.checkDeptNameUnique(renamingSelf),
                "改自己的名字时查到的是自己，不能判成重名");

        SysDept renamingOther = dept(101L, 0L, "0", "教务处");
        assertFalse(deptService.checkDeptNameUnique(renamingOther), "改别人占用的名字要判冲突");
    }

    @Test
    @DisplayName("checkDeptNameUnique：库里没有同名校验通过")
    void checkDeptNameUnique_passesWhenAbsent()
    {
        // 未 stub 时 Mockito 默认返回 null，正好是「库里查不到同名」的情形
        assertTrue(deptService.checkDeptNameUnique(dept(null, 0L, "0", "新部门")));
    }

    @Test
    @DisplayName("insertDept：祖先链由父部门 ancestors 追加 parentId 得到，并同步院系")
    void insertDept_buildsAncestorsAndSyncs()
    {
        SysDept parent = dept(100L, 0L, "0", "教务处");
        when(deptMapper.selectDeptById(100L)).thenReturn(parent);
        when(deptMapper.insertDept(any(SysDept.class))).thenReturn(1);

        SysDept child = dept(null, 100L, null, "教学运行科");
        assertEquals(1, deptService.insertDept(child));

        assertEquals("0,100", child.getAncestors(), "祖先链算错会让子树的部门数据权限失效");
        verify(brmDepartmentService).upsertFromSysDept(child);
    }

    @Test
    @DisplayName("insertDept：父部门停用时拒绝新增")
    void insertDept_rejectsDisabledParent()
    {
        SysDept parent = dept(100L, 0L, "0", "教务处");
        parent.setStatus("1");
        when(deptMapper.selectDeptById(100L)).thenReturn(parent);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deptService.insertDept(dept(null, 100L, null, "教学运行科")));

        assertEquals("部门停用，不允许新增", ex.getMessage());
        verify(deptMapper, never()).insertDept(any(SysDept.class));
        verify(brmDepartmentService, never()).upsertFromSysDept(any(SysDept.class));
    }

    @Test
    @DisplayName("updateDept：换了父部门后重算祖先链并重写整个子树的 ancestors")
    void updateDept_rewritesAncestorsOfSubtree()
    {
        SysDept newParent = dept(200L, 0L, "0", "研究生院");
        SysDept oldDept = dept(103L, 101L, "0,100,101", "选课管理岗");
        SysDept child = dept(104L, 103L, "0,100,101,103", "选课审核岗");

        when(deptMapper.selectDeptById(200L)).thenReturn(newParent);
        when(deptMapper.selectDeptById(103L)).thenReturn(oldDept);
        when(deptMapper.selectChildrenDeptById(103L)).thenReturn(new ArrayList<>(List.of(child)));
        when(deptMapper.updateDept(any(SysDept.class))).thenReturn(1);

        SysDept dept = dept(103L, 200L, "0,100,101", "选课管理岗");
        assertEquals(1, deptService.updateDept(dept));

        assertEquals("0,200", dept.getAncestors(), "自身祖先链要换成新父部门的链");
        assertEquals("0,200,103", child.getAncestors(), "子孙节点的祖先链必须整体重写，不能只改自己");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SysDept>> captor = ArgumentCaptor.forClass(List.class);
        verify(deptMapper).updateDeptChildren(captor.capture());
        assertEquals(1, captor.getValue().size());

        verify(brmDepartmentService).upsertFromSysDept(dept);
    }

    @Test
    @DisplayName("updateDept：部门启用时把祖先链上的所有上级一并置为正常")
    void updateDept_enablesAncestors()
    {
        when(deptMapper.selectDeptById(200L)).thenReturn(dept(200L, 0L, "0", "研究生院"));
        when(deptMapper.selectDeptById(103L)).thenReturn(dept(103L, 200L, "0,200", "选课管理岗"));
        when(deptMapper.updateDept(any(SysDept.class))).thenReturn(1);

        deptService.updateDept(dept(103L, 200L, "0,200", "选课管理岗"));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(deptMapper).updateDeptStatusNormal(captor.capture());
        assertEquals(List.of(0L, 200L), List.of(captor.getValue()),
                "祖先链要按「,」拆成部门 ID 数组，漏掉会留下停用的上级部门");
    }

    @Test
    @DisplayName("updateDept：顶级部门（ancestors 为 0）不触发上级启用")
    void updateDept_skipsTopLevel()
    {
        // 新旧父部门都查不到（默认返回 null）时跳过祖先链重算，只做落库与院系同步
        when(deptMapper.updateDept(any(SysDept.class))).thenReturn(1);

        deptService.updateDept(dept(100L, 0L, "0", "教务处"));

        verify(deptMapper, never()).updateDeptStatusNormal(any(Long[].class));
    }

    @Test
    @DisplayName("updateDeptChildren：没有子部门时不发更新")
    void updateDeptChildren_skipsEmptyChildren()
    {
        when(deptMapper.selectChildrenDeptById(103L)).thenReturn(new ArrayList<>());

        deptService.updateDeptChildren(103L, "0,200", "0,100,101");

        verify(deptMapper, never()).updateDeptChildren(any());
    }

    @Test
    @DisplayName("updateDeptSort：逐个落库排序值")
    void updateDeptSort_updatesEachRow()
    {
        deptService.updateDeptSort(new String[] { "100", "101" }, new String[] { "1", "2" });

        ArgumentCaptor<SysDept> captor = ArgumentCaptor.forClass(SysDept.class);
        verify(deptMapper, times(2)).updateDeptSort(captor.capture());
        assertEquals(100L, captor.getAllValues().get(0).getDeptId());
        assertEquals(1, captor.getAllValues().get(0).getOrderNum());
        assertEquals(101L, captor.getAllValues().get(1).getDeptId());
        assertEquals(2, captor.getAllValues().get(1).getOrderNum());
    }

    @Test
    @DisplayName("updateDeptSort：批量落库中途异常要包成可读的服务异常")
    void updateDeptSort_wrapsException()
    {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> deptService.updateDeptSort(null, new String[] { "1" }));

        assertEquals("保存排序异常，请联系管理员", ex.getMessage(),
                "不能让空指针直接冒到前端，否则只看到一个 500");
    }

    @Test
    @DisplayName("deleteDeptById：删除部门后同步删除院系记录")
    void deleteDeptById_syncsBrm()
    {
        when(deptMapper.deleteDeptById(100L)).thenReturn(1);

        assertEquals(1, deptService.deleteDeptById(100L));

        verify(brmDepartmentService).deleteByDeptId(100L);
    }

    @Test
    @DisplayName("selectDeptById / selectNormalChildrenDeptById：透传 Mapper 结果")
    void simpleQueries_passThrough()
    {
        SysDept dept = dept(100L, 0L, "0", "教务处");
        when(deptMapper.selectDeptById(100L)).thenReturn(dept);
        when(deptMapper.selectNormalChildrenDeptById(100L)).thenReturn(3);

        assertEquals(dept, deptService.selectDeptById(100L));
        assertEquals(3, deptService.selectNormalChildrenDeptById(100L));

        verify(deptMapper, never()).selectDeptListByRoleId(anyLong(), eq(false));
    }
}
