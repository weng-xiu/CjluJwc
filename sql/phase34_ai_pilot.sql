-- =============================================================================
-- Phase 34：AI 应用试点（计划文档 五（二）AI 应用维度 / 四（三）能力空白延伸）
--
-- 目标：在不引入外部强依赖的前提下落地三项 AI 能力，并预留大模型接线：
--   1) 教务政策智能问答：知识库 + 本地检索（分词+词频重合度打分）+ 可插拔 LLM 网关
--      （未配置模型时自动降级为「抽取式回答 + 引用出处」，检索未命中时明确告知，绝不编造）
--   2) 选课推荐：以真实开课/名单/成绩/评教数据打分，硬约束复用既有选课冲突校验
--   3) 学业画像：成绩/GPA、学分达成、结构均衡、学习进度、学业风险、修读积极性
--      六维真实计算，并与同专业同年级对比
--
-- 说明：
--   1) sys_ai_knowledge 为教务处可维护的政策知识库（含关键词与出处），是问答的唯一
--      事实来源；sys_ai_chat_record 为每次问答的留痕（命中知识、回答来源、耗时），
--      支撑「AI 是否真的可用」的量化审计。
--   2) 大模型参数一律走 sys_config（与既有 tpm./aem./sam. 参数口径一致），
--      ai.llm.enabled=false 时问答走本地抽取式回答，功能仍完整可用。
--   3) 知识库种子条目按本系统实际业务规则口径撰写，教务处可在后台修订；
--      全部语句幂等，可重复执行。
-- =============================================================================

