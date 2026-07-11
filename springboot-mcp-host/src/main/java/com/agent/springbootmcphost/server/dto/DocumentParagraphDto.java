package com.agent.springbootmcphost.server.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentParagraphDto {

    private String id;

    private String title;

    /**
     * 命中的问
     */
    private String query;

    private List<String> paths;

    private List<String> splitContent;

    private List<String> similarQuery;

    private String libId;

    private String docId;

    private String categoryId;

    private String docName;

    private String content;

    private String html;

    private String fileExtension;

    private Double score;

    private String fileType;

    private String type;

    private Integer sort;

    private Map<String, ReferenceData> reference;

    /**
     * 前端溯源时对应原文的页码
     */
    private Integer tracePage;

    @Data
    public static class ReferenceData {

        private String content;

        private String format;

        private String suffix;

        private String link;
    }


}
