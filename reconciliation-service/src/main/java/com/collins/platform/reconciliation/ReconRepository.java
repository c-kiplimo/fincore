package com.collins.platform.reconciliation;

import com.collins.platform.reconciliation.ReconModels.Line;
import com.collins.platform.reconciliation.ReconModels.Run;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ReconRepository {

    private static final RowMapper<Line> LINE = (rs, i) -> new Line(rs.getString("id"), rs.getString("run_id"),
            rs.getString("external_ref"), rs.getLong("amount_minor"), rs.getString("currency"),
            rs.getObject("value_date", LocalDate.class), rs.getString("description"), rs.getString("status"),
            rs.getString("matched_txn_id"), rs.getString("explanation"), rs.getString("suspense_txn_id"));

    private static final RowMapper<Run> RUN = (rs, i) -> new Run(rs.getString("id"), rs.getString("entity_id"), rs.getString("source"),
            rs.getString("currency"), rs.getObject("statement_date", LocalDate.class), rs.getInt("matched"), rs.getInt("exceptions"));

    private final NamedParameterJdbcTemplate jdbc;

    public ReconRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertRun(Run r) {
        jdbc.update("""
                insert into recon_runs(id, entity_id, source, currency, statement_date) values (:id, :e, :s, :c, :d)
                """, new MapSqlParameterSource().addValue("id", r.id()).addValue("e", r.entityId()).addValue("s", r.source())
                .addValue("c", r.currency()).addValue("d", r.statementDate()));
    }

    public void updateCounts(String runId, int matched, int exceptions) {
        jdbc.update("update recon_runs set matched = :m, exceptions = :x where id = :id", Map.of("m", matched, "x", exceptions, "id", runId));
    }

    public Optional<Run> run(String id) {
        return jdbc.query("select * from recon_runs where id = :id", Map.of("id", id), RUN).stream().findFirst();
    }

    public void insertLine(Line l) {
        jdbc.update("""
                insert into statement_lines(id, run_id, external_ref, amount_minor, currency, value_date, description, status, matched_txn_id)
                values (:id, :r, :ref, :a, :c, :d, :desc, :s, :t)
                """, new MapSqlParameterSource().addValue("id", l.id()).addValue("r", l.runId()).addValue("ref", l.externalRef())
                .addValue("a", l.amountMinor()).addValue("c", l.currency()).addValue("d", l.valueDate())
                .addValue("desc", l.description()).addValue("s", l.status()).addValue("t", l.matchedTxnId()));
    }

    public List<Line> lines(String runId, String status) {
        return jdbc.query("select * from statement_lines where run_id = :r and (:s = '' or status = :s) order by created_at",
                Map.of("r", runId, "s", status == null ? "" : status), LINE);
    }

    public Optional<Line> line(String id) {
        return jdbc.query("select * from statement_lines where id = :id", Map.of("id", id), LINE).stream().findFirst();
    }

    public void updateLine(String id, String status, String explanation, String suspenseTxnId) {
        jdbc.update("""
                update statement_lines set status = coalesce(:s, status), explanation = coalesce(:e, explanation),
                    suspense_txn_id = coalesce(:t, suspense_txn_id) where id = :id
                """, new MapSqlParameterSource().addValue("s", status).addValue("e", explanation).addValue("t", suspenseTxnId)
                .addValue("id", id));
    }
}
