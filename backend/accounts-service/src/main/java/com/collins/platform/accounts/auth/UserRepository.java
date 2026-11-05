package com.collins.platform.accounts.auth;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    public record User(String id, String email, String passwordHash, String name, String role, String tenantId) {
    }

    private final JdbcClient db;

    public UserRepository(JdbcClient db) {
        this.db = db;
    }

    public void save(User user) {
        db.sql("""
                insert into users (id, email, password_hash, name, role, tenant_id)
                values (?, ?, ?, ?, ?, ?)
                """)
                .params(user.id(), user.email(), user.passwordHash(), user.name(), user.role(), user.tenantId())
                .update();
    }

    public Optional<User> findByEmail(String email) {
        return db.sql("select id, email, password_hash, name, role, tenant_id from users where email = ?")
                .param(email)
                .query((rs, rowNum) -> new User(
                        rs.getString("id"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("name"),
                        rs.getString("role"),
                        rs.getString("tenant_id")
                ))
                .optional();
    }

    public Optional<User> findById(String id) {
        return db.sql("select id, email, password_hash, name, role, tenant_id from users where id = ?")
                .param(id)
                .query((rs, rowNum) -> new User(
                        rs.getString("id"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("name"),
                        rs.getString("role"),
                        rs.getString("tenant_id")
                ))
                .optional();
    }

    public boolean existsByEmail(String email) {
        Integer count = db.sql("select count(*) from users where email = ?")
                .param(email)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }
}
