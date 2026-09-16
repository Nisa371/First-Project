package com.marketplace.skill;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.skills.seed-catalog", havingValue="true", matchIfMissing=true)
@RequiredArgsConstructor
public class SkillCatalog implements ApplicationRunner {
    private final SkillRepository skills;
    @Override @Transactional public void run(ApplicationArguments args) {
        for(String[] item : new String[][]{{"Java","TECH"},{"React","TECH"},{"UI/UX Design","TECH"},{"Data Analysis","TECH"},
            {"Driving","TRADE"},{"Electrical Work","TRADE"},{"Plumbing","TRADE"},{"Vehicle Repair","TRADE"}}) {
            if(skills.findByNameIgnoreCase(item[0]).isEmpty()) {
                var skill=new Skill(); skill.setName(item[0]); skill.setCategory(item[1]); skills.save(skill);
            }
        }
    }
}
