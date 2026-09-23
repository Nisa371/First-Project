package com.marketplace.verification;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.*;
class VerificationSchemaUpgradeTest {
    @Test void existingCandidateConstraintIsRelaxedWithoutRemovingLegacyData() {
        var jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:verification-upgrade;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE verification_records (id BIGINT PRIMARY KEY, candidate_id BIGINT NOT NULL)");
        jdbc.update("INSERT INTO verification_records VALUES (1,42)");
        var upgrade=new VerificationSchemaUpgrade(jdbc);upgrade.run(null);upgrade.run(null);
        jdbc.update("INSERT INTO verification_records VALUES (2,NULL)");
        assertThat(jdbc.queryForObject("SELECT candidate_id FROM verification_records WHERE id=1",Long.class)).isEqualTo(42);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification_records",Long.class)).isEqualTo(2);
    }
}
