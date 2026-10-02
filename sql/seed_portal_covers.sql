-- ============================================================
-- 门户文章封面 & 轮播图 配图脚本（幂等，按标题主题匹配网图）
-- 图片来源：Unsplash（已实测无 Referer 亦可 200 加载，适合热链；许可允许商用免署名）
-- 背景：portal_article.cover_url / portal_banner.image_url 原指向
--       /profile/upload/... 等本地文件，磁盘上不存在，前端回退为“暂无图片”。
-- 说明：UPDATE 按主键定位，可重复执行。URL 带 ?q=80&w=1600 尺寸参数以加快加载。
-- ============================================================

-- 文章封面（按标题主题）
-- 1 news 教学工作会议 → 学术报告厅
UPDATE portal_article SET cover_url = 'https://images.unsplash.com/photo-1587825140708-dfaf72ae4b04?q=80&w=1600&auto=format&fit=crop' WHERE article_id = 1;
-- 2 news 与中科院签署战略合作协议 → 签约握手
UPDATE portal_article SET cover_url = 'https://images.unsplash.com/photo-1521791136064-7986c2920216?q=80&w=1600&auto=format&fit=crop' WHERE article_id = 2;
-- 3 academic 国家自然科学基金重点项目 → 科研实验室
UPDATE portal_article SET cover_url = 'https://images.unsplash.com/photo-1578496480240-32d3e0c04525?q=80&w=1600&auto=format&fit=crop' WHERE article_id = 3;
-- 4 notice 期末考试安排 → 教室考试
UPDATE portal_article SET cover_url = 'https://images.unsplash.com/photo-1598981457915-aea220950616?q=80&w=1600&auto=format&fit=crop' WHERE article_id = 4;
-- 5 notice 毕业生学位授予 → 毕业典礼
UPDATE portal_article SET cover_url = 'https://images.unsplash.com/photo-1633734973050-d6499a977c17?q=80&w=1600&auto=format&fit=crop' WHERE article_id = 5;
-- 6 campus 校园樱花季摄影展 → 春天樱花
UPDATE portal_article SET cover_url = 'https://images.unsplash.com/photo-1775011412407-20a8786f5ccd?q=80&w=1600&auto=format&fit=crop' WHERE article_id = 6;
-- 7 media 扎根荆楚服务区域发展 → 大学校园航拍
UPDATE portal_article SET cover_url = 'https://images.unsplash.com/photo-1767855017537-0c7c74f50ba9?q=80&w=1600&auto=format&fit=crop' WHERE article_id = 7;
-- 8 people 国家奖学金获得者学生科研 → 图书馆学习
UPDATE portal_article SET cover_url = 'https://images.unsplash.com/photo-1741699428220-65f37f3fbbcb?q=80&w=1600&auto=format&fit=crop' WHERE article_id = 8;

-- 首页轮播图（按标题主题）
-- banner 1 招生季 欢迎报考 → 校园大门/招生
UPDATE portal_banner SET image_url = 'https://images.unsplash.com/photo-1776861963167-fb0334d15555?q=80&w=1600&auto=format&fit=crop' WHERE banner_id = 1;
-- banner 2 与中科院签署战略合作协议 → 签约握手
UPDATE portal_banner SET image_url = 'https://images.unsplash.com/photo-1521791136064-7986c2920216?q=80&w=1600&auto=format&fit=crop' WHERE banner_id = 2;
-- banner 3 春日校园樱花季 → 春天樱花
UPDATE portal_banner SET image_url = 'https://images.unsplash.com/photo-1775011412407-20a8786f5ccd?q=80&w=1600&auto=format&fit=crop' WHERE banner_id = 3;
