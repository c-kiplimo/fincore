package com.collins.platform.lending.collections;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CollectionsRepository {

    public record RecoveryCase(String id, String entityId, String debtorType, String debtorId, String debtKind, String debtAccount,
                               String currency, long amountMinor, long recoveredMinor, long writtenOffMinor, String status,
                               String reason, String referenceId) {
        /** Still to recover before any write-off. */
        public long remainingMinor() {
            return amountMinor - recoveredMinor - writtenOffMinor;
        }

        /** What can still be collected, including amounts already written off. */
        public long collectableMinor() {
            return amountMinor - recoveredMinor;
        }
    }

    public record Step(String id, String caseId, String stepType, String sourceAccount, String sourceCurrency, long sourceMinor,
                       long debtMinor, BigDecimal fxRate, String ledgerTxnId) {
    }

    private static final RowMapper<RecoveryCase> CASE = (rs, i) -> new RecoveryCase(rs.getString("id"), rs.getString("entity_id"),
            rs.getString("debtor_type"), rs.getString("debtor_id"), rs.getString("debt_kind"), rs.getString("debt_account"),
            rs.getString("currency"), rs.getLong("amount_minor"), rs.getLong("recovered_minor"), rs.getLong("written_off_minor"),
            rs.getString("status"), rs.getString("reason"), rs.getString("reference_id"));

    private final NamedParameterJdbcTemplate jdbc;

    public CollectionsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(RecoveryCase c) {
        jdbc.update("""
                insert into recovery_cases(id, entity_id, debtor_type, debtor_id, debt_kind, debt_account, currency, amount_minor,
                    status, reason, reference_id)
                values (:id, :e, :dt, :did, :dk, :acc, :c, :a, :s, :r, :ref)
                """, new MapSqlParameterSource().addValue("id", c.id()).addValue("e", c.entityId()).addValue("dt", c.debtorType())
                .addValue("did", c.debtorId()).addValue("dk", c.debtKind()).addValue("acc", c.debtAccount()).addValue("c", c.currency())
                .addValue("a", c.amountMinor()).addValue("s", c.status()).addValue("r", c.reason()).addValue("ref", c.referenceId()));
    }

    public Optional<RecoveryCase> find(String id, boolean forUpdate) {
        return jdbc.query("select * from recovery_cases where id = :id" + (forUpdate ? " for update" : ""), Map.of("id", id), CASE)
                .stream().findFirst();
    }

    public Optional<RecoveryCase> openForReference(String referenceId) {
        return jdbc.query("""
                select * from recovery_cases where reference_id = :r and status in ('OPEN','PARTIALLY_RECOVERED') limit 1
                """, Map.of("r", referenceId), CASE).stream().findFirst();
    }

    public List<RecoveryCase> byDebtor(String debtorId) {
        return jdbc.query("select * from recovery_cases where debtor_id = :d order by created_at desc", Map.of("d", debtorId), CASE);
    }

    public void updateAmounts(String id, long recovered, long writtenOff, String status, long newAmount) {
        jdbc.update("""
                update recovery_cases set recovered_minor = :r, written_off_minor = :w, status = :s, amount_minor = :a,
                    updated_at = now() where id = :id
                """, Map.of("r", recovered, "w", writtenOff, "s", status, "a", newAmount, "id", id));
    }

    public void insertStep(Step s) {
        jdbc.update("""
                insert into recovery_steps(id, case_id, step_type, source_account, source_currency, source_minor, debt_minor,
                    fx_rate, ledger_txn_id)
                values (:id, :c, :t, :sa, :sc, :sm, :dm, :fx, :l)
                """, new MapSqlParameterSource().addValue("id", s.id()).addValue("c", s.caseId()).addValue("t", s.stepType())
                .addValue("sa", s.sourceAccount()).addValue("sc", s.sourceCurrency()).addValue("sm", s.sourceMinor())
                .addValue("dm", s.debtMinor()).addValue("fx", s.fxRate()).addValue("l", s.ledgerTxnId()));
    }

    public List<Step> steps(String caseId) {
        return jdbc.query("select * from recovery_steps where case_id = :c order by created_at", Map.of("c", caseId),
                (rs, i) -> new Step(rs.getString("id"), rs.getString("case_id"), rs.getString("step_type"),
                        rs.getString("source_account"), rs.getString("source_currency"), rs.getLong("source_minor"),
                        rs.getLong("debt_minor"), rs.getBigDecimal("fx_rate"), rs.getString("ledger_txn_id")));
    }
}
