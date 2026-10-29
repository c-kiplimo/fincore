package com.collins.platform.accounts.infrastructure;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AccountsRepository {

    public record Merchant(String merchantId, String entityId, String name, String country, String settlementCurrency,
                           int reservePercentBp, int reserveDays, String status) {
    }

    public record Individual(String individualId, String entityId, String name, String country) {
    }

    public record Wallet(String walletId, String individualId, String entityId, String currency, String kind,
                         String ledgerAccount, String status) {
    }

    public record ReserveHold(String holdId, String merchantId, String paymentId, String entityId, long amountMinor,
                              String currency, LocalDate releaseOn, String status) {
    }

    public record Transfer(String transferId, String transferType, String entityId, String fromRef, String toRef,
                           long amountMinor, String currency, long feeMinor, long taxMinor, Long targetAmountMinor,
                           String targetCurrency, String quoteId, String ledgerTxnId) {
    }

    private static final RowMapper<Merchant> MERCHANT = (rs, i) -> new Merchant(rs.getString("id"), rs.getString("entity_id"),
            rs.getString("name"), rs.getString("country"), rs.getString("settlement_currency"), rs.getInt("reserve_percent_bp"),
            rs.getInt("reserve_days"), rs.getString("status"));

    private static final RowMapper<Wallet> WALLET = (rs, i) -> new Wallet(rs.getString("id"), rs.getString("individual_id"),
            rs.getString("entity_id"), rs.getString("currency"), rs.getString("kind"), rs.getString("ledger_account"),
            rs.getString("status"));

    private static final RowMapper<ReserveHold> HOLD = (rs, i) -> new ReserveHold(rs.getString("id"), rs.getString("merchant_id"),
            rs.getString("payment_id"), rs.getString("entity_id"), rs.getLong("amount_minor"), rs.getString("currency"),
            rs.getObject("release_on", LocalDate.class), rs.getString("status"));

    private final NamedParameterJdbcTemplate jdbc;

    public AccountsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertMerchant(Merchant m) {
        jdbc.update("""
                insert into merchants(id, entity_id, name, country, settlement_currency, reserve_percent_bp, reserve_days, status)
                values (:id, :e, :n, :c, :ccy, :bp, :days, :s)
                """, new MapSqlParameterSource().addValue("id", m.merchantId()).addValue("e", m.entityId()).addValue("n", m.name())
                .addValue("c", m.country()).addValue("ccy", m.settlementCurrency()).addValue("bp", m.reservePercentBp())
                .addValue("days", m.reserveDays()).addValue("s", m.status()));
    }

    public Optional<Merchant> merchant(String id) {
        return jdbc.query("select * from merchants where id = :id", Map.of("id", id), MERCHANT).stream().findFirst();
    }

    public List<Merchant> merchants(String entityId) {
        return jdbc.query("select * from merchants where entity_id = :e order by created_at", Map.of("e", entityId), MERCHANT);
    }

    public void insertIndividual(Individual ind) {
        jdbc.update("insert into individuals(id, entity_id, name, country) values (:id, :e, :n, :c)",
                Map.of("id", ind.individualId(), "e", ind.entityId(), "n", ind.name(), "c", ind.country()));
    }

    public Optional<Individual> individual(String id) {
        return jdbc.query("select * from individuals where id = :id", Map.of("id", id), (rs, i) -> new Individual(rs.getString("id"),
                rs.getString("entity_id"), rs.getString("name"), rs.getString("country"))).stream().findFirst();
    }

    public void insertWallet(Wallet w) {
        jdbc.update("""
                insert into wallets(id, individual_id, entity_id, currency, kind, ledger_account, status)
                values (:id, :ind, :e, :c, :k, :acc, :s)
                """, new MapSqlParameterSource().addValue("id", w.walletId()).addValue("ind", w.individualId())
                .addValue("e", w.entityId()).addValue("c", w.currency()).addValue("k", w.kind())
                .addValue("acc", w.ledgerAccount()).addValue("s", w.status()));
    }

    public Optional<Wallet> wallet(String id) {
        return jdbc.query("select * from wallets where id = :id", Map.of("id", id), WALLET).stream().findFirst();
    }

    public List<Wallet> wallets(String individualId) {
        return jdbc.query("select * from wallets where individual_id = :i order by kind desc, created_at", Map.of("i", individualId), WALLET);
    }

    public boolean insertHold(ReserveHold h) {
        return jdbc.update("""
                insert into reserve_holds(id, merchant_id, payment_id, entity_id, amount_minor, currency, release_on, status)
                values (:id, :m, :p, :e, :a, :c, :r, 'HELD') on conflict (payment_id) do nothing
                """, new MapSqlParameterSource().addValue("id", h.holdId()).addValue("m", h.merchantId()).addValue("p", h.paymentId())
                .addValue("e", h.entityId()).addValue("a", h.amountMinor()).addValue("c", h.currency())
                .addValue("r", h.releaseOn())) == 1;
    }

    public List<ReserveHold> dueHolds(LocalDate asOf) {
        return jdbc.query("select * from reserve_holds where status = 'HELD' and release_on <= :d order by release_on limit 500",
                Map.of("d", asOf), HOLD);
    }

    public List<ReserveHold> holds(String merchantId) {
        return jdbc.query("select * from reserve_holds where merchant_id = :m order by created_at desc", Map.of("m", merchantId), HOLD);
    }

    public boolean markReleased(String holdId) {
        return jdbc.update("update reserve_holds set status = 'RELEASED', released_at = now() where id = :id and status = 'HELD'",
                Map.of("id", holdId)) == 1;
    }

    public void insertTransfer(Transfer t) {
        jdbc.update("""
                insert into transfers(id, transfer_type, entity_id, from_ref, to_ref, amount_minor, currency, fee_minor, tax_minor,
                    target_amount_minor, target_currency, quote_id, ledger_txn_id)
                values (:id, :t, :e, :f, :to, :a, :c, :fee, :tax, :ta, :tc, :q, :l)
                """, new MapSqlParameterSource().addValue("id", t.transferId()).addValue("t", t.transferType())
                .addValue("e", t.entityId()).addValue("f", t.fromRef()).addValue("to", t.toRef()).addValue("a", t.amountMinor())
                .addValue("c", t.currency()).addValue("fee", t.feeMinor()).addValue("tax", t.taxMinor())
                .addValue("ta", t.targetAmountMinor()).addValue("tc", t.targetCurrency()).addValue("q", t.quoteId())
                .addValue("l", t.ledgerTxnId()));
    }

    public List<Transfer> transfers(String ref) {
        return jdbc.query("""
                select * from transfers where from_ref = :r or to_ref = :r order by created_at desc limit 200
                """, Map.of("r", ref), (rs, i) -> new Transfer(rs.getString("id"), rs.getString("transfer_type"),
                rs.getString("entity_id"), rs.getString("from_ref"), rs.getString("to_ref"), rs.getLong("amount_minor"),
                rs.getString("currency"), rs.getLong("fee_minor"), rs.getLong("tax_minor"), (Long) rs.getObject("target_amount_minor"),
                rs.getString("target_currency"), rs.getString("quote_id"), rs.getString("ledger_txn_id")));
    }
}
