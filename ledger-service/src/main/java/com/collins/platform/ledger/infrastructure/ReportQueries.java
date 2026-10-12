package com.collins.platform.ledger.infrastructure;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Read-side SQL for reports. Everything is computed from posted entries, never from the projection. */
@Repository
public class ReportQueries {

    public record AccountTotals(String account, String baseCode, String name, String accountType, String normalSide,
                                String currency, long debitMinor, long creditMinor) {
        public long net() {
            return debitMinor - creditMinor;
        }

        public long natural() {
            return "DEBIT".equals(normalSide) ? net() : -net();
        }
    }

    public record StatementLine(String transactionId, LocalDate effectiveDate, String transactionType, String description,
                                String direction, long amountMinor) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public ReportQueries(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Totals per account for posted transactions with effective_date between from and to (inclusive). */
    public List<AccountTotals> totals(String entityId, LocalDate from, LocalDate to) {
        return totals(entityId, from, to, false);
    }

    /** @param excludeClosing leave out PERIOD_CLOSE transactions (needed for an income statement of a closed period). */
    public List<AccountTotals> totals(String entityId, LocalDate from, LocalDate to, boolean excludeClosing) {
        return jdbc.query("""
                select la.account, la.base_code, ca.name, ca.account_type, ca.normal_side, la.currency,
                       coalesce(sum(case when e.direction = 'DEBIT' then e.amount_minor else 0 end), 0) as debit_minor,
                       coalesce(sum(case when e.direction = 'CREDIT' then e.amount_minor else 0 end), 0) as credit_minor
                from ledger_entries e
                join ledger_transactions t on t.id = e.transaction_id and t.status = 'POSTED'
                join ledger_accounts la on la.id = e.account_id
                join chart_accounts ca on ca.entity_id = la.entity_id and ca.code = la.base_code
                where t.entity_id = :e and t.effective_date between :from and :to
                  and (:excl = false or t.transaction_type <> 'PERIOD_CLOSE')
                group by la.account, la.base_code, ca.name, ca.account_type, ca.normal_side, la.currency
                order by la.currency, la.base_code, la.account
                """, Map.of("e", entityId, "from", from, "to", to, "excl", excludeClosing), (rs, i) -> new AccountTotals(
                rs.getString("account"), rs.getString("base_code"), rs.getString("name"), rs.getString("account_type"),
                rs.getString("normal_side"), rs.getString("currency"), rs.getLong("debit_minor"), rs.getLong("credit_minor")));
    }

    public List<StatementLine> statement(String entityId, String account, String currency, LocalDate from, LocalDate to) {
        String filter = account.contains(":") ? "la.account = :a" : "la.base_code = :a";
        return jdbc.query("""
                select t.id, t.effective_date, t.transaction_type, t.description, e.direction, e.amount_minor
                from ledger_entries e
                join ledger_transactions t on t.id = e.transaction_id and t.status = 'POSTED'
                join ledger_accounts la on la.id = e.account_id
                where t.entity_id = :e and la.currency = :c and t.effective_date between :from and :to and """ + filter + """
                 order by t.effective_date, t.created_at, e.line_no
                """, Map.of("e", entityId, "a", account, "c", currency, "from", from, "to", to),
                (rs, i) -> new StatementLine(rs.getString("id"), rs.getObject("effective_date", LocalDate.class),
                        rs.getString("transaction_type"), rs.getString("description"), rs.getString("direction"),
                        rs.getLong("amount_minor")));
    }

    /** Projection vs entries: any difference means the balance table drifted (should never happen). */
    public List<Map<String, Object>> projectionDrift(String entityId) {
        return jdbc.queryForList("""
                select la.account, la.currency, b.posted_net,
                       coalesce((select sum(case when e.direction = 'DEBIT' then e.amount_minor else -e.amount_minor end)
                                 from ledger_entries e join ledger_transactions t on t.id = e.transaction_id
                                 where e.account_id = la.id and t.status = 'POSTED'), 0) as entries_net
                from ledger_accounts la join account_balances b on b.account_id = la.id
                where la.entity_id = :e
                """, Map.of("e", entityId)).stream()
                .filter(r -> ((Number) r.get("posted_net")).longValue() != ((Number) r.get("entries_net")).longValue())
                .toList();
    }

    public long stalePendingCount(String entityId, int hours) {
        Long n = jdbc.queryForObject("""
                select count(*) from ledger_transactions where entity_id = :e and status = 'PENDING'
                and created_at < now() - make_interval(hours => :h)
                """, Map.of("e", entityId, "h", hours), Long.class);
        return n == null ? 0 : n;
    }
}
