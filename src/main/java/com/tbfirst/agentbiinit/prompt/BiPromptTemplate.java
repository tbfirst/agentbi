package com.tbfirst.agentbiinit.prompt;

/**
 * 智能BI的提示词模板 - RAG增强版
 *
 * 【重要】使用 RAG 后，知识库内容会自动注入到 UserMessage 中，
 * 因此 SystemPrompt 主要关注行为控制，UserPrompt 提供结构化指导
 */
public class BiPromptTemplate {

    // 初始系统提示词模板
    private static final String SYSTEM_PROMPT_TEMPLATE = """
        你是一位专业的数据分析师和可视化专家。请严格遵循以下规则：

        1. 【数据安全】禁止在输出中包含原始CSV数据的具体数值
        2. 【代码规范】ECharts配置必须是标准JSON格式，禁止包含函数、注释或变量声明
        3. 【分析深度】必须提供数据洞察，而非简单描述
        4. 【格式严格】必须按指定的JSON Schema输出，禁止额外字段
        """;

    /**
     * 系统提示词 - 控制模型行为
     * 知识库内容会通过 RetrievalAugmentor 注入到 UserMessage 中
     */
//    private static final String SYSTEM_PROMPT_TEMPLATE = """
//        你是一位专业的数据分析师和 ECharts 可视化专家。
//
//        【核心职责】
//        分析用户提供的 CSV 数据，生成准确的 ECharts 配置和数据分析结论。
//
//        【代码规范】
//        1. 生成的 ECharts option 必须是标准 JSON 格式
//        2. 禁止包含函数、注释或变量声明
//        3. 严格遵循 ECharts 5.x 官方配置项命名（区分大小写）
//        4. 必须按指定的JSON Schema输出，禁止额外字段
//
//        【数据安全】
//        禁止在分析结论中暴露原始数据的具体敏感数值
//
//        【RAG 知识使用规范】
//        1. 严格使用知识库中提供的 ECharts 配置项和 API
//        2. 优先参考知识库中的示例代码结构
//        3. 禁止编造知识库中未提及的配置项
//        """;

    // 初始用户提示词模板（根据 csvdata 数据直接生成 ECharts 配置）
    private static final String USER_PROMPT_TEMPLATE = """
        ## 输入数据

        ### 1. 数据内容（CSV格式）
        {csvData}

        ### 2. 用户需求
        {goal}

        ### 3. 指定图表类型
        {chartType}

        ### 4. 图表名称
        {name}

        ## 任务要求

        请分析上述数据并生成ECharts配置。
        【ECharts 配置规范】
        必须严格遵循以下字段命名（区分大小写）：
        - xAxis（X轴配置，注意大写A）
        - yAxis（Y轴配置，注意大写A）
        - series（系列数据）
        - title（标题）
        - legend（图例）
        - tooltip（提示框）

        必须严格按照以下JSON Schema输出：

        ```json
        {
          "analysis": {
            "summary": "string, 数据整体概况（2-3句）",
            "keyFindings": ["string, 关键发现1", "string, 关键发现2", "string, 关键发现3"],
            "suggestions": ["string, 优化建议1", "string, 优化建议2"]
          },
          "chartConfig": {
            "title": {
              "text": "string, 使用指定的图表名称",
              "subtext": "string, 可选副标题，基于数据特征生成"
            },
            "tooltip": {
              "trigger": "string, 根据图表类型：axis/item"
            },
            "legend": {
              "show": "boolean",
              "position": "string, top/bottom/left/right"
            },
            "xAxis": {
              "type": "category/value/time",
              "name": "string, 维度名称",
              "data": ["string, 维度值1", "string, 维度值2"]
            },
            "yAxis": {
              "type": "value/category",
              "name": "string, 指标名称"
            },
            "series": [
              {
                "name": "string, 系列名称",
                "type": "string, 必须与指定图表类型一致",
                "data": [number, number, number]
              }
            ],
            "color": ["string, 配色方案，如#5470c6"]
          },
          "dataMapping": {
            "dimensionField": "string, 使用的维度字段名",
            "metricFields": ["string, 使用的指标字段名"],
            "rowCount": "number, 数据行数"
          }
        }
        ```

        ## 重要约束

        1. chartConfig必须是合法的ECharts option配置，前端可直接调用echarts.setOption()
        2. 禁止在series.data中嵌入原始CSV数据，必须是从CSV解析出的数值数组
        3. 如果图表类型是'pie'，则不需要xAxis/yAxis，series.data格式为[{name: "类别", value: 数值}]
        4. 如果用户需求不明确，基于数据特征智能推断分析维度
        5. 所有字符串必须使用双引号，禁止单引号

        ## 输出示例（bar图表类型）

        ```json
        {
          "analysis": {
            "summary": "该数据展示了各产品线的季度销售表现，整体呈现增长趋势。",
            "keyFindings": [
              "产品线A在Q3出现显著峰值，环比增长45%",
              "产品线B表现平稳，但Q4略有下滑",
              "产品线C作为新品，增长潜力最大"
            ],
            "suggestions": [
              "建议深入分析产品线A在Q3的营销活动",
              "关注产品线B的Q4下滑原因，制定挽回策略"
            ]
          },
          "chartConfig": {
            "title": {"text": "季度销售分析", "subtext": "2024年各产品线表现"},
            "tooltip": {"trigger": "axis"},
            "legend": {"show": true, "position": "top"},
            "xAxis": {"type": "category", "name": "季度", "data": ["Q1", "Q2", "Q3", "Q4"]},
            "yAxis": {"type": "value", "name": "销售额（万元）"},
            "series": [
              {"name": "产品线A", "type": "bar", "data": [120, 132, 191, 134]},
              {"name": "产品线B", "type": "bar", "data": [220, 182, 191, 234]},
              {"name": "产品线C", "type": "bar", "data": [150, 232, 201, 154]}
            ],
            "color": ["#5470c6", "#91cc75", "#fac858"]
          },
          "dataMapping": {
            "dimensionField": "quarter",
            "metricFields": ["product_a", "product_b", "product_c"],
            "rowCount": 4
          }
        }
        ```

        请现在分析数据并输出JSON：
        """;

