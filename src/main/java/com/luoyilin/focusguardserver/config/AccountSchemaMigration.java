package com.luoyilin.focusguardserver.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Hibernate update does not remove historical NOT NULL constraints. Preserve all existing rows. */
@Component
public class AccountSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public AccountSchemaMigration(JdbcTemplate jdbc, EntityManagerFactory entityManagerFactory) { this.jdbc=jdbc; }
    @Override public void run(ApplicationArguments args) {
        relax("email","ALTER TABLE users MODIFY COLUMN email VARCHAR(100) NULL");
        relax("password_hash","ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NULL");
    }
    private void relax(String column,String sql) {
        var nullable=jdbc.queryForList("SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='users' AND COLUMN_NAME=?",String.class,column);
        if(nullable.size()==1 && "NO".equals(nullable.get(0))) jdbc.execute(sql);
    }
}
