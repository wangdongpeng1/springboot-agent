package com.agent.springbootmcphost.server.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 工具类
 * @author wangdp
 */
@Slf4j
@Component
public class ToolUtils {

    /**
     * 从 SSE (Server-Sent Events) 格式中提取 JSON 数据
     *
     * @param sseData SSE 格式的响应数据，格式如：data:{...json...}\n\n
     * @return 提取出的 JSON 字符串
     * @throws RuntimeException 如果 SSE 数据为空或无法提取 JSON
     */
    public static String extractJsonFromSse(String sseData) {
        if (sseData == null || sseData.trim().isEmpty()) {
            log.error("SSE 数据为空");
            throw new RuntimeException("SSE 数据为空");
        }

        log.debug("SSE 格式的响应数据: {}", sseData);

        // 使用正则提取 data: 后面的内容
        var matcher = Pattern.compile("data:\\s*(\\{.*\\})")
                .matcher(sseData);

        if (matcher.find()) {
            return matcher.group(1);
        }

        log.error("无法从 SSE 响应中提取 JSON 数据: {}", sseData);
        throw new RuntimeException("无法从 SSE 响应中提取 JSON 数据: " + sseData);
    }

}
