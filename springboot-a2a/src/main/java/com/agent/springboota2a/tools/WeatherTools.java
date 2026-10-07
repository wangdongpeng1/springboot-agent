package com.agent.springboota2a.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Define Tools
 */
@Service
public class WeatherTools {

    @Tool(description = "Get current weather for a location")
    public String getCurrentWeather(
            @ToolParam(description = "City and state, e.g. San Francisco, CA") String location) {
        return "Current weather in " + location + ": Sunny, 72°F";
    }
}
