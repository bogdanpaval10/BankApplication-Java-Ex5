package com.luxoft.bankapp.service.audit;

import com.luxoft.bankapp.model.Account;
import com.luxoft.bankapp.model.Client;
import com.luxoft.bankapp.service.audit.events.BalanceEvent;
import com.luxoft.bankapp.service.audit.events.DepositEvent;
import com.luxoft.bankapp.service.audit.events.WithdrawEvent;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
@Aspect
public class AuditAspect {
    @Autowired
    private ApplicationContext applicationContext;


    @Before("anyDeposit()")
    public void logDeposit(JoinPoint joinPoint) {
        Object[] methodArgs = joinPoint.getArgs();

        //audit.auditDeposit(getAccountId(methodArgs), (double) methodArgs[1]);
        applicationContext.publishEvent(new DepositEvent(getAccountId(methodArgs), (double) methodArgs[1]));
    }

    @Pointcut("execution(* com.luxoft.bankapp.service.operations.*.deposit(..))")
    public void anyDeposit() {}

    @Around("anyWithdraw()")
    public Object logWithdrawal(ProceedingJoinPoint thisJoinPoint) throws Throwable {
        Object[] methodArgs = thisJoinPoint.getArgs();

        long accountId = getAccountId(methodArgs);

        //audit.auditWithdraw(accountId, (double) methodArgs[1], WithdrawState.TRYING);
        applicationContext.publishEvent(new WithdrawEvent(getAccountId(methodArgs), (double) methodArgs[1]));

        Object result;

        try {
            result = thisJoinPoint.proceed();

            //audit.auditWithdraw(accountId, (double) methodArgs[1], WithdrawState.SUCCESSFUL);
            applicationContext.publishEvent(new WithdrawEvent(getAccountId(methodArgs), (double) methodArgs[1],
                    WithdrawEvent.State.SUCCESSFUL));
        } catch (Exception e) {
            //audit.auditWithdraw(accountId, (double) methodArgs[1], WithdrawState.FAILED);
            applicationContext.publishEvent(new WithdrawEvent(getAccountId(methodArgs), (double) methodArgs[1],
                    WithdrawEvent.State.FAILED));

            throw e;
        }

        return result;
    }

    @Pointcut("execution(* com.luxoft.bankapp.service.operations.*.withdraw(..))")
    public void anyWithdraw() {}

    @Before("anyBalance()")
    public void logBalance(JoinPoint joinPoint) {
        Object[] methodArgs = joinPoint.getArgs();

        //audit.auditBalance(getAccountId(methodArgs));
        applicationContext.publishEvent(new BalanceEvent(getAccountId(methodArgs)));
    }

    @Pointcut("execution(* com.luxoft.bankapp.service.operations.*.getBalance(..))")
    public void anyBalance() {}

    private long getAccountId(Object[] methodArgs) {
        Account account;

        if (methodArgs[0] instanceof Client) {
            account = ((Client) methodArgs[0]).getActiveAccount();
        } else {
            account = (Account) methodArgs[0];
        }

        return account.getId();
    }

}
