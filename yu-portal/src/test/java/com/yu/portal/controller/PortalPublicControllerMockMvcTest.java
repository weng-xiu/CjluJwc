package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.portal.domain.PortalArticle;
import com.yu.portal.domain.PortalBanner;
import com.yu.portal.domain.PortalColumn;
import com.yu.portal.service.IPortalArticleService;
import com.yu.portal.service.IPortalBannerService;
import com.yu.portal.service.IPortalColumnService;

/**
 * 门户前台公开接口层测试（Q1 第十七批：MockMvc standalone）。
 * 覆盖匿名门户首页/详情/检索契约：home 聚合轮播+置顶+各栏目最新文章并做数量截断
 * （news/academic/notice 上限 5、campus 上限 4）且可见栏目查询固定 isVisible=1；
 * article 仅放行已发布（publishStatus=2）否则 error，放行时自增浏览并以 null→1 兜底、附上一篇/下一篇；
 * search 以登录无关的 title=keyword 且强制 publishStatus=2 只检已发布。渲染/统计业务由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalPublicControllerMockMvcTest {

    @Mock
    private IPortalArticleService portalArticleService;

    @Mock
    private IPortalBannerService portalBannerService;

    @Mock
    private IPortalColumnService portalColumnService;

    @InjectMocks
    private PortalPublicController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private List<PortalArticle> articles(int n) {
        List<PortalArticle> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            PortalArticle a = new PortalArticle();
            a.setArticleId((long) i);
            list.add(a);
        }
        return list;
    }

    @Test
    @DisplayName("home：聚合各区块并对 news/campus 做上限截断")
    void home_aggregatesAndTruncates() throws Exception {
        when(portalBannerService.selectActiveBanners()).thenReturn(List.of(new PortalBanner()));
        when(portalArticleService.selectFeaturedArticles()).thenReturn(articles(2));
        when(portalArticleService.selectPublishedArticlesByColumnCode("news")).thenReturn(articles(6));
        when(portalArticleService.selectPublishedArticlesByColumnCode("academic")).thenReturn(articles(3));
        when(portalArticleService.selectPublishedArticlesByColumnCode("notice")).thenReturn(articles(5));
        when(portalArticleService.selectPublishedArticlesByColumnCode("campus")).thenReturn(articles(5));
        when(portalColumnService.selectPortalColumnList(any(PortalColumn.class)))
                .thenReturn(List.of(new PortalColumn()));

        mockMvc.perform(get("/portal/public/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.featured.length()").value(2))
                .andExpect(jsonPath("$.news.length()").value(5))   // 6 截断为 5
                .andExpect(jsonPath("$.academic.length()").value(3)) // 未超限保持
                .andExpect(jsonPath("$.campus.length()").value(4));  // 5 截断为 4

        ArgumentCaptor<PortalColumn> captor = ArgumentCaptor.forClass(PortalColumn.class);
        verify(portalColumnService).selectPortalColumnList(captor.capture());
        Assertions.assertEquals("1", captor.getValue().getIsVisible());
    }

    @Test
    @DisplayName("column：按栏目编码查询并返回分页行")
    void column_returnsPage() throws Exception {
        when(portalArticleService.selectPublishedArticlesByColumnCode("news")).thenReturn(articles(2));

        mockMvc.perform(get("/portal/public/column/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows.length()").value(2));
    }

    @Test
    @DisplayName("article：已发布则自增浏览、null 浏览兜底为 1 并附上一篇/下一篇")
    void article_publishedIncrementsView() throws Exception {
        PortalArticle a = new PortalArticle();
        a.setArticleId(10L);
        a.setColumnId(3L);
        a.setPublishStatus("2");
        a.setViewCount(null);
        when(portalArticleService.selectPortalArticleById(10L)).thenReturn(a);
        when(portalArticleService.selectPrevArticle(eq(3L), eq(10L))).thenReturn(article(100L));
        when(portalArticleService.selectNextArticle(eq(3L), eq(10L))).thenReturn(article(200L));

        mockMvc.perform(get("/portal/public/article/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.viewCount").value(1))
                .andExpect(jsonPath("$.prev.articleId").value(100))
                .andExpect(jsonPath("$.next.articleId").value(200));

        verify(portalArticleService).incrementViewCount(10L);
    }

    @Test
    @DisplayName("article：不存在返回 error 且不自增浏览")
    void article_absentReturnsError() throws Exception {
        when(portalArticleService.selectPortalArticleById(404L)).thenReturn(null);

        mockMvc.perform(get("/portal/public/article/404"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(portalArticleService, never()).incrementViewCount(any());
    }

    @Test
    @DisplayName("article：未发布（publishStatus≠2）返回 error")
    void article_notPublishedReturnsError() throws Exception {
        PortalArticle a = new PortalArticle();
        a.setArticleId(10L);
        a.setPublishStatus("0");
        when(portalArticleService.selectPortalArticleById(10L)).thenReturn(a);

        mockMvc.perform(get("/portal/public/article/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(portalArticleService, never()).incrementViewCount(any());
    }

    @Test
    @DisplayName("banners：返回激活轮播列表")
    void banners_returnsList() throws Exception {
        when(portalBannerService.selectActiveBanners()).thenReturn(List.of(new PortalBanner()));

        mockMvc.perform(get("/portal/public/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("search：keyword 绑定 title 且强制只检已发布")
    void search_forcesPublishedAndKeyword() throws Exception {
        when(portalArticleService.selectPortalArticleList(any(PortalArticle.class)))
                .thenReturn(articles(1));

        mockMvc.perform(get("/portal/public/search").param("keyword", "期末"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows.length()").value(1));

        ArgumentCaptor<PortalArticle> captor = ArgumentCaptor.forClass(PortalArticle.class);
        verify(portalArticleService).selectPortalArticleList(captor.capture());
        Assertions.assertEquals("期末", captor.getValue().getTitle());
        Assertions.assertEquals("2", captor.getValue().getPublishStatus());
    }

    private PortalArticle article(long id) {
        PortalArticle a = new PortalArticle();
        a.setArticleId(id);
        return a;
    }
}
