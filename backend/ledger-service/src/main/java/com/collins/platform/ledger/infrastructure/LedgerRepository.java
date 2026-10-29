package com.collins.platform.ledger.infrastructure;

import com.collins.platform.ledger.infrastructure.LedgerRows.AccountRef;
import com.collins.platform.ledger.infrastructure.LedgerRows.BalanceRow;
import com.collins.platform.ledger.infrastructure.LedgerRows.ChartAccount;
import com.collins.platform.ledger.infrastructure.LedgerRows.EntityRow;
import com.collins.platform.ledger.infrastructure.LedgerRows.EntryRow;
import com.collins.platform.ledger.infrastructure.LedgerRows.TxnRow;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** All ledger writes and point reads, with JdbcTemplate (no ORM: the SQL here is the design). */
@Repository
public class LedgerRepository {

    private static final RowMapper<TxnRow> TXN = (rs, i) -> new TxnRow(
            rs.getString("id"), rs.getString("entity_id"), rs.getString("idempotency_key"), rs.getString("request_hash"),
            rs.getString("transaction_type"), rs.getString("description"), rs.getObject("effective_date", LocalDate.class),
            rs.getString("reference_type"), rs.getString("reference_id"), rs.getString("status"), rs.getString("reversal_of"),
            rs.getTimestamp("created_at").toInstant());

    private static final RowMapper<ChartAccount> CHART = (rs, i) -> new ChartAccount(
            rs.getString("entity_id"), rs.getString("code"), rs.getString("name"),
            rs.getString("account_type"), rs.getString("normal_side"));

    private static final RowMapper<EntityRow> ENTITY = (rs, i) -> new EntityRow(
            rs.getString("entity_id"), rs.getString("name"), rs.getString("country"),
            rs.getString("functional_currency"), rs.getString("timezone"));

    private final NamedParameterJdbcTemplate jdbc;

    public LedgerRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------- entities and chart ----------

    public List<EntityRow> entities() {
        return jdbc.query("select * from legal_entities order by entity_id", ENTITY);
    }

    public Optional<EntityRow> entity(String entityId) {
        return jdbc.query("select * from legal_entities where entity_id = :id", Map.of("id", entityId), ENTITY).stream().findFirst();
    }

    public void insertEntity(EntityRow e, String copyChartFrom) {
        jdbc.update("""
                insert into legal_entities(entity_id, name, country, functional_currency, timezone)
                values (:id, :name, :country, :ccy, :tz)
                """, new MapSqlParameterSource().addValue("id", e.entityId()).addValue("name", e.name())
                .addValue("country", e.country()).addValue("ccy", e.functionalCurrency()).addValue("tz", e.timezone()));
        jdbc.update("""
                insert into chart_accounts(entity_id, code, name, account_type, normal_side)
                select :id, code, name, account_type, normal_side from chart_accounts where entity_id = :from
                """, Map.of("id", e.entityId(), "from", copyChartFrom));
    }

    public List<ChartAccount> chart(String entityId) {
        return jdbc.query("select * from chart_accounts where entity_id = :id order by code", Map.of("id", entityId), CHART);
    }

    public Optional<ChartAccount> chartAccount(String entityId, String code) {
        return jdbc.query("select * from chart_accounts where entity_id = :id and code = :code",
                Map.of("id", entityId, "code", code), CHART).stream().findFirst();
    }

    // ---------- ledger accounts and balances ----------

