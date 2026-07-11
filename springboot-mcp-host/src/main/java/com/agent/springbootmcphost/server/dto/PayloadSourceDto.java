package com.agent.springbootmcphost.server.dto;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.*;

import java.util.List;

/**
 * @author wangdp
 * Created on 2026/3/18.
 * Description:
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayloadSourceDto {
    public PayloadChoices choices;
    public JSONArray sources;
    public String answerSource;
    public List<String> querykeywords;
    public String query;

    @Data
    public static class PayloadChoices {
        public int status;
        public int seq;
        public List<ChoicesText> text;
    }

    @Data
    public static class ChoicesText {
        public String role;
        public String content;
        public String content_type;
        public JSONObject asyncInfo;
        public int index;
    }

}
