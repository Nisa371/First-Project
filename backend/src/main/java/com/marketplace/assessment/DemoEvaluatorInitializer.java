package com.marketplace.assessment;
import com.marketplace.user.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
@Component
@Profile("dev")
public class DemoEvaluatorInitializer implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;
    public DemoEvaluatorInitializer(UserRepository users,PasswordEncoder encoder,
        @Value("${DEMO_EVALUATOR_EMAIL:}") String email,@Value("${DEMO_EVALUATOR_PASSWORD:}") String password) {
        this.users=users; this.encoder=encoder; this.email=email; this.password=password;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        if(email.isBlank() && password.isBlank()) return;
        if(!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || password.length()<10)
            throw new IllegalArgumentException("Demo evaluator requires a valid email and a password of at least 10 characters.");
        var existing=users.findByEmailIgnoreCase(email);
        if(existing.isPresent()) {
            if(existing.get().getRole()!=Role.EVALUATOR) throw new IllegalStateException("Demo evaluator email is already used by another role.");
            return;
        }
        var u=new User(); u.setEmail(email); u.setRole(Role.EVALUATOR); u.setPasswordHash(encoder.encode(password)); users.save(u);
    }
}
