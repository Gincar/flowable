package com.chiyew.flowable;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author Gincar
 * date 2024/6/20 0:10
 */
public class CallExternalSystemDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution delegateExecution) {
        System.out.println("跳转到其他系统或者调用其他服务完成员工 "
                + delegateExecution.getVariable("employee") + "的请假流程其他事项");
    }
}