     // 初始用户提示词模板（根据 csvurl 从阿里云oss下载数据直接生成 ECharts 配置）
    private static final String USER_PROMPT_TEMPLATE_BY_CSV_URL = """
        ## 数据源说明

        ### 1. CSV数据文件URL（可直接下载访问）
        {csvUrl}

        ### 2. 用户需求
        {goal}

        ### 3. 指定图表类型
        {chartType}

        ### 4. 图表名称
        {name}

        ## 任务要求

        请执行以下步骤：
        1. 从提供的CSV URL下载并解析数据
        2. 分析数据内容
        3. 根据用户需求生成ECharts配置

        【ECharts 配置规范】
        必须严格遵循以下字段命名（区分大小写）：
        - xAxis（X轴配置，注意大写A）
        - yAxis（Y轴配置，注意大写A）
        - series（系列数据）
        - title（标题）
        - legend（图例）
        - tooltip（提示框）

        必须严格按照以下JSON Schema输出：

        ```json
        {
          "analysis": {
            "summary": "string, 数据整体概况（2-3句）",
            "keyFindings": ["string, 关键发现1", "string, 关键发现2", "string, 关键发现3"],
            "suggestions": ["string, 优化建议1", "string, 优化建议2"]
          },
          "chartConfig": {
            "title": {
              "text": "string, 使用指定的图表名称",
              "subtext": "string, 可选副标题，基于数据特征生成"
            },
            "tooltip": {
              "trigger": "string, 根据图表类型：axis/item"
            },
            "legend": {
              "show": "boolean",
              "position": "string, top/bottom/left/right"
            },
            "xAxis": {
              "type": "category/value/time",
              "name": "string, 维度名称",
              "data": ["string, 维度值1", "string, 维度值2"]
            },
            "yAxis": {
              "type": "value/category",
              "name": "string, 指标名称"
            },
            "series": [
              {
                "name": "string, 系列名称",
                "type": "string, 必须与指定图表类型一致",
                "data": [number, number, number]
              }
            ],
            "color": ["string, 配色方案，如#5470c6"]
          },
          "dataMapping": {
            "dimensionField": "string, 使用的维度字段名",
            "metricFields": ["string, 使用的指标字段名"],
            "rowCount": "number, 数据行数"
          }
        }
        ```
        ## 重要约束
        1. 必须从CSV URL下载数据并解析后再生成配置
        2. chartConfig必须是合法的ECharts option配置，前端可直接调用echarts.setOption()
        3. 禁止在series.data中嵌入原始CSV数据，必须是从CSV解析出的数值数组
        4. 如果图表类型是'pie'，则不需要xAxis/yAxis，series.data格式为[{name: "类别", value: 数值}]
        5. 如果用户需求不明确，基于数据特征智能推断分析维度
        6. 所有字符串必须使用双引号，禁止单引号
        7. 确保正确处理CSV文件中的表头和数据行

        ## 输出示例（bar图表类型）

        ```json
        {
          "analysis": {
            "summary": "该数据展示了各产品线的季度销售表现，整体呈现增长趋势。",
            "keyFindings": [
              "产品线A在Q3出现显著峰值，环比增长45%",
              "产品线B表现平稳，但Q4略有下滑",
              "产品线C作为新品，增长潜力最大"
            ],
            "suggestions": [
              "建议深入分析产品线A在Q3的营销活动",
              "关注产品线B的Q4下滑原因，制定挽回策略"
            ]
          },
          "chartConfig": {
            "title": {"text": "季度销售分析", "subtext": "2024年各产品线表现"},
            "tooltip": {"trigger": "axis"},
            "legend": {"show": true, "position": "top"},
            "xAxis": {"type": "category", "name": "季度", "data": ["Q1", "Q2", "Q3", "Q4"]},
            "yAxis": {"type": "value", "name": "销售额（万元）"},
            "series": [
              {"name": "产品线A", "type": "bar", "data": [120, 132, 191, 134]},
              {"name": "产品线B", "type": "bar", "data": [220, 182, 191, 234]},
              {"name": "产品线C", "type": "bar", "data": [150, 232, 201, 154]}
            ],
            "color": ["#5470c6", "#91cc75", "#fac858"]
          },
          "dataMapping": {
            "dimensionField": "quarter",
            "metricFields": ["product_a", "product_b", "product_c"],
            "rowCount": 4
          }
        }
        ```

        请现在分析数据并输出JSON：
        """;