    /** Get-or-create the ledger account for (entity, account, currency) and its balance row. */
    public AccountRef account(String entityId, String account, String baseCode, String currency, String normalSide) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("e", entityId).addValue("a", account)
                .addValue("b", baseCode).addValue("c", currency);
        jdbc.update("""
                insert into ledger_accounts(entity_id, account, base_code, currency) values (:e, :a, :b, :c)
                on conflict (entity_id, account, currency) do nothing
                """, p);
        Long id = jdbc.queryForObject("select id from ledger_accounts where entity_id = :e and account = :a and currency = :c", p, Long.class);
        jdbc.update("insert into account_balances(account_id) values (:id) on conflict do nothing", Map.of("id", id));
        return new AccountRef(id, entityId, account, baseCode, currency, normalSide);
    }

    /** Locks balance rows in ascending id order: a fixed lock order means two postings can never deadlock. */
    public Map<Long, BalanceRow> lockBalances(Collection<Long> accountIds) {
        List<BalanceRow> rows = jdbc.query("""
                select account_id, posted_net, pending_net from account_balances
                where account_id in (:ids) order by account_id for update
                """, Map.of("ids", accountIds),
                (rs, i) -> new BalanceRow(rs.getLong("account_id"), rs.getLong("posted_net"), rs.getLong("pending_net")));
        Map<Long, BalanceRow> byId = new LinkedHashMap<>();
        rows.forEach(r -> byId.put(r.accountId(), r));
        return byId;
    }

    public void addToBalance(long accountId, long postedDelta, long pendingDelta) {
        jdbc.update("""
                update account_balances set posted_net = posted_net + :posted, pending_net = pending_net + :pending,
                       updated_at = now() where account_id = :id
                """, Map.of("posted", postedDelta, "pending", pendingDelta, "id", accountId));
    }

    /** Current natural balance of an exact account, or of a base code plus all its sub-accounts. */
    public long[] currentBalance(String entityId, String account, String currency, String normalSide) {
        String filter = account.contains(":") ? "la.account = :a" : "la.base_code = :a";
        Map<String, Object> row = jdbc.queryForMap("""
                select coalesce(sum(b.posted_net), 0) as posted, coalesce(sum(b.pending_net), 0) as pending
                from ledger_accounts la join account_balances b on b.account_id = la.id
                where la.entity_id = :e and la.currency = :c and """ + filter,
                Map.of("e", entityId, "c", currency, "a", account));
        long posted = ((Number) row.get("posted")).longValue();
        long pending = ((Number) row.get("pending")).longValue();
        int sign = "DEBIT".equals(normalSide) ? 1 : -1;
        return new long[]{sign * posted, sign * pending};
    }

    // ---------- transactions and entries ----------

    public Optional<TxnRow> transactionByKey(String entityId, String idempotencyKey) {
        return jdbc.query("select * from ledger_transactions where entity_id = :e and idempotency_key = :k",
                Map.of("e", entityId, "k", idempotencyKey), TXN).stream().findFirst();
    }

    public Optional<TxnRow> transaction(String id, boolean forUpdate) {
        String sql = "select * from ledger_transactions where id = :id" + (forUpdate ? " for update" : "");
        return jdbc.query(sql, Map.of("id", id), TXN).stream().findFirst();
    }

    public List<TxnRow> transactionsByReference(String entityId, String referenceId) {
        return jdbc.query("""
                select * from ledger_transactions where (:e = '' or entity_id = :e) and reference_id = :r
                order by created_at
                """, Map.of("e", entityId == null ? "" : entityId, "r", referenceId), TXN);
    }

    public List<TxnRow> recentTransactions(String entityId, int limit, int offset) {
        return jdbc.query("""
                select * from ledger_transactions where entity_id = :e
                order by created_at desc limit :limit offset :offset
                """, Map.of("e", entityId, "limit", limit, "offset", offset), TXN);
    }

    public void insertTransaction(TxnRow t, String metadataJson) {
        jdbc.update("""
                insert into ledger_transactions(id, entity_id, idempotency_key, request_hash, transaction_type, description,
                    effective_date, reference_type, reference_id, status, reversal_of, metadata_json, posted_at)
                values (:id, :e, :k, :h, :type, :d, :date, :rt, :rid, :status, :rev, :meta,
                    case when :status = 'POSTED' then now() else null end)
                """, new MapSqlParameterSource()
                .addValue("id", t.id()).addValue("e", t.entityId()).addValue("k", t.idempotencyKey())
                .addValue("h", t.requestHash()).addValue("type", t.transactionType()).addValue("d", t.description())
                .addValue("date", t.effectiveDate()).addValue("rt", t.referenceType()).addValue("rid", t.referenceId())
                .addValue("status", t.status()).addValue("rev", t.reversalOf()).addValue("meta", metadataJson));
    }

    public void insertEntry(String transactionId, long accountId, int lineNo, String direction, long amountMinor, String currency) {
        jdbc.update("""
                insert into ledger_entries(transaction_id, account_id, line_no, direction, amount_minor, currency)
                values (:t, :a, :n, :d, :amt, :c)
                """, new MapSqlParameterSource().addValue("t", transactionId).addValue("a", accountId).addValue("n", lineNo)
                .addValue("d", direction).addValue("amt", amountMinor).addValue("c", currency));
    }

    public List<EntryRow> entries(String transactionId) {
        return jdbc.query("""
                select e.line_no, e.account_id, la.account, e.direction, e.amount_minor, e.currency, ca.normal_side
                from ledger_entries e
                join ledger_accounts la on la.id = e.account_id
                join chart_accounts ca on ca.entity_id = la.entity_id and ca.code = la.base_code
                where e.transaction_id = :t order by e.line_no
                """, Map.of("t", transactionId), (rs, i) -> new EntryRow(rs.getInt("line_no"), rs.getLong("account_id"),
                rs.getString("account"), rs.getString("direction"), rs.getLong("amount_minor"), rs.getString("currency"),
                rs.getString("normal_side")));
    }

    public void markStatus(String transactionId, String status) {
        String column = "POSTED".equals(status) ? "posted_at" : "voided_at";
        jdbc.update("update ledger_transactions set status = :s, " + column + " = now() where id = :id",
                Map.of("s", status, "id", transactionId));
    }

    public void linkReversal(String reversalId, String originalId) {
        jdbc.update("update ledger_transactions set reversal_of = :o where id = :r", Map.of("o", originalId, "r", reversalId));
    }

    // ---------- periods ----------

    public boolean periodClosed(String entityId, String period) {
        Integer n = jdbc.queryForObject("""
                select count(*) from accounting_periods where entity_id = :e and period = :p and status = 'CLOSED'
                """, Map.of("e", entityId, "p", period), Integer.class);
        return n != null && n > 0;
    }

    public void closePeriod(String entityId, String period) {
        jdbc.update("""
                insert into accounting_periods(entity_id, period, status, closed_at) values (:e, :p, 'CLOSED', now())
                on conflict (entity_id, period) do update set status = 'CLOSED', closed_at = now()
                """, Map.of("e", entityId, "p", period));
    }

    public List<Map<String, Object>> periods(String entityId) {
        return jdbc.queryForList("select period, status, closed_at from accounting_periods where entity_id = :e order by period",
                Map.of("e", entityId));
    }

    // ---------- revaluation state ----------

    public Optional<java.math.BigDecimal> lastRevaluationRate(long accountId) {
        return jdbc.query("select rate from fx_revaluation_state where account_id = :id", Map.of("id", accountId),
                (rs, i) -> rs.getBigDecimal("rate")).stream().findFirst();
    }

    public void saveRevaluationRate(long accountId, java.math.BigDecimal rate, LocalDate asOf) {
        jdbc.update("""
                insert into fx_revaluation_state(account_id, rate, as_of) values (:id, :r, :d)
                on conflict (account_id) do update set rate = :r, as_of = :d
                """, Map.of("id", accountId, "r", rate, "d", asOf));
    }

    public List<AccountRef> foreignCurrencyMonetaryAccounts(String entityId, String functionalCurrency) {
        return jdbc.query("""
                select la.id, la.entity_id, la.account, la.base_code, la.currency, ca.normal_side
                from ledger_accounts la join chart_accounts ca on ca.entity_id = la.entity_id and ca.code = la.base_code
                where la.entity_id = :e and la.currency <> :c and ca.account_type in ('ASSET','LIABILITY')
                  and la.base_code not in ('1400', '1410')
                order by la.id
                """, Map.of("e", entityId, "c", functionalCurrency), (rs, i) -> new AccountRef(rs.getLong("id"),
                rs.getString("entity_id"), rs.getString("account"), rs.getString("base_code"), rs.getString("currency"),
                rs.getString("normal_side")));
    }

    public Timestamp now() {
        return jdbc.queryForObject("select now()", Map.of(), Timestamp.class);
    }
}