-- ----------------------------
-- 1、教务政策知识库
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_ai_knowledge (
  knowledge_id   bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '知识ID',
  category       varchar(30)  NOT NULL                COMMENT '分类（字典 sys_ai_category）',
  title          varchar(200) NOT NULL                COMMENT '标准问题/条目标题',
  keywords       varchar(500) DEFAULT NULL            COMMENT '检索关键词（逗号分隔，命中加权）',
  content        text                                 COMMENT '解答正文',
  source         varchar(200) DEFAULT NULL            COMMENT '依据来源（文件名/制度条款/系统口径）',
  ref_url        varchar(255) DEFAULT NULL            COMMENT '参考链接（可选）',
  hit_count      int(11)      DEFAULT 0               COMMENT '累计命中次数',
  order_num      int(4)       DEFAULT 0               COMMENT '排序',
  status         char(1)      DEFAULT '0'             COMMENT '状态（0启用 1停用）',
  create_by      varchar(64)  DEFAULT ''              COMMENT '创建者',
  create_time    datetime     DEFAULT NULL            COMMENT '创建时间',
  update_by      varchar(64)  DEFAULT ''              COMMENT '更新者',
  update_time    datetime     DEFAULT NULL            COMMENT '更新时间',
  remark         varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (knowledge_id),
  KEY idx_category_status (category, status),
  KEY idx_title (title)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='教务政策AI知识库';

-- ----------------------------
-- 2、AI 问答留痕
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_ai_chat_record (
  record_id      bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  user_id        bigint(20)   DEFAULT NULL            COMMENT '提问用户ID',
  user_name      varchar(64)  DEFAULT NULL            COMMENT '提问用户登录名',
  user_role      varchar(64)  DEFAULT NULL            COMMENT '提问用户角色标识',
  scene          varchar(20)  DEFAULT 'portal'        COMMENT '提问场景（portal门户自助 admin后台自测）',
  question       varchar(1000) NOT NULL               COMMENT '问题原文',
  answer         text                                 COMMENT '回答内容',
  answer_source  varchar(10)  DEFAULT NULL            COMMENT '回答来源（LLM大模型 EXTRACT本地抽取 NONE未命中 ERROR调用失败）',
  knowledge_ids  varchar(255) DEFAULT NULL            COMMENT '命中知识ID（逗号分隔）',
  knowledge_titles varchar(1000) DEFAULT NULL         COMMENT '命中知识标题（逗号分隔，用于引用展示）',
  confidence     decimal(5,2) DEFAULT 0               COMMENT '检索置信度（最高命中得分）',
  cost_time      int(11)      DEFAULT 0               COMMENT '耗时（毫秒）',
  status         char(1)      DEFAULT '0'             COMMENT '状态（0成功 1失败）',
  error_msg      varchar(500) DEFAULT NULL            COMMENT '失败原因',
  create_time    datetime     DEFAULT NULL            COMMENT '提问时间',
  PRIMARY KEY (record_id),
  KEY idx_user_time (user_id, create_time),
  KEY idx_answer_source (answer_source),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='AI问答留痕记录';

-- ----------------------------
-- 3、参数字典
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT 'AI知识分类', 'sys_ai_category', '0', 'system', NOW(), '教务政策AI知识库分类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'sys_ai_category');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT 'AI回答来源', 'sys_ai_answer_source', '0', 'system', NOW(), '智能问答回答生成口径'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'sys_ai_answer_source');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT * FROM (
  SELECT 1 AS a, '选课管理' AS b, 'SELECTION' AS c, 'sys_ai_category' AS d, '' AS e, 'primary' AS f, 'N' AS g, '0' AS h, 'system' AS i, NOW() AS j, '' AS k
  UNION ALL SELECT 2,'成绩与GPA','GRADE','sys_ai_category','','success','N','0','system',NOW(),''
  UNION ALL SELECT 3,'学籍异动','STATUS','sys_ai_category','','info','N','0','system',NOW(),''
  UNION ALL SELECT 4,'考核考试','EXAM','sys_ai_category','','warning','N','0','system',NOW(),''
  UNION ALL SELECT 5,'毕业与学位','GRADUATION','sys_ai_category','','danger','N','0','system',NOW(),''
  UNION ALL SELECT 6,'学业预警','WARNING','sys_ai_category','','primary','N','0','system',NOW(),''
  UNION ALL SELECT 7,'毕业论文','THESIS','sys_ai_category','','success','N','0','system',NOW(),''
  UNION ALL SELECT 8,'证书凭证','CERTIFICATE','sys_ai_category','','info','N','0','system',NOW(),''
  UNION ALL SELECT 9,'校历与其他','OTHER','sys_ai_category','','','Y','0','system',NOW(),''
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_ai_category' AND dict_value = 'SELECTION');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT * FROM (
  SELECT 1 AS a, '大模型生成' AS b, 'LLM' AS c, 'sys_ai_answer_source' AS d, '' AS e, 'success' AS f, 'N' AS g, '0' AS h, 'system' AS i, NOW() AS j, '检索结果作为上下文交由大模型生成' AS k
  UNION ALL SELECT 2,'本地抽取','EXTRACT','sys_ai_answer_source','','primary','Y','0','system',NOW(),'由知识库命中条目直接抽取作答'
  UNION ALL SELECT 3,'未命中','NONE','sys_ai_answer_source','','info','N','0','system',NOW(),'知识库无匹配，不编造答案'
  UNION ALL SELECT 4,'调用失败','ERROR','sys_ai_answer_source','','danger','N','0','system',NOW(),'大模型调用异常，已降级为抽取式回答'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_ai_answer_source' AND dict_value = 'LLM');

-- ----------------------------
-- 4、系统参数（大模型网关与算法阈值，未配置即走本地口径）
-- ----------------------------
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT * FROM (
  SELECT 'AI大模型开关' AS a, 'ai.llm.enabled' AS b, 'false' AS c, 'Y' AS d, 'system' AS e, NOW() AS f, 'true 时问答走 OpenAI 兼容大模型接口，false 时走本地知识库抽取式回答' AS g
  UNION ALL SELECT 'AI大模型服务地址','ai.llm.baseUrl','','Y','system',NOW(),'OpenAI 兼容服务基址，如 https://host/v1'
  UNION ALL SELECT 'AI大模型密钥','ai.llm.apiKey','','Y','system',NOW(),'Bearer 方式传入 Authorization 头'
  UNION ALL SELECT 'AI大模型名称','ai.llm.model','','Y','system',NOW(),'对话模型标识'
  UNION ALL SELECT 'AI大模型采样温度','ai.llm.temperature','0.3','Y','system',NOW(),'越低越贴合知识库原文'
  UNION ALL SELECT 'AI大模型最大输出','ai.llm.maxTokens','1024','Y','system',NOW(),'completion 上限 token 数'
  UNION ALL SELECT 'AI大模型超时秒','ai.llm.timeoutSeconds','20','Y','system',NOW(),'连接与读取超时'
  UNION ALL SELECT 'AI问答命中阈值','ai.qa.minScore','0.12','Y','system',NOW(),'检索得分为 0~1 的余弦相似度，低于该值判为未命中，回答明确告知未找到'
  UNION ALL SELECT 'AI问答召回条数','ai.qa.topK','4','Y','system',NOW(),'作为上下文/引用注入的知识条目数'
  UNION ALL SELECT 'AI推荐返回条数','ai.recommend.topN','8','Y','system',NOW(),'选课推荐最多返回的候选门数'
  UNION ALL SELECT 'AI推荐学分上限','ai.recommend.maxCredits','30','Y','system',NOW(),'推荐时校验的学期学分上限（与 tpm.selection.maxCredits 同口径）'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_config c2 WHERE c2.config_key = t.b);

-- 阈值量纲对齐：得分是 0~1 的余弦相似度，历史默认值 1.0 会导致几乎全部判为未命中；
-- 仅修正未被人为改动过的默认值，教务处自行调过的阈值保持不变（幂等）
UPDATE sys_config SET config_value = '0.12', update_by = 'system', update_time = NOW()
WHERE config_key = 'ai.qa.minScore' AND config_value = '1.0';

-- ----------------------------
-- 5、知识库种子（按本系统实际业务规则口径撰写，教务处可在后台修订）
-- ----------------------------
-- 列口径：写入 category / title / keywords / content 与统一的依据来源，其余字段取表默认值
-- （order_num=0、status='0' 启用、create_by 默认），避开多行 UNION 列数不一致问题
INSERT INTO sys_ai_knowledge (category, title, keywords, content, source, create_by, create_time)
SELECT t.a, t.b, t.c, t.d, '系统业务规则口径（教务处可在知识库后台修订并逐条注明来源）', 'system', NOW() FROM (
  SELECT 'SELECTION' AS a, '选课轮次是怎么安排的，错过本轮还能补选吗？' AS b, '选课,轮次,补选,开始,结束,退改选' AS c,
         '选课按「选课轮次」组织，每轮次设有开始时间与结束时间，状态分为未开始、进行中、已结束三类，只有进行中的轮次可以选课与退课。轮次上可配置每人最多可选门数与选课规则（专业、年级、院系、先修课程限制）。本轮结束后不再开放选课，补选由教务处另行发起新的轮次并在教务通知中公布，学生端无法自行补录。可在门户「选课中心」查看当前进行中的轮次与本人已选结果。' AS d
  UNION ALL SELECT 'SELECTION','选课时提示时间冲突或课程已满怎么办？','时间冲突,课程已满,容量,替代,教学班,冲突检测',
         '选课提交前系统会做四类校验：一是时间冲突，与已选课程在同一星期几的节次或周次区间重叠即判冲突；二是学分超限，所选学分合计超过本轮学分上限；三是课程已满，教学班已选人数达到容量上限；四是规则限制，不满足专业、年级、院系或先修课程条件。出现冲突时，门户会同时给出「替代课程建议」，即同一课程的其他教学班，已自动过滤已满与时间冲突的班次，可直接改选该班次。'
  UNION ALL SELECT 'SELECTION','选课抽签的规则是什么，落选后还有机会吗？','抽签,落选,候补,递补,种子,随机',
         '超出容量的课程采用抽签决定中签结果。抽签支持指定随机种子，未指定时系统自动生成并把种子与抽签时间记录在轮次上，同一种子可完整复现，便于审计。抽签后中签者置为中签状态，落选者按顺序进入候补队列并记录候补排名；一旦有中签学生退课释放容量，系统按候补排名自动递补，并同步刷新容量缓存。可在管理端对指定开课手动触发候补递补。'
  UNION ALL SELECT 'SELECTION','退课后容量什么时候释放，会影响下学期选课吗？','退课,退选,容量,回补,结果状态',
         '轮次进行中可在门户退课，退课后记录状态置为已退选，同时回补该教学班的可选容量，其他同学可立即选入。轮次结束后的退课不再回补容量，但会触发候补递补流程。退课记录会保留在选课历史中，不影响后续轮次的选课资格；频繁退选只会计入修读活跃度统计。'
  UNION ALL SELECT 'GRADE','成绩是怎么合成的，权重能改吗？','成绩,权重,平时成绩,期末成绩,总评,30%,70%',
         '总评成绩由平时成绩与期末成绩按比例合成。权重支持配置：可按具体课程或课程类别在「成绩权重配置」中维护平时、期末比例，未配置的课程按平时 30%、期末 70% 的默认比例回退。教师录入或导入成绩时系统自动按权重计算总评，并同步生成是否及格、绩点与等级。'
  UNION ALL SELECT 'GRADE','绩点和 GPA 有哪几种算法，怎么切换？','GPA,绩点,算法,百分制,四分制,自定义映射,重算',
         '系统内置三种算法：国内标准算法（绩点＝总分除以10再减5，低于60记0）、四分制分段映射（90分以上记4.0，向下逐段递减至60分记1.0）、自定义区间映射（按维护的分数区间到绩点的映射表计算）。在「GPA 配置」中可新增算法、维护分数映射并设为默认。默认算法变更后需手动触发批量重算，重算按学分加权得到平均绩点。'
  UNION ALL SELECT 'GRADE','成绩提交锁定后还能改吗？发现成绩录入错误怎么办？','成绩复核,提交,锁定,驳回,修改,申请',
         '成绩从录入到锁定分三段：教师提交、教研室审核、审核通过即锁定并记录锁定时间，已提交或已锁定的记录不允许直接改动。若成绩有误，须由教师或学生发起成绩复核申请，复核走两级审批流程（课程负责人初审、教务处终审），终审通过才回写新成绩并自动重算绩点，全过程留痕可追溯。录入开放期之外也无法录入。'
  UNION ALL SELECT 'GRADE','成绩单能打印并验真吗？','成绩单,打印,电子凭证,验真,验证码,模板',
         '门户「我的凭证」可自助申请成绩证明单、课表、准考证等电子凭证，系统按后台配置的打印模板渲染，并生成凭证编号、验证码与数据快照哈希。任何方可通过公开验真页输入编号与验证码核验真伪，验真时比对快照哈希，内容被改动即判无效，同时返回脱敏姓名。'
  UNION ALL SELECT 'STATUS','休学、复学、退学怎么申请，走什么流程？','休学,复学,退学,转学,异动,申请,审批',
         '学籍异动在门户「学籍服务」在线提交，开放休学、复学、退学三类申请，转学等其他类型由管理员在管理端登记。提交后自动进入两级审批流程：先由所在院系审批，再由教务处终审，审批通过自动回写学籍状态并向申请人推送站内消息，驳回同样通知并说明意见。申请后可在同一页面查看流程节点与办理意见，未被审批前可自行撤销。'
  UNION ALL SELECT 'STATUS','异动审批期间选课和成绩会怎样？','异动,学籍状态,在读,选课限制,成绩',
         '学籍状态决定业务资格：在读学生可选课、可参加考试；休学与保留学籍期间不开放新的选课资格；退学与转出学生不再纳入开课与选课范围。异动审批通过后学籍状态即时变更，已产生的历史选课与成绩记录保留不删除，供毕业审核与成绩统计使用。'
  UNION ALL SELECT 'EXAM','考试安排和考场座位是怎么确定的？','考试,考场,座位,编排,监考,蛇形,准考证',
         '考试按考试计划组织，计划发布前必须已产生考场座位或监考安排。座位支持一键自动编排：考生取自选课名单，按学号排序后蛇形排座（奇数列正序、偶数列反序并隔列入座），超出单个考场容量时自动按容量降序拆分到多个考场；总容量不足则拒绝编排并提示。监考按每考场一主一副派发，自动回避同场重复与同时段已有监考的教师。考试安排可在门户「考试安排」查询，并在「我的凭证」打印准考证。'
  UNION ALL SELECT 'EXAM','缓考、补考怎么办理？','缓考,补考,重修,考试类型,申请',
         '考试类型区分正常考核、补考、重修等，补考与重修成绩在成绩记录中按考试类型分别登记，互不覆盖。因故不能参加考试须在规定时间内向教务处提交申请并在管理端登记，未登记而缺考的成绩按缺考处理。补考的具体时间与考场由教务处统一安排后在考试计划中公布。'
  UNION ALL SELECT 'GRADUATION','毕业审核看哪些条件，多久审核一次？','毕业审核,学分,英语,体育,批量审核,预审,自动审核',
         '毕业审核比对四项：修读学分是否达到培养方案规定学分、是否存在不及格课程、外语类课程是否全部通过、体育类课程是否全部通过。课程判定按课程属性标记而非课程名称关键字，课程改名不影响判定；应修学分取自培养方案规定学分（可通过开关回退到历史口径）。审核支持单人与批量自动审核，逐人独立落库、单点失败不影响整体，并返回通过、不通过与明细，结果可导出。'
  UNION ALL SELECT 'GRADUATION','学生在毕业前能自己先查还差多少学分吗？','毕业预审,自助,差距,分项,达成,模拟',
         '可以。门户「毕业预审」提供实时模拟审核，只读不落库：展示总学分达成情况、四项毕业条件的逐项判定、按培养方案学分结构的分项达成率（如公共基础、专业、外语、体育各要求的学分与已获学分），并列出差距课程清单与预计结论。临近毕业建议多次查看，据此与学业导师确认修读计划。'
  UNION ALL SELECT 'GRADUATION','学位授予的审核条件是什么？','学位,GPA,2.0,学位课,论文,学术成果,配置',
         '学位审核条件可在「学位条件配置」中维护并设为默认生效项，包括绩点门槛（如不低于2.0）、是否要求学位课程无不及格、是否要求外语达标、是否要求毕业论文合格、是否要求学术成果。审核按生效配置动态判定各分项，论文项取毕业论文档案的当前届归档结论（已归档且合格才判通过），无档案、未归档或不合格均判不通过并在审核意见中说明。'
  UNION ALL SELECT 'WARNING','学业预警分几个等级，什么条件会触发？','学业预警,预警等级,GPA,学分,出勤,高危,综合预警',
         '预警按三类规则生成：绩度偏低（平均绩点低于设定阈值）、学分达成不足（已获学分占应获学分的比例低于阈值）、修读达标不足（以不及格课程门数作为代理指标，待接入考勤数据后替换为真实出勤）。预警类型分为成绩、学分、出勤、综合四类，等级分一般、严重、高危三级；同时命中两个及以上维度即判综合高危预警。预警记录生成前会清理未解除的旧记录，避免重复。'
  UNION ALL SELECT 'WARNING','收到预警后有什么帮扶措施，怎么解除？','预警帮扶,认领,跟踪,解除,通知,邮件,短信',
         '预警生成后系统按级别多渠道通知：站内信必发，邮件与短信按参数开关启用并逐渠道留痕。达到严重及以上等级的预警会自动派发帮扶任务（也可由辅导员或导师手动派发），任务状态从未认领、帮扶中到已完成或已关闭，帮扶人可在「预警帮扶」页认领、填报跟踪记录；每次跟踪都会记录时间与内容。帮扶完成时可勾选联动解除预警，解除后预警标记为已解除并记录解除日期与说明。'
  UNION ALL SELECT 'THESIS','毕业论文要经过哪些环节，哪些环节会被退回重做？','毕业论文,选题,开题,中期,查重,答辩,归档,抽检,环节',
         '毕业论文按六环节状态机推进：选题、开题、中期检查、查重、答辩、成绩归档，随后进入档案归档。学生先在门户从选题库选题（题目设有来源、难度、可选容量，超容量不可选），师生双选后建立论文档案；每个环节在线提交材料并由指导教师审核，审核不通过时退回同一环节重新提交。查重率超过阈值（可在参数中配置）会自动拦截不进入下一环节；已归档后档案锁定不可回退。抽检结果为系统内登记，与省级抽检平台的接口对接待立项。'
  UNION ALL SELECT 'CERTIFICATE','毕业证书、学位证书编号是怎么生成的，遗失了怎么补办？','证书编号,自动生成,唯一,补办,发放,结业',
         '证书编号按「院校码＋年份＋类型码＋流水号」分桶自动生成，服务层校验唯一性并配合数据库唯一索引双重保障，支持批量生成与发放登记，登记时记录发放日期与领取人。证书类型分毕业证书、学位证书、结业证书。遗失可在管理端发起补办申请，走提交、受理通过或驳回流程，受理通过后系统自动生成补办证书并向申请人推送站内消息。套打与打印模板统一由打印凭证模块维护。'
  UNION ALL SELECT 'OTHER','离校手续要办哪几项，会自动判定完成吗？','离校手续,图书馆,财务,宿管,一卡通,环节配置,自动判定',
         '离校手续的环节清单可在后台配置，不再固定为四项。每个环节绑定一个数据源（如图书借阅归还、财务结算、宿舍退宿、一卡通退还、毕业审核结论、学位审核结论），系统按对应数据源的结论自动判定该环节是否办结并重算整体手续状态，无需人工逐项改状态。整体转为已办结时向申请人推送站内消息。'
  UNION ALL SELECT 'OTHER','教务数据为什么和实际不一致，谁来维护基础数据？','基础数据,导入,教室,教师,课程库,学籍,校验报告',
         '课程库、教师、教室、学籍、培养方案五类基础数据均支持 Excel 模板导入，提供逐行校验报告：先校验必填格式，再校验外键是否存在（如教室所属教学楼、专业所属院系），最后校验业务唯一键（教师工号唯一、教室为教学楼加名称组合唯一、学号唯一且身份证号不与他人冲突，已毕业学生不被覆盖）。导入支持是否更新已有数据的选择，部分失败不回滚成功行并返回明细报告。发现不一致时按报告定位问题行修正后重新导入。'
) t
WHERE (SELECT COUNT(1) FROM sys_ai_knowledge) = 0;

-- ----------------------------
-- 6、菜单与权限
--    管理端：3160-3166（知识库）、3170-3172（问答留痕统计）
--    门户端：3180-3183（AI 问答 / 选课推荐 / 学业画像）
-- ----------------------------
-- 6.1 管理端 知识库
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3160, 'AI知识库', 1, 13, 'aiKnowledge', 'system/aiKnowledge/index', '', '', 1, 0, 'C', '0', '0', 'system:aiKnowledge:list', 'documentation', 'system', NOW(), '教务政策智能问答知识库维护'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3160);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3161, '知识查询', 3160, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiKnowledge:query', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3161);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3162, '知识新增', 3160, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiKnowledge:add', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3162);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3163, '知识修改', 3160, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiKnowledge:edit', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3163);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3164, '知识删除', 3160, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiKnowledge:remove', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3164);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3165, '知识导出', 3160, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiKnowledge:export', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3165);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3166, '问答自测', 3160, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiKnowledge:ask', '#', 'system', NOW(), '管理端知识库问答自测'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3166);

