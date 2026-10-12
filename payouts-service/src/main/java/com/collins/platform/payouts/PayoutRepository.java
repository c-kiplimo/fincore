package com.collins.platform.payouts;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PayoutRepository {

    private static final RowMapper<Payout> PAYOUT = (rs, i) -> new Payout(rs.getString("id"), rs.getString("entity_id"),
            rs.getString("beneficiary_type"), rs.getString("beneficiary_id"), rs.getString("source_account"),
            rs.getLong("amount_minor"), rs.getString("currency"), rs.getLong("fee_minor"), rs.getLong("tax_minor"),
            rs.getLong("total_debited_minor"), rs.getString("destination_type"), rs.getString("destination_account"),
            rs.getString("status"), rs.getString("pending_txn_id"), rs.getString("confirm_txn_id"), rs.getString("bank_ref"),
            rs.getString("failure_reason"), rs.getInt("attempts"), rs.getTimestamp("created_at").toInstant());

    private final NamedParameterJdbcTemplate jdbc;

    public PayoutRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Payout p) {
        jdbc.update("""
                insert into payouts(id, entity_id, beneficiary_type, beneficiary_id, source_account, amount_minor, currency,
                    fee_minor, tax_minor, total_debited_minor, destination_type, destination_account, status)
                values (:id, :e, :bt, :bid, :src, :a, :c, :fee, :tax, :total, :dt, :da, :s)
                """, new MapSqlParameterSource().addValue("id", p.id()).addValue("e", p.entityId()).addValue("bt", p.beneficiaryType())
                .addValue("bid", p.beneficiaryId()).addValue("src", p.sourceAccount()).addValue("a", p.amountMinor())
                .addValue("c", p.currency()).addValue("fee", p.feeMinor()).addValue("tax", p.taxMinor())
                .addValue("total", p.totalDebitedMinor()).addValue("dt", p.destinationType()).addValue("da", p.destinationAccount())
                .addValue("s", p.status()));
    }

    public Optional<Payout> find(String id) {
        return jdbc.query("select * from payouts where id = :id", Map.of("id", id), PAYOUT).stream().findFirst();
    }

    public List<Payout> byBeneficiary(String beneficiaryId) {
        return jdbc.query("select * from payouts where beneficiary_id = :b order by created_at desc limit 100", Map.of("b", beneficiaryId), PAYOUT);
    }

    public List<Payout> awaitingBank(int maxAttempts) {
        return jdbc.query("""
                select * from payouts where status in ('SENDING','SENT') and attempts < :max
                and updated_at < now() - interval '10 seconds' order by created_at limit 50
                """, Map.of("max", maxAttempts), PAYOUT);
    }

    public void update(String id, String status, String pendingTxnId, String confirmTxnId, String bankRef, String failureReason,
                       boolean incrementAttempts) {
        jdbc.update("""
                update payouts set status = :s, pending_txn_id = coalesce(:p, pending_txn_id),
                    confirm_txn_id = coalesce(:c, confirm_txn_id), bank_ref = coalesce(:b, bank_ref),
                    failure_reason = coalesce(:f, failure_reason), attempts = attempts + :inc, updated_at = now()
                where id = :id
                """, new MapSqlParameterSource().addValue("s", status).addValue("p", pendingTxnId).addValue("c", confirmTxnId)
                .addValue("b", bankRef).addValue("f", failureReason).addValue("inc", incrementAttempts ? 1 : 0).addValue("id", id));
    }
}
