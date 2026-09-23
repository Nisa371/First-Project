package com.marketplace.verification;

import java.sql.ResultSetMetaData;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// Hibernate update adds columns but does not relax an existing NOT NULL constraint.
// Keep the development upgrade idempotent; validate/none installations use the SQL migration.
@Component @RequiredArgsConstructor @Order(-95)
@ConditionalOnProperty(name="spring.jpa.hibernate.ddl-auto", havingValue="update")
public class VerificationSchemaUpgrade implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    @Override public void run(ApplicationArguments args) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            boolean required;
            try(var statement=connection.createStatement();var result=statement.executeQuery("SELECT candidate_id FROM verification_records WHERE 1=0")) {
                required=result.getMetaData().isNullable(1)==ResultSetMetaData.columnNoNulls;
            }
            if(required) {
                String database=connection.getMetaData().getDatabaseProductName();
                String sql=switch(database) {
                    case "MySQL", "MariaDB" -> "ALTER TABLE verification_records MODIFY COLUMN candidate_id BIGINT NULL";
                    case "H2" -> "ALTER TABLE verification_records ALTER COLUMN candidate_id DROP NOT NULL";
                    default -> throw new IllegalStateException("Apply verification-schema.sql before upgrading this database");
                };
                try(var statement=connection.createStatement()) { statement.execute(sql); }
            }
            return null;
        });
    }
}