-- 6.2 管理端 问答留痕与统计
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3170, 'AI问答分析', 1, 14, 'aiChatStat', 'system/aiChatStat/index', '', '', 1, 0, 'C', '0', '0', 'system:aiChat:list', 'chart', 'system', NOW(), 'AI问答留痕、命中率与来源分布分析'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3170);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3171, '记录查询', 3170, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiChat:query', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3171);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3172, '记录导出', 3170, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiChat:export', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3172);

-- 6.3 门户端 AI 服务（学生服务分组下，教师通过角色菜单分配复用同一权限字符）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3180, 'AI智能问答', 2501, 11, 'aiChat', 'portal/aiChat/index', '', '', 1, 0, 'C', '0', '0', 'portal:ai:chat', 'message', 'system', NOW(), '教务政策智能问答'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3180);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3181, '提问', 3180, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'portal:ai:ask', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3181);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3182, 'AI选课推荐', 2501, 12, 'aiRecommend', 'portal/aiRecommend/index', '', '', 1, 0, 'C', '0', '0', 'portal:ai:recommend', 'star', 'system', NOW(), '个性化选课推荐'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3182);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3183, '学业画像', 2501, 13, 'aiProfile', 'portal/aiProfile/index', '', '', 1, 0, 'C', '0', '0', 'portal:ai:portrait', 'user', 'system', NOW(), '学生学业画像与分析建议'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3183);

