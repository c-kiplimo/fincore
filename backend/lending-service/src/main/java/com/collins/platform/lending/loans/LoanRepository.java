package com.collins.platform.lending.loans;

import com.collins.platform.lending.loans.LoanModels.Loan;
import com.collins.platform.lending.loans.LoanModels.Product;
import com.collins.platform.lending.loans.LoanModels.Repayment;
import com.collins.platform.lending.loans.LoanModels.ScheduleRow;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class LoanRepository {

    private static final RowMapper<Loan> LOAN = (rs, i) -> new Loan(rs.getString("id"), rs.getString("entity_id"),
            rs.getString("jurisdiction"), rs.getString("product_code"), rs.getString("kind"), rs.getString("borrower_type"),
            rs.getString("borrower_id"), rs.getString("borrower_account"), rs.getString("currency"), rs.getLong("principal_minor"),
            rs.getLong("fee_minor"), rs.getInt("annual_rate_bp"), rs.getInt("term_months"), rs.getString("status"),
            rs.getInt("stage"), rs.getInt("dpd"), rs.getLong("outstanding_minor"), rs.getLong("interest_due_minor"),
            rs.getLong("fees_due_minor"), rs.getLong("unearned_fee_minor"), rs.getLong("allowance_minor"),
            rs.getObject("disbursed_on", LocalDate.class), rs.getObject("last_accrual_date", LocalDate.class));

    private static final RowMapper<ScheduleRow> ROW = (rs, i) -> new ScheduleRow(rs.getString("loan_id"), rs.getInt("n"),
            rs.getObject("due_date", LocalDate.class), rs.getLong("principal_due"), rs.getLong("interest_due"),
            rs.getLong("paid_minor"), rs.getBoolean("late_fee_done"));

    private final NamedParameterJdbcTemplate jdbc;

    public LoanRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Product> product(String code) {
        return jdbc.query("select * from loan_products where code = :c", Map.of("c", code), (rs, i) -> new Product(
                rs.getString("code"), rs.getString("kind"), rs.getString("name"), rs.getInt("annual_rate_bp"), rs.getInt("term_months"),
                rs.getInt("fixed_fee_bp"), rs.getInt("withhold_bp"), rs.getLong("late_fee_minor"), rs.getInt("grace_days"),
                rs.getLong("max_principal_minor"))).stream().findFirst();
    }

    public List<Product> products() {
        return jdbc.query("select * from loan_products order by code", Map.of(), (rs, i) -> new Product(
                rs.getString("code"), rs.getString("kind"), rs.getString("name"), rs.getInt("annual_rate_bp"), rs.getInt("term_months"),
                rs.getInt("fixed_fee_bp"), rs.getInt("withhold_bp"), rs.getLong("late_fee_minor"), rs.getInt("grace_days"),
                rs.getLong("max_principal_minor")));
    }

    public void insert(Loan l) {
        jdbc.update("""
                insert into loans(id, entity_id, jurisdiction, product_code, kind, borrower_type, borrower_id, borrower_account,
                    currency, principal_minor, fee_minor, annual_rate_bp, term_months, status)
                values (:id, :e, :j, :p, :k, :bt, :bid, :acc, :c, :pr, :fee, :rate, :term, :s)
                """, new MapSqlParameterSource().addValue("id", l.id()).addValue("e", l.entityId()).addValue("j", l.jurisdiction())
                .addValue("p", l.productCode()).addValue("k", l.kind()).addValue("bt", l.borrowerType()).addValue("bid", l.borrowerId())
                .addValue("acc", l.borrowerAccount()).addValue("c", l.currency()).addValue("pr", l.principalMinor())
                .addValue("fee", l.feeMinor()).addValue("rate", l.annualRateBp()).addValue("term", l.termMonths())
                .addValue("s", l.status()));
    }

    public Optional<Loan> find(String id, boolean forUpdate) {
        return jdbc.query("select * from loans where id = :id" + (forUpdate ? " for update" : ""), Map.of("id", id), LOAN)
                .stream().findFirst();
    }

    public List<Loan> byBorrower(String borrowerId) {
        return jdbc.query("select * from loans where borrower_id = :b order by created_at desc", Map.of("b", borrowerId), LOAN);
    }

    public List<Loan> withStatus(List<String> statuses, String kind) {
        return jdbc.query("select * from loans where status in (:s) and kind = :k order by created_at",
                Map.of("s", statuses, "k", kind), LOAN);
    }

    public Optional<Loan> activeFinancingFor(String merchantId) {
        return jdbc.query("""
                select * from loans where borrower_id = :m and kind = 'MERCHANT_FINANCING' and status in ('ACTIVE','DELINQUENT')
                order by created_at limit 1
                """, Map.of("m", merchantId), LOAN).stream().findFirst();
    }

    public boolean hasBadLoan(String borrowerId) {
        Integer n = jdbc.queryForObject("""
                select count(*) from loans where borrower_id = :b and status in ('DEFAULTED','WRITTEN_OFF')
                """, Map.of("b", borrowerId), Integer.class);
        return n != null && n > 0;
    }

    public void save(Loan l) {
        jdbc.update("""
                update loans set status = :s, stage = :stage, dpd = :dpd, outstanding_minor = :o, interest_due_minor = :i,
                    fees_due_minor = :f, unearned_fee_minor = :u, allowance_minor = :a, fee_minor = :fee,
                    disbursed_on = :d, last_accrual_date = :lad, updated_at = now()
                where id = :id
                """, new MapSqlParameterSource().addValue("s", l.status()).addValue("stage", l.stage()).addValue("dpd", l.dpd())
                .addValue("o", l.outstandingMinor()).addValue("i", l.interestDueMinor()).addValue("f", l.feesDueMinor())
                .addValue("u", l.unearnedFeeMinor()).addValue("a", l.allowanceMinor()).addValue("fee", l.feeMinor())
                .addValue("d", l.disbursedOn()).addValue("lad", l.lastAccrualDate()).addValue("id", l.id()));
    }

    public void insertScheduleRow(ScheduleRow r) {
        jdbc.update("""
                insert into loan_schedule(loan_id, n, due_date, principal_due, interest_due) values (:l, :n, :d, :p, :i)
                on conflict do nothing
                """, new MapSqlParameterSource().addValue("l", r.loanId()).addValue("n", r.n()).addValue("d", r.dueDate())
                .addValue("p", r.principalDue()).addValue("i", r.interestDue()));
    }

    public List<ScheduleRow> schedule(String loanId) {
        return jdbc.query("select * from loan_schedule where loan_id = :l order by n", Map.of("l", loanId), ROW);
    }

    /** Removes not-yet-paid instalments due on or after a date (used when restructuring). */
    public int deleteUnpaidFrom(String loanId, LocalDate from) {
        return jdbc.update("delete from loan_schedule where loan_id = :l and due_date >= :d and paid_minor = 0",
                Map.of("l", loanId, "d", from));
    }

    public void addPaid(String loanId, int n, long amount) {
        jdbc.update("update loan_schedule set paid_minor = paid_minor + :a where loan_id = :l and n = :n",
                Map.of("a", amount, "l", loanId, "n", n));
    }

    public void markLateFee(String loanId, int n) {
        jdbc.update("update loan_schedule set late_fee_done = true where loan_id = :l and n = :n", Map.of("l", loanId, "n", n));
    }

    public void insertRepayment(Repayment r) {
        jdbc.update("""
                insert into loan_repayments(id, loan_id, amount_minor, fees_part, interest_part, principal_part, source_account, ledger_txn_id)
                values (:id, :l, :a, :f, :i, :p, :s, :t)
                """, new MapSqlParameterSource().addValue("id", r.id()).addValue("l", r.loanId()).addValue("a", r.amountMinor())
                .addValue("f", r.feesPart()).addValue("i", r.interestPart()).addValue("p", r.principalPart())
                .addValue("s", r.sourceAccount()).addValue("t", r.ledgerTxnId()));
    }

    public List<Repayment> repayments(String loanId) {
        return jdbc.query("select * from loan_repayments where loan_id = :l order by created_at", Map.of("l", loanId),
                (rs, i) -> new Repayment(rs.getString("id"), rs.getString("loan_id"), rs.getLong("amount_minor"),
                        rs.getLong("fees_part"), rs.getLong("interest_part"), rs.getLong("principal_part"),
                        rs.getString("source_account"), rs.getString("ledger_txn_id")));
    }
}
