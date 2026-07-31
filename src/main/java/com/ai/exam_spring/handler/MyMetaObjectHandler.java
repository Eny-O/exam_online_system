package com.ai.exam_spring.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
/**
 * 自定义元数据对象处理器
 * 实现基于mybatis的创建时间和更新时间的自动填充
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /*
      注意:
      MetaObjectHandler只对通过 MyBatis-Plus 提供的方法（insert、updateById 等）生效。
      如果你写了自定义 SQL（比如手写 @Update 注解的 mapper 方法），自动填充不会触发，
      那种情况就得自己处理了。
     */

    /**
     * strictInsertFill 只在字段为 null 时才填，不会覆盖你手动设的值。
     * @param metaObject
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * strictUpdateFill 同理，只在 update 操作时触发。
     * @param metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
