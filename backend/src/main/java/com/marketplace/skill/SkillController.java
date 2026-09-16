package com.marketplace.skill;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController
@RequiredArgsConstructor
public class SkillController {
    private final SkillRepository skills;
    public record SkillView(Long id, String name, String category) {}
    @GetMapping("/api/skills") public List<SkillView> list() {
        return skills.findByActiveTrueOrderByNameAsc().stream().map(s->new SkillView(s.getId(),s.getName(),s.getCategory())).toList();
    }
}
