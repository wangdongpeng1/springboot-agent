package com.agent.springbootmcphost.client.dto;


import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

@Data
public class McpExecuteParam {
    private String url;
    private String toolName;
    private JSONObject input;
}
