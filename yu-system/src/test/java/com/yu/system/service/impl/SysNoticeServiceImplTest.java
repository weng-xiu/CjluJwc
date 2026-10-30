package com.yu.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yu.system.domain.SysNotice;
import com.yu.system.mapper.SysNoticeMapper;

/**
 * S-2 存储型 XSS：公告正文落库前的白名单清洗（Service 侧兜底）。
 *
 * <p>Controller 已清洗过一次，这里验证绕过 Controller 直调 Service 的路径（Job、其他模块）同样净化。</p>
 *
 * @author S-2
 */
@ExtendWith(MockitoExtension.class)
class SysNoticeServiceImplTest
{
    @Mock
    private SysNoticeMapper noticeMapper;

    @InjectMocks
    private SysNoticeServiceImpl noticeService;

    @Test
    @DisplayName("insertNotice：脚本与事件属性在落库前被剥离")
    void insertNotice_sanitizesContent()
    {
        when(noticeMapper.insertNotice(any(SysNotice.class))).thenReturn(1);
        SysNotice notice = new SysNotice();
        notice.setNoticeTitle("选课通知");
        notice.setNoticeContent("<p onmouseover=\"alert(1)\">正文</p><script>alert(document.cookie)</script>");

        assertEquals(1, noticeService.insertNotice(notice));

        ArgumentCaptor<SysNotice> captor = ArgumentCaptor.forClass(SysNotice.class);
        verify(noticeMapper).insertNotice(captor.capture());
        assertEquals("<p>正文</p>", captor.getValue().getNoticeContent());
    }

    @Test
    @DisplayName("updateNotice：图片 onerror 载荷在落库前被剥离")
    void updateNotice_sanitizesContent()
    {
        when(noticeMapper.updateNotice(any(SysNotice.class))).thenReturn(1);
        SysNotice notice = new SysNotice();
        notice.setNoticeId(7L);
        notice.setNoticeContent("<img src=\"x\" onerror=\"alert(1)\"><a href=\"javascript:alert(2)\">链接</a>");

        assertEquals(1, noticeService.updateNotice(notice));

        ArgumentCaptor<SysNotice> captor = ArgumentCaptor.forClass(SysNotice.class);
        verify(noticeMapper).updateNotice(captor.capture());
        assertEquals("<img src=\"x\"><a rel=\"noopener noreferrer\">链接</a>", captor.getValue().getNoticeContent());
    }
}
