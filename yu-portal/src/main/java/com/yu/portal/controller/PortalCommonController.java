package com.yu.portal.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.brm.domain.BrmSemester;
import com.yu.brm.service.IBrmSemesterService;

/**
 * 门户公共字典Controller（学期等轻量下拉，登录用户均可访问）
 *
 * <p>E2 学期/字典全局供给：门户各页学期下拉由本接口供给，换学期只改 brm_semester 数据即生效，零发版。
 *
 * @author ruoyi
 * @date 2026-10-16
 */
@RestController
@RequestMapping("/portal/common")
public class PortalCommonController extends BaseController
{
    @Autowired
    private IBrmSemesterService brmSemesterService;

    /** 学期下拉：启用学期按学期ID倒序（最新在前），仅回传下拉所需字段 */
    @GetMapping("/semesters")
    public AjaxResult semesters()
    {
        BrmSemester query = new BrmSemester();
        query.setStatus("0");
        List<BrmSemester> list = brmSemesterService.selectBrmSemesterList(query);
        list.sort(Comparator.comparing(BrmSemester::getSemesterId).reversed());
        List<Map<String, Object>> options = new ArrayList<>();
        for (BrmSemester semester : list)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("semesterId", semester.getSemesterId());
            item.put("semesterName", semester.getSemesterName());
            options.add(item);
        }
        return success(options);
    }
}
