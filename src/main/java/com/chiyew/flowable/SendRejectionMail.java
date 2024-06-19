package com.chiyew.flowable;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author Gincar
 * date 2024/6/20 0:15
 */
public class SendRejectionMail implements JavaDelegate {
    @Override
    public void execute(DelegateExecution delegateExecution) {
        System.out.println("驳回员工 "
                + delegateExecution.getVariable("employee") + "的请假申请，并发个邮件告知拒绝原因");
    }
}
