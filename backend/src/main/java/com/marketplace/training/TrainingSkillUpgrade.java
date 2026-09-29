package com.marketplace.training;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Preserve legacy program assignments while leaving existing unclassified programs available.
@Component @RequiredArgsConstructor @Order(-80)
public class TrainingSkillUpgrade implements ApplicationRunner {
    private final TrainingProgramRepository programs;
    @Override @Transactional public void run(ApplicationArguments args) {
        for(var p:programs.findAll()) {
            if(p.getSkill()!=null) { p.getSkills().add(p.getSkill()); p.setSkill(null); }
        }
    }
}