-- ----------------------------
-- 7、角色授权
-- ----------------------------
-- 教务处管理员（3）：知识库 + 问答分析
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 3, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (3160, 3161, 3162, 3163, 3164, 3165, 3166, 3170, 3171, 3172)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 3 AND rm.menu_id = m.menu_id);

-- 学生（7）：问答 + 选课推荐 + 学业画像
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 7, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (3180, 3181, 3182, 3183)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 7 AND rm.menu_id = m.menu_id);

-- 教师（6）：仅开放智能问答
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 6, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (3180, 3181)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 6 AND rm.menu_id = m.menu_id);

-- 学院管理员（4）、教学秘书（5）：可查看知识库与问答分析（不含维护）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 4, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (3160, 3161, 3170, 3171)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 4 AND rm.menu_id = m.menu_id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 5, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (3160, 3161, 3170, 3171)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 5 AND rm.menu_id = m.menu_id);

-- ----------------------------
-- 8、验证
-- ----------------------------
SELECT '知识库表' AS item, COUNT(1) AS cnt FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_ai_knowledge'
UNION ALL SELECT '问答留痕表', COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_ai_chat_record'
UNION ALL SELECT '知识库种子', COUNT(1) FROM sys_ai_knowledge
UNION ALL SELECT 'AI参数', COUNT(1) FROM sys_config WHERE config_key LIKE 'ai.%'
UNION ALL SELECT 'AI字典明细', COUNT(1) FROM sys_dict_data WHERE dict_type IN ('sys_ai_category', 'sys_ai_answer_source')
UNION ALL SELECT 'AI菜单', COUNT(1) FROM sys_menu WHERE menu_id BETWEEN 3160 AND 3183
UNION ALL SELECT 'AI角色授权', COUNT(1) FROM sys_role_menu WHERE menu_id BETWEEN 3160 AND 3183;
