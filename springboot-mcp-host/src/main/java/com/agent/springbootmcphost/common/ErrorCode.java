package com.agent.springbootmcphost.common;

import lombok.Getter;

/**
 * @author renq
 */

@Getter
public enum ErrorCode {
    SUCCESS("请求成功", 0),
    FAILURE("请求异常", -1),
    TIME_OUT("请求超时", -2),
    PARAM_ERROR("参数异常", -100),
    PARAM_IS_NULL("参数不能为空", -101),
    PARAM_VALID_ERROR("参数校验失败", -102),
    PARAM_ILLEGAL("非法的请求参数", -103),
    ENTITY_NOT_FOUND("未找到数据", -201),
    ENTITY_EXIST("数据已存在，【%s】字段重复", -202),
    DATABASE_CONNECTION_ERROR("数据库连接异常", -203),
    DATABASE_SQL_ERROR("数据库sql异常", -204),
    NOT_LOGGED_IN("用户登录失效或者未登录", -301),
    NOT_AUTHORITY_OPERATE("当前数据没有权限操作", -302),
    NOT_CURRENT_TENANT_USER("非创建者租户成员", -303),
    PLANET_API_ERROR("星球API异常", -600),
    PARAM_ACTION_CODE_ILLEGAL("服务编码必须是数字字母或者中划线下划线", -601),
    UNSUPPORTED_TYPE("不支持的 type 类型", -602),
    MISSING_PARAMETER("缺少请求参数", -603),
    BLANK_IP_ADDRESS("ip 地址是空", -604),
    DATA_BLANK("数据为空", -1001),
    DATA_NOT_EXIST("数据不存在", -1002),
    DATA_ALREADY_EXIST("系统已经存在", -1005),
    USER_ROLES_NOT_FIT("当前用户权限不支持此功能", -1006),
    ASSISTANT_STATUS_CHANGED("助手状态已发生变化,无法操作", -2001),
    ASSISTANT_STATUS_NO_SUPPORT("助手状态不支持当前操作", -2002),
    ASSISTANT_STATUS_PUBLISHED("助手状态已是发布状态", -2011),
    ASSISTANT_STATUS_TO_BE_RELEASED("助手状态已是待发布状态", -2012),
    ASSISTANT_STATUS_IN_REVIEW("助手状态已是审核中状态", -2013),
    ASSISTANT_STATUS_REJECT("助手状态已是拒绝状态", -2014),
    ASSISTANT_STATUS_RECALL("助手状态已是撤回状态", -2015),
    ASSISTANT_VERSION_STATUS_UNPUBLISHED("助手没有已发布的版本,不能发布", -2016),
    ASSISTANT_VERSION_STATUS_UNPUBLISHED_SYNC("助手没有已发布的版本,不能同步", -2017),
    ASSISTANT_NO_CHANGED("助手状态未变化,无需操作", -2018),
    ASSISTANT_NOT_EXIST("助手不存在", -2020),
    ASSISTANT_ORIGIN_NOT_EXIST("源助手不存在", -2021),
    ASSISTANT_NAME_EXIST("助手名称已存在", -2022),
    ASSISTANT_KIND_CODE_NOT_EXIST("助手种类信息有误,请检查数据", -2023),
    ASSISTANT_LABEL_NOT_EXIST("助手标签不存在或者已被删除", -2024),
    ASSISTANT_EXIST("内置助手已创建", -2025),
    ASSISTANT_SUB_KIND_CODE_NOT_EXIST("助手种类子类信息有误,请检查数据", -2026),
    ASSISTANT_VERSION_NOT_EXIST("助手版本不存在或者已被删除", -2100),
    ASSISTANT_VERSION_STATUS_NO_ENABLED("助手版本未启用", -2101),
    ASSISTANT_VERSION_PROMPT_EMPTY("助手指令信息不能为空", -2110),
    ASSISTANT_VERSION_PROMPT_FORMAT_ERROR("助手指令内容格式错误", -2111),
    APP_ASSISTANT_NOT_EXIST("应用关联助手未同步或者已被删除", -3001),
    VERIFY_STATUS_TO_AUDIT("当前审核状态已是待审核", -4000),
    VERIFY_STATUS_NOT_TO_AUDIT("当前审核状态不是待审核状态，无法撤回", -4000),
    VERIFY_STATUS_PASS("当前审核状态已审批通过", -4001),
    VERIFY_STATUS_CANCEL("当前审核状态已撤销", -4002),
    VERIFY_STATUS_REJECT("当前审核状态已驳回", -4003),
    SHARE_NOT_EXIST("分享不存在", -5000),
    SHARE_EXPIRED("分享已失效", -5001),
    SHARE_SECRET_CODE_ERROR("分享失效", -5002),
    SHARE_LIMIT_COUNT_ERROR("分享超过使用人数限制", -5003),
    TENANT_ID_EMPTY("租户ID不能为空", -5004),
    ASSISTANT_STATUS_PUBLISHED_UPDATE("助手状态已是更新审核状态", -5005),
    ASSISTANT_STATUS_NOT_PUBLISHED("当前状态非已发布状态", -5006),
    ASSISTANT_PUBLISHED_UPDATE_REVIEW("助手状态已是更新撤回状态", -5007),
    ASSISTANT_NOT_PUBLISHED_UPDATE_REVIEW("当前状态非更新状态，无法撤回", -5008),
    ASSISTANT_PUBLISHED_UPDATE_REJECT("当前状态已是更新拒绝状态", -5009),
    ASSISTANT_PUBLISHED_IN_REVIEW("助手状态已是审核-复核状态", -5010),
    DOC_LIB_NO_CHANGED("知识库同步状态未变化,无需操作", -6002),
    DOC_LIB_STATUS_ENABLE_SYNC("知识库没有已启用的版本,不能同步", -6003),
    MODEL_NOT_EXIST("模型不存在", -6004)
    ;

    private final int code;
    private final String value;

    ErrorCode(String value, int code) {
        this.value = value;
        this.code = code;
    }

    public static ErrorCode get(int code) {
        for (ErrorCode value : ErrorCode.values()) {
            if (value.code == code) {
                return value;
            }
        }
        return ErrorCode.FAILURE;
    }

    public int getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }
}
