package com.yu.common.utils.html;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

/**
 * 富文本 HTML 白名单清洗器（S-2 存储型 XSS 收口）
 *
 * <p>背景：{@code xss.excludes} 把 /system/notice、/portal/articleManage 从 XssFilter 中排除，
 * 这两类正文因此绕过统一过滤落库；而前端（顶部公告、门户通知、OA 公告）用 {@code v-html} 直出，
 * 任意有发布权的账号写入 {@code <img src=x onerror=...>} 即可在访问者会话里执行脚本。</p>
 *
 * <p>本类只保留排版必需的白名单标签与属性，其余标签（script/style/iframe/svg/form 等）连同内容一并丢弃，
 * 未列入白名单的属性（on* 事件、style、srcset 等）全部剥离；超链接与图片地址只放行
 * http/https/mailto 与站内相对地址，javascript:/data:/vbscript: 等协议一律剥除。</p>
 *
 * <p>接入要求「入口 + 落库前」双重校验：Controller 清洗保证回给前端的就是净化结果，
 * Service 再清洗一次兜住直接调用 Service 的旁路（定时任务、其他模块、后续新增的 Controller）。</p>
 *
 * @author ruoyi
 */
public final class RichTextSanitizer
{
    /**
     * 允许保留的标签：Quill 编辑器（公告/文章正文编辑器）的正常产出 + 通知里常见的表格排版。
     * 不做 {@code Safelist.relaxed()}：relaxed 放行 style 与各类结构性标签，攻击面明显更大。
     */
    private static final String[] ALLOWED_TAGS = {
            "p", "br", "span", "strong", "em", "b", "i", "u", "s", "sub", "sup",
            "h1", "h2", "h3", "h4", "h5", "h6",
            "ul", "ol", "li", "blockquote", "pre", "code",
            "a", "img",
            "table", "thead", "tbody", "tr", "td", "th"
    };

    /**
     * 只放行 Quill 自身产出的排版类名（ql-align-center、ql-indent-1、ql-syntax 等）。
     *
     * <p>jsoup 无法按“属性值”设白名单，只能先允许 class 再在清洗后二次扫一遍剥掉非 ql-* 类名；
     * 这样既保住首行缩进/对齐等既有排版，又不给“借用站点既有样式做视觉伪装”留口子。</p>
     */
    private static final Pattern SAFE_CLASS_TOKEN = Pattern.compile("ql-[a-z0-9-]+");

    /** 控制字符与空白：浏览器解析 URL 时会先剥掉它们，判定相对地址前必须做同样处理 */
    private static final Pattern URL_NOISE = Pattern.compile("[\\u0000-\\u0020\\u007f]");

    private static final String CLASS_ATTRIBUTE = "class";

    private static final Safelist SAFELIST = buildSafelist();

    private static final Document.OutputSettings OUTPUT_SETTINGS = new Document.OutputSettings().prettyPrint(false);

    private RichTextSanitizer()
    {
    }

    /**
     * 按白名单清洗富文本 HTML
     *
     * @param html 原始正文，允许为 null
     * @return 净化后的 HTML；入参为 null 或空串时原样返回
     */
    public static String sanitize(String html)
    {
        if (html == null || html.isEmpty())
        {
            return html;
        }
        String cleaned = Jsoup.clean(html, "", SAFELIST, OUTPUT_SETTINGS);
        return stripNonQlClasses(cleaned);
    }

    private static Safelist buildSafelist()
    {
        return new RelativeUrlSafelist()
                .addTags(ALLOWED_TAGS)
                .addAttributes("a", "href", "title", "target")
                .addAttributes("img", "src", "alt", "title", "width", "height")
                .addAttributes("td", "colspan", "rowspan")
                .addAttributes("th", "colspan", "rowspan")
                .addAttributes("p", CLASS_ATTRIBUTE)
                .addAttributes("span", CLASS_ATTRIBUTE)
                .addAttributes("li", CLASS_ATTRIBUTE)
                .addAttributes("pre", CLASS_ATTRIBUTE)
                .addProtocols("a", "href", "http", "https", "mailto")
                .addProtocols("img", "src", "http", "https")
                // target="_blank" 的新窗口句柄不再回传本站 window.opener（反向 Tabnabbing）
                .addEnforcedAttribute("a", "rel", "noopener noreferrer")
                // 相对地址保持原样，避免被解析成绝对地址（清洗时无 baseUri，解析结果会是空串）
                .preserveRelativeLinks(true);
    }

    /**
     * 二次剥离非 ql-* 类名；没有 class 时直接原样返回，避免多余的一次 DOM 解析。
     */
    private static String stripNonQlClasses(String html)
    {
        if (html.indexOf(CLASS_ATTRIBUTE + "=") < 0)
        {
            return html;
        }
        Document doc = Jsoup.parseBodyFragment(html);
        doc.outputSettings().prettyPrint(false);
        boolean changed = false;
        for (Element element : doc.select("[class]"))
        {
            Set<String> names = element.classNames();
            Set<String> kept = new LinkedHashSet<>();
            for (String name : names)
            {
                if (SAFE_CLASS_TOKEN.matcher(name).matches())
                {
                    kept.add(name);
                }
            }
            if (kept.size() == names.size())
            {
                continue;
            }
            changed = true;
            if (kept.isEmpty())
            {
                element.removeAttr(CLASS_ATTRIBUTE);
            }
            else
            {
                element.classNames(kept);
            }
        }
        return changed ? doc.body().html() : html;
    }

    /**
     * 允许站内相对地址的白名单。
     *
     * <p>jsoup 的协议校验只认带 scheme 的绝对地址（http:/https:/mailto:），相对地址会被判为不安全
     * 而整条丢弃——公告里指向站内页面的链接会静默失效。这里补一条判定：剥掉控制字符与空白后
     * <b>不含冒号</b>即视为相对地址放行；一旦出现冒号就退回父类，由协议白名单裁决，
     * 因此 {@code javascript:}、{@code java\tscript:}、{@code data:text/html}、{@code vbscript:} 仍然被拒。</p>
     */
    private static final class RelativeUrlSafelist extends Safelist
    {
        @Override
        protected boolean isSafeAttribute(String tagName, Element el, Attribute attr)
        {
            if (super.isSafeAttribute(tagName, el, attr))
            {
                return true;
            }
            String key = attr.getKey();
            if (!"href".equalsIgnoreCase(key) && !"src".equalsIgnoreCase(key))
            {
                return false;
            }
            String value = attr.getValue();
            if (value == null)
            {
                return false;
            }
            String normalized = URL_NOISE.matcher(value).replaceAll("");
            return !normalized.isEmpty() && normalized.indexOf(':') < 0;
        }
    }
}
