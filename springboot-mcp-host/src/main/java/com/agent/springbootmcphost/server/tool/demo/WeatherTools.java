package com.agent.springbootmcphost.server.tool.demo;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpAsyncRequestContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
public class WeatherTools {

    @McpTool(
            name = "getCurrentWeather",
            description = "获取指定城市的当前天气信息"
    )
    public Mono<String> getCurrentWeather(
            @McpToolParam(description = "城市名称", required = true) String city) {

        String[] weathers = {"晴天", "多云", "阴天", "小雨", "中雨"};
        String weather = weathers[new Random().nextInt(weathers.length)];
        int temperature = 15 + new Random().nextInt(20);

        return Mono.just(String.format(
                "【%s】当前天气：%s，温度：%d°C，更新时间：%s",
                city, weather, temperature,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        ));
    }

    @McpTool(name = "batchWeatherQuery", description = "批量查询多个城市的天气信息")
    public Flux<String> batchWeatherQuery(
            McpAsyncRequestContext context,
            @McpToolParam(description = "城市列表", required = true) List<String> cities) {

        // 解析城市列表
        String[] cityArray = cities.toArray(String[]::new);
        int totalCities = cityArray.length;

        Object progressToken = context.request().progressToken();

        return Flux.fromArray(cityArray)
                .concatMap(city -> {
                    int cityIndex = java.util.Arrays.asList(cityArray).indexOf(city) + 1;

                    // 生成随机天气数据
                    String[] weathers = {"晴天", "多云", "阴天", "小雨", "中雨", "大雨", "雪"};
                    String weather = weathers[new Random().nextInt(weathers.length)];
                    int temperature = 5 + new Random().nextInt(25); // 5-30度范围

                    // 发送进度信息
                    if (progressToken != null) {
                        double progress = (double) cityIndex / totalCities;
                        return context.progress(p ->
                                p.progress(progress)
                                        .total(1.0)
                                        .message(String.format(
                                                "【%s】当前天气：%s，温度：%d°C，更新时间：%s",
                                                city.trim(), weather, temperature,
                                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
                    }
                    return Flux.empty();
                })
                .thenMany(Flux.just("Complete"));
    }

}
