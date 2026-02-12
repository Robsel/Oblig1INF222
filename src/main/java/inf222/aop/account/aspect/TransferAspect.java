package inf222.aop.account.aspect;

import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import inf222.aop.account.Account;
import inf222.aop.account.annotation.Transfer;

@Aspect
public class TransferAspect {

    @Around("@annotation(transfer) && execution(* *(..))")
    public Object aroundTransfer(ProceedingJoinPoint jp, Transfer transfer) throws Throwable {
        MethodSignature sig = (MethodSignature) jp.getSignature();
        Method method = sig.getMethod();
        Object[] args = jp.getArgs();
        String[] paramNames = sig.getParameterNames();

        Logger logger = LoggerFactory.getLogger(method.getDeclaringClass());

        // International transfer log
        if (transfer.internationalTransfer()) {
            String msg = logInternationalTransfer(args);
            logAtLevel(logger, transfer.value(), msg);
        }

        // Transfer above threshold
        Double amount = extractAmount(args);
        if (amount != null && amount > transfer.LogTransferAbove()) {
            String msg = logTransferAbove(args, transfer.LogTransferAbove());
            logAtLevel(logger, transfer.value(), msg);
        }

        // Proceed and possibly log errors
        try {
            return jp.proceed();
        } catch (Throwable t) {
            if (transfer.logErrors()) {
                String params = buildParamsString(paramNames, args);
                String msg = logErrors(args, method.getName(), new String[] { params });
                logAtLevel(logger, transfer.value(), msg);
            }
            throw t;
        }
    }

    private void logAtLevel(Logger logger, Level level, String message) {
        if (level == null) level = Level.INFO;
        switch (level) {
            case TRACE -> logger.trace(message);
            case DEBUG -> logger.debug(message);
            case INFO  -> logger.info(message);
            case WARN  -> logger.warn(message);
            case ERROR -> logger.error(message);
            default -> logger.info(message);
        }
    }

    private Double extractAmount(Object[] args) {
        if (args == null || args.length < 3) return null;
        Object o = args[2];
        if (o instanceof Number) return ((Number) o).doubleValue();
        return null;
    }

    private String buildParamsString(String[] names, Object[] args) {
        if (args == null) return "";
        if (names == null) {
            return IntStream.range(0, args.length)
                    .mapToObj(i -> String.valueOf(args[i]))
                    .collect(Collectors.joining(", "));
        }
        return IntStream.range(0, args.length)
                .mapToObj(i -> names.length>i && names[i]!=null ? names[i] + "=" + String.valueOf(args[i]) : String.valueOf(args[i]))
                .collect(Collectors.joining(", "));
    }

    private String logInternationalTransfer(Object[] methodArgs) {
        Account from = methodArgs != null && methodArgs.length>0 && methodArgs[0] instanceof Account ? (Account) methodArgs[0] : null;
        Account to = methodArgs != null && methodArgs.length>1 && methodArgs[1] instanceof Account ? (Account) methodArgs[1] : null;
        Double amount = extractAmount(methodArgs);
        String fromName = from!=null?from.getAccountName():"?";
        String toName = to!=null?to.getAccountName():"?";
        String fromCurr = from!=null && from.getCurrency()!=null?from.getCurrency().name():"?";
        String toCurr = to!=null && to.getCurrency()!=null?to.getCurrency().name():"?";
        return String.format("International transfer from %s to %s, %s %s converted to %s", fromName, toName, amount, fromCurr, toCurr);
    }

    private String logTransferAbove(Object[] methodArgs, double value) {
        Account from = methodArgs != null && methodArgs.length>0 && methodArgs[0] instanceof Account ? (Account) methodArgs[0] : null;
        Account to = methodArgs != null && methodArgs.length>1 && methodArgs[1] instanceof Account ? (Account) methodArgs[1] : null;
        Double amount = extractAmount(methodArgs);
        String fromName = from!=null?from.getAccountName():"?";
        String toName = to!=null?to.getAccountName():"?";
        return String.format("Transfer above %s from %s to %s, amount: %s", value, fromName, toName, amount);
    }

    private String logErrors(Object[] methodArgs, String methodName, String[] methodParams) {
        Account from = methodArgs != null && methodArgs.length>0 && methodArgs[0] instanceof Account ? (Account) methodArgs[0] : null;
        Account to = methodArgs != null && methodArgs.length>1 && methodArgs[1] instanceof Account ? (Account) methodArgs[1] : null;
        Double amount = extractAmount(methodArgs);
        String fromName = from!=null?from.getAccountName():"?";
        String toName = to!=null?to.getAccountName():"?";
        String params = (methodParams != null && methodParams.length>0) ? methodParams[0] : "";
        return String.format("Error in transfer from %s to %s, amount: %s %s, method: %s(%s)", fromName, toName, amount, from!=null && from.getCurrency()!=null?from.getCurrency().name():"?", methodName, params);
    }
}
