package com.marketplace.skill;

@org.springframework.stereotype.Component
@lombok.RequiredArgsConstructor
public class SkillCategoryUpgrade implements org.springframework.boot.ApplicationRunner {
    private final SkillRepository skills;
    @Override @org.springframework.transaction.annotation.Transactional
    public void run(org.springframework.boot.ApplicationArguments args) { skills.normalizeCategories(); }
}
