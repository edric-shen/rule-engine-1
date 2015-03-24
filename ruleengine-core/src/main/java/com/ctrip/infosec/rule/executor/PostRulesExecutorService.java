/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctrip.infosec.rule.executor;

import com.ctrip.infosec.common.model.RiskFact;
import com.ctrip.infosec.configs.Configs;
import com.ctrip.infosec.configs.event.PostRule;
import com.ctrip.infosec.rule.Contexts;
import com.ctrip.infosec.rule.engine.StatelessPostRuleEngine;
import com.ctrip.infosec.sars.util.Collections3;
import com.ctrip.infosec.sars.util.SpringContextHolder;
import java.util.List;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author zhengby
 */
@Service
public class PostRulesExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(PostRulesExecutorService.class);

    /**
     * 执行预处理规则
     */
    public RiskFact executePostRules(RiskFact fact, boolean isAsync) {
        execute(fact, false);
        return fact;
    }

    /**
     * 串行执行
     */
    void execute(RiskFact fact, boolean isAsync) {

        // matchRules      
        List<PostRule> matchedRules = Configs.matchPostRules(fact, isAsync);
        List<String> packageNames = Collections3.extractToList(matchedRules, "ruleNo");
        logger.info(Contexts.getLogPrefix() + "matched post rules: " + packageNames.size());
        StatelessPostRuleEngine statelessPostRuleEngine = SpringContextHolder.getBean(StatelessPostRuleEngine.class);

        StopWatch clock = new StopWatch();
        try {
            clock.reset();
            clock.start();

            statelessPostRuleEngine.execute(packageNames, fact);

            clock.stop();
            long handlingTime = clock.getTime();
            if (handlingTime > 50) {
                logger.info(Contexts.getLogPrefix() + "postRules: " + packageNames + ", usage: " + handlingTime + "ms");
            }

        } catch (Throwable ex) {
            logger.warn(Contexts.getLogPrefix() + "invoke stateless post rule failed. packageNames: " + packageNames, ex);
        }
    }
}