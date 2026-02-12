package inf222.aop.measures;


import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class MeasureAspect {
    private final String regex;
    private final Pattern unitpattern;

    private final Map<String, Double> toMeter = new HashMap<String, Double>(Map.of(
            "m", 1d,
            "ft", 0.3048d,
            "in", 0.0254d,
            "cm", 0.01d,
            "yd", 0.9144d));

    public MeasureAspect() {
        String elems = String.join("|", toMeter.keySet());
        regex = String.format(".*_(%s)", elems);
        unitpattern = Pattern.compile(regex);
    }

    @Around("get(double inf222.aop.measures.Measures.*)")
    public double convertFieldGet(ProceedingJoinPoint jp) throws Throwable {
       
        double value = (double) jp.proceed();
        Matcher matcher = unitpattern.matcher(jp.getSignature().getName());
        if (matcher.find()) {
            String toBeConverted = matcher.group(1);
            double conversionFactor = toMeter.get(toBeConverted);
            return value * conversionFactor;
        }
        return value;
    }

    @Around("set(double inf222.aop.measures.Measures.*) && args(value) &&!cflow(execution(inf222.aop.measures.Measures.new(..)))")
    public void convertFieldSet(ProceedingJoinPoint jp, double value) throws Throwable {

        Matcher matcher = unitpattern.matcher(jp.getSignature().getName());
        if (matcher.find()){
            String toBeConverted = matcher.group(1);
            double conversionFactor = toMeter.get(toBeConverted);
            jp.proceed(new Object[]{value / conversionFactor});  
        } 
        
        
    }

    @Before("set(double inf222.aop.measures.Measures.*) &&args(value)")
    public void negaThrow(JoinPoint jp, double value) throws Throwable{
        if(value<0){
            throw new Error("Illegal modification");
        }
    }
}