    /**
     * 用户提示词模板（CSV URL 版本）
     * 注意：此模板内容会被 RetrievalAugmentor 增强，在前面注入知识库内容
     */
//    private static final String USER_PROMPT_TEMPLATE_BY_CSV_URL = """
//        ## 图表需求
//        名称: {name}
//        类型: {chartType}
//        目标: {goal}
//
//        ## 数据源
//        CSV文件URL: {csvUrl}
//
//        ## 任务要求
//        1. 从 CSV URL 下载并解析数据
//        2. 根据数据特征选择合适的坐标轴类型（category/value/time）
//        3. 结合【知识库参考】中的配置示例生成 ECharts option
//        4. 提供数据分析结论（关键发现 + 优化建议）
//
//        ## 输出格式
//        必须严格按照以下JSON Schema输出：
//
//        ```json
//        {
//          "analysis": {
//            "summary": "string, 数据整体概况（2-3句）",
//            "keyFindings": ["string, 关键发现1", "string, 关键发现2"],
//            "suggestions": ["string, 优化建议1", "string, 优化建议2"]
//          },
//          "chartConfig": {
//            "title": {"text": "string", "subtext": "string"},
//            "tooltip": {"trigger": "axis/item"},
//            "legend": {"show": true, "position": "top"},
//            "xAxis": {"type": "category/value/time", "name": "string", "data": []},
//            "yAxis": {"type": "value", "name": "string"},
//            "series": [{"name": "string", "type": "bar/line/pie", "data": []}],
//            "color": ["#5470c6", "#91cc75"]
//          },
//          "dataMapping": {
//            "dimensionField": "string",
//            "metricFields": ["string"],
//            "rowCount": 0
//          }
//        }
//        ```
//
//        ## 重要约束
//        1. chartConfig 必须是合法的 ECharts option 配置
//        2. 如果图表类型是'pie'，不需要 xAxis/yAxis，series.data 格式为 [{name: "类别", value: 数值}]
//        3. 所有字符串使用双引号，禁止单引号
//        4. 必须参考【知识库参考】中的配置项，确保准确性
//        """;

    /**
     * 构建用户提示词
     * @param csvData 数据内容（CSV格式）
     * @param goal 用户需求
     * @param chartType 指定图表类型
     * @param name 图表名称
     * @return 用户提示词
     */
    public static String buildUserPrompt(String csvData, String goal,
                                     String chartType, String name) {
        return USER_PROMPT_TEMPLATE
                .replace("{csvData}", csvData)
                .replace("{goal}", goal)
                .replace("{chartType}", chartType)
                .replace("{name}", name);
    }

    /**
     * 构建用户提示词（基于CSV URL）
     * @param csvUrl 数据URL（CSV格式）
     * @param goal 用户需求
     * @param chartType 指定图表类型
     * @param name 图表名称
     * @return 用户提示词
     */
    // 做空值处理
    public static String buildUserPromptByCsvUrl(String csvUrl, String goal,
                                                 String chartType, String name) {
        return USER_PROMPT_TEMPLATE_BY_CSV_URL
                .replace("{csvUrl}", nullToEmpty(csvUrl))
                .replace("{goal}", nullToEmpty(goal))
                .replace("{chartType}", nullToEmpty(chartType))
                .replace("{name}", nullToEmpty(name));
    }

    private static String nullToEmpty(String str) {
        return str == null ? "" : str;
    }

    public static String getSystemPrompt(){
        return SYSTEM_PROMPT_TEMPLATE;
    }

}

