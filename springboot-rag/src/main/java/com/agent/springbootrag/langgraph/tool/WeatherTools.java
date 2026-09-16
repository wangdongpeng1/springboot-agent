package com.agent.springbootrag.langgraph.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 定义 Tool
 */
@Component
public class WeatherTools {

    @Tool(description = "查询指定城市的当前天气")
    public String getWeather(
            @ToolParam(description = "城市名称，例如：北京") String city) {
        // 实际场景替换为真实 API
        return city + "：晴，25°C";
    }
}
