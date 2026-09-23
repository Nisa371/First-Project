package com.marketplace.candidate;

import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import static org.assertj.core.api.Assertions.*;

class CandidateCvSchemaTest {
    @Test void migrationPreservesLegacyCandidatesAndCreatesOrderedCvTables() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:phase6-migration;MODE=MySQL", "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE candidate_profiles (id BIGINT PRIMARY KEY, cv_stored_name VARCHAR(255))");
            statement.execute("INSERT INTO candidate_profiles VALUES (1, 'original.pdf')");
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("../scripts/candidate-cv-schema.sql"));
            try (var result = statement.executeQuery("SELECT cv_stored_name, photo_stored_name FROM candidate_profiles WHERE id=1")) {
                assertThat(result.next()).isTrue(); assertThat(result.getString(1)).isEqualTo("original.pdf"); assertThat(result.getString(2)).isNull();
            }
            statement.execute("INSERT INTO candidate_cvs (id,candidate_id,created_at,updated_at) VALUES (1,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
            statement.execute("INSERT INTO cv_languages (cv_id,display_order,name,proficiency) VALUES (1,0,'Bangla','Native')");
            assertThatThrownBy(() -> statement.execute("INSERT INTO candidate_cvs (candidate_id,created_at,updated_at) VALUES (1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)")).isInstanceOf(java.sql.SQLException.class);
            statement.execute("DELETE FROM candidate_cvs WHERE id=1");
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM cv_languages")) { result.next(); assertThat(result.getInt(1)).isZero(); }
        }
    }
}
