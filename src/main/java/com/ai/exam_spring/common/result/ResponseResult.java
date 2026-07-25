package com.ai.exam_spring.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
//封装响应结果
public class ResponseResult<T> {
    @Schema(description = "状态码", example = "200")
    private String code;

    @Schema(description = "响应信息", example = "操作成功")
    private String message;

    @Schema(description = "响应数据")
    private T data;


    public ResponseResult(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 响应成功
     * @param data
     * @return
     */
    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>("200", "操作成功", data);
    }
    /**
     * 响应成功
     * @param message
     * @param data
     * @return
     */
    public static <T> ResponseResult<T> success(String message,T data) {
        return new ResponseResult<>("200", message, data);
    }

    /**
     * 响应成功
     * @return
     */
    public static ResponseResult<Void> success() {
        return new ResponseResult<>("200", "操作成功");
    }

    /**
     * 响应成功
     * @param message
     * @return
     */
    public static ResponseResult<Void> success(String message) {
        return new ResponseResult<>("200", message);
    }




    /**
     * 响应失败
     *
     * @param message
     * @return
     */
    public static <T> ResponseResult<T> error(String message) {
        return new ResponseResult<>("500", message);
    }

    /**
     * 响应失败
     *
     * @param code
     * @param message
     * @return
     */
    public static <T> ResponseResult<T> error(String code, String message) {
        return new ResponseResult<>(code, message);
    }

    /**
     * 响应失败
     *
     * @return
     */
    public static ResponseResult<Void> error() {
        return new ResponseResult<>("500", "操作失败");
    }

}
