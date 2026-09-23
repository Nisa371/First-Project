package com.marketplace.skill;
import com.marketplace.auth.CurrentAccount;
import com.marketplace.common.api.ApiException;
import com.marketplace.candidate.CandidateService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
@Service @RequiredArgsConstructor @Transactional @PreAuthorize("hasRole('ADMIN')")
public class AdminSkillService {
    private final SkillRepository skills; private final CurrentAccount current;
    public record Input(@NotBlank @Size(max=120) String name,@NotBlank @Size(max=120) String category,@NotNull Boolean active) {}
    public void save(Long id,Input input) {
        current.requireActive(); var skill=id==null?new Skill():skills.findById(id).orElseThrow(CandidateService::missing);
        skills.findByNameIgnoreCase(input.name().strip()).filter(s->!s.getId().equals(id)).ifPresent(s->{throw new ApiException(409,"DUPLICATE_SKILL","This skill already exists.");});
        skill.setName(input.name().strip()); skill.setCategory(input.category().strip()); skill.setActive(input.active()); skills.saveAndFlush(skill);
    }
}
