package com.tbfirst.agentbiinit.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * AI分析图表的返回结构
 * 要对应提示词模板
 */
@Data
@Schema(description = "AI分析响应结构")
public class AiAnalysisVO {

    @Schema(description = "数据分析结果")
    private Analysis analysis;

    @Schema(description = "ECharts配置")
    private ChartConfig chartConfig;

    @Schema(description = "数据映射信息")
    private DataMapping dataMapping;

    @Data
    public static class Analysis {
        @Schema(description = "整体概况", example = "数据整体呈现上升趋势...")
        private String summary;

        @Schema(description = "关键发现")
        private List<String> keyFindings;

        @Schema(description = "优化建议")
        private List<String> suggestions;
    }

    @Data
    public static class ChartConfig {
        private Title title;
        private Tooltip tooltip;
        private Legend legend;
        private Axis xAxis;
        private Axis yAxis;
        private List<Series> series;
        private List<String> color;

        @Data
        public static class Title {
            private String text;
            private String subtext;
        }

        @Data
        public static class Tooltip {
            private String trigger; // axis/item
        }

        @Data
        public static class Legend {
            private boolean show;
            private String position;
        }

        @Data
        public static class Axis {
            private String type; // category/value/time
            private String name;
            private List<String> data; // 仅category类型需要
        }

        @Data
        public static class Series {
            private String name;
            private String type; // bar/line/pie/scatter...
            private List<Object> data; // 数值数组或{name,value}对象数组
        }
    }

    @Data
    public static class DataMapping {
        private String dimensionField;
        private List<String> metricFields;
        private Integer rowCount;
    }
}
