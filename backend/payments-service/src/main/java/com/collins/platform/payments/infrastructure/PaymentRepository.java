package com.collins.platform.payments.infrastructure;

import com.collins.platform.payments.domain.PaymentIntent;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PaymentRepository {

    private static final RowMapper<PaymentIntent> INTENT = (rs, i) -> new PaymentIntent(
            rs.getString("id"), rs.getString("entity_id"), rs.getString("merchant_id"), rs.getLong("amount_minor"),
            rs.getString("currency"), rs.getString("method"), rs.getString("status"), rs.getString("network_ref"),
            rs.getString("decline_reason"), rs.getLong("captured_minor"), rs.getLong("refunded_minor"), rs.getLong("fee_minor"),
            rs.getLong("tax_minor"), rs.getLong("net_minor"), rs.getString("quote_id"), rs.getString("description"),
            rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());

    public record Dispute(String id, String paymentId, long amountMinor, long feeMinor, String status, String reason) {
    }

    public record Refund(String id, String paymentId, long amountMinor, String reason, String ledgerTxnId) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public PaymentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(PaymentIntent p) {
        jdbc.update("""
                insert into payment_intents(id, entity_id, merchant_id, amount_minor, currency, method, status, network_ref,
                    decline_reason, description)
                values (:id, :e, :m, :amt, :c, :method, :status, :ref, :reason, :d)
                """, new MapSqlParameterSource().addValue("id", p.id()).addValue("e", p.entityId()).addValue("m", p.merchantId())
                .addValue("amt", p.amountMinor()).addValue("c", p.currency()).addValue("method", p.method())
                .addValue("status", p.status()).addValue("ref", p.networkRef()).addValue("reason", p.declineReason())
                .addValue("d", p.description()));
    }

    public Optional<PaymentIntent> find(String id, boolean forUpdate) {
        return jdbc.query("select * from payment_intents where id = :id" + (forUpdate ? " for update" : ""), Map.of("id", id), INTENT)
                .stream().findFirst();
    }

    public List<PaymentIntent> byMerchant(String merchantId, int limit) {
        return jdbc.query("select * from payment_intents where merchant_id = :m order by created_at desc limit :l",
                Map.of("m", merchantId, "l", limit), INTENT);
    }

    public void markCaptured(String id, long captured, long fee, long tax, long net, String quoteId) {
        jdbc.update("""
                update payment_intents set status = 'CAPTURED', captured_minor = :cap, fee_minor = :fee, tax_minor = :tax,
                    net_minor = :net, quote_id = :q, updated_at = now() where id = :id
                """, new MapSqlParameterSource().addValue("cap", captured).addValue("fee", fee).addValue("tax", tax)
                .addValue("net", net).addValue("q", quoteId).addValue("id", id));
    }

    public void updateStatus(String id, String status) {
        jdbc.update("update payment_intents set status = :s, updated_at = now() where id = :id", Map.of("s", status, "id", id));
    }

    public void addRefunded(String id, long amount, String status) {
        jdbc.update("""
                update payment_intents set refunded_minor = refunded_minor + :a, status = :s, updated_at = now() where id = :id
                """, Map.of("a", amount, "s", status, "id", id));
    }

    public void insertRefund(Refund r) {
        jdbc.update("insert into refunds(id, payment_id, amount_minor, reason, ledger_txn_id) values (:id, :p, :a, :r, :t)",
                new MapSqlParameterSource().addValue("id", r.id()).addValue("p", r.paymentId()).addValue("a", r.amountMinor())
                        .addValue("r", r.reason()).addValue("t", r.ledgerTxnId()));
    }

    public List<Refund> refunds(String paymentId) {
        return jdbc.query("select * from refunds where payment_id = :p order by created_at", Map.of("p", paymentId),
                (rs, i) -> new Refund(rs.getString("id"), rs.getString("payment_id"), rs.getLong("amount_minor"),
                        rs.getString("reason"), rs.getString("ledger_txn_id")));
    }

    public void insertDispute(Dispute d) {
        jdbc.update("insert into disputes(id, payment_id, amount_minor, fee_minor, status, reason) values (:id, :p, :a, :f, :s, :r)",
                new MapSqlParameterSource().addValue("id", d.id()).addValue("p", d.paymentId()).addValue("a", d.amountMinor())
                        .addValue("f", d.feeMinor()).addValue("s", d.status()).addValue("r", d.reason()));
    }

    public Optional<Dispute> dispute(String id, boolean forUpdate) {
        return jdbc.query("select * from disputes where id = :id" + (forUpdate ? " for update" : ""), Map.of("id", id),
                (rs, i) -> new Dispute(rs.getString("id"), rs.getString("payment_id"), rs.getLong("amount_minor"),
                        rs.getLong("fee_minor"), rs.getString("status"), rs.getString("reason"))).stream().findFirst();
    }

    public void resolveDispute(String id, String status) {
        jdbc.update("update disputes set status = :s, resolved_at = now() where id = :id", Map.of("s", status, "id", id));
    }

    public void insertSettlement(String batchId, String entityId, String currency, LocalDate date, long gross, long fees, long net,
                                 int lines, String ledgerTxnId) {
        jdbc.update("""
                insert into settlement_batches(batch_id, entity_id, currency, settled_on, gross_minor, fees_minor, net_minor,
                    line_count, ledger_txn_id)
                values (:b, :e, :c, :d, :g, :f, :n, :l, :t)
                """, new MapSqlParameterSource().addValue("b", batchId).addValue("e", entityId).addValue("c", currency)
                .addValue("d", date).addValue("g", gross).addValue("f", fees).addValue("n", net).addValue("l", lines)
                .addValue("t", ledgerTxnId));
    }

    public List<Map<String, Object>> settlements(String entityId) {
        return jdbc.queryForList("select * from settlement_batches where entity_id = :e order by created_at desc", Map.of("e", entityId));
    }
}
