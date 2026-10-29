package com.collins.platform.pricing.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PricingRepository {

    public record EntityRef(String entityId, String country, String functionalCurrency) {
    }

    public record FeePlan(long id, String entityId, String product, int percentBp, long fixedMinor, long minMinor,
                          Long maxMinor, String feePayer, LocalDate validFrom, LocalDate validTo) {
    }

    public record TaxRule(long id, String ruleCode, int version, String jurisdiction, String product, String taxType,
                          String base, BigDecimal rate, String ledgerAccount, boolean reverseChargeB2b,
                          LocalDate validFrom, LocalDate validTo, boolean example, String description, String sourceUrl) {
    }

    public record FxRate(String base, String quote, BigDecimal midRate, int spreadBp, Instant asOf) {
    }

    public record InterestRule(String jurisdiction, String product, String dayCount, Integer maxAnnualRateBp, String disclosure) {
    }

    public record StoredQuote(String id, String status, String quoteJson, Instant expiresAt, String usedBy) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public PricingRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<EntityRef> entity(String entityId) {
        return jdbc.query("select * from entities where entity_id = :e", Map.of("e", entityId),
                (rs, i) -> new EntityRef(rs.getString("entity_id"), rs.getString("country"), rs.getString("functional_currency")))
                .stream().findFirst();
    }

    public Optional<FeePlan> feePlan(String entityId, String product, LocalDate on) {
        return jdbc.query("""
                select * from fee_plans where entity_id = :e and product = :p
                and valid_from <= :d and (valid_to is null or valid_to >= :d)
                order by valid_from desc limit 1
                """, Map.of("e", entityId, "p", product, "d", on), (rs, i) -> new FeePlan(rs.getLong("id"), rs.getString("entity_id"),
                rs.getString("product"), rs.getInt("percent_bp"), rs.getLong("fixed_minor"), rs.getLong("min_minor"),
                (Long) rs.getObject("max_minor"), rs.getString("fee_payer"), rs.getObject("valid_from", LocalDate.class),
                rs.getObject("valid_to", LocalDate.class))).stream().findFirst();
    }

    public List<FeePlan> feePlans(String entityId) {
        return jdbc.query("select * from fee_plans where entity_id = :e order by product, valid_from", Map.of("e", entityId),
                (rs, i) -> new FeePlan(rs.getLong("id"), rs.getString("entity_id"), rs.getString("product"), rs.getInt("percent_bp"),
                        rs.getLong("fixed_minor"), rs.getLong("min_minor"), (Long) rs.getObject("max_minor"), rs.getString("fee_payer"),
                        rs.getObject("valid_from", LocalDate.class), rs.getObject("valid_to", LocalDate.class)));
    }

    private static final org.springframework.jdbc.core.RowMapper<TaxRule> TAX = (rs, i) -> new TaxRule(rs.getLong("id"),
            rs.getString("rule_code"), rs.getInt("version"), rs.getString("jurisdiction"), rs.getString("product"),
            rs.getString("tax_type"), rs.getString("base"), rs.getBigDecimal("rate"), rs.getString("ledger_account"),
            rs.getBoolean("reverse_charge_b2b"), rs.getObject("valid_from", LocalDate.class), rs.getObject("valid_to", LocalDate.class),
            rs.getBoolean("example"), rs.getString("description"), rs.getString("source_url"));

    public List<TaxRule> taxRules(String jurisdiction, String product, LocalDate on) {
        return jdbc.query("""
                select * from tax_rules where jurisdiction = :j and product = :p
                and valid_from <= :d and (valid_to is null or valid_to >= :d)
                order by rule_code
                """, Map.of("j", jurisdiction, "p", product, "d", on), TAX);
    }

    public List<TaxRule> allTaxRules(String jurisdiction) {
        return jdbc.query("select * from tax_rules where (:j = '' or jurisdiction = :j) order by jurisdiction, rule_code, version",
                Map.of("j", jurisdiction == null ? "" : jurisdiction), TAX);
    }

    public Optional<TaxRule> latestVersion(String ruleCode) {
        return jdbc.query("select * from tax_rules where rule_code = :c order by version desc limit 1", Map.of("c", ruleCode), TAX)
                .stream().findFirst();
    }

    public void closeRule(long id, LocalDate validTo) {
        jdbc.update("update tax_rules set valid_to = :d where id = :id", Map.of("d", validTo, "id", id));
    }

    public void insertRule(TaxRule r) {
        jdbc.update("""
                insert into tax_rules(rule_code, version, jurisdiction, product, tax_type, base, rate, ledger_account,
                    reverse_charge_b2b, valid_from, valid_to, example, description, source_url)
                values (:code, :v, :j, :p, :t, :b, :rate, :acc, :rc, :from, :to, :ex, :desc, :src)
                """, new MapSqlParameterSource().addValue("code", r.ruleCode()).addValue("v", r.version())
                .addValue("j", r.jurisdiction()).addValue("p", r.product()).addValue("t", r.taxType()).addValue("b", r.base())
                .addValue("rate", r.rate()).addValue("acc", r.ledgerAccount()).addValue("rc", r.reverseChargeB2b())
                .addValue("from", r.validFrom()).addValue("to", r.validTo()).addValue("ex", r.example())
                .addValue("desc", r.description()).addValue("src", r.sourceUrl()));
    }

    public Optional<FxRate> fxRate(String base, String quote) {
        return jdbc.query("select * from fx_rates where base = :b and quote = :q", Map.of("b", base, "q", quote),
                (rs, i) -> new FxRate(rs.getString("base"), rs.getString("quote"), rs.getBigDecimal("mid_rate"),
                        rs.getInt("spread_bp"), rs.getTimestamp("as_of").toInstant())).stream().findFirst();
    }

    public List<FxRate> fxRates() {
        return jdbc.query("select * from fx_rates order by base, quote", Map.of(), (rs, i) -> new FxRate(rs.getString("base"),
                rs.getString("quote"), rs.getBigDecimal("mid_rate"), rs.getInt("spread_bp"), rs.getTimestamp("as_of").toInstant()));
    }

    public void upsertFxRate(String base, String quote, BigDecimal mid, int spreadBp) {
        jdbc.update("""
                insert into fx_rates(base, quote, mid_rate, spread_bp, as_of) values (:b, :q, :m, :s, now())
                on conflict (base, quote) do update set mid_rate = :m, spread_bp = :s, as_of = now()
                """, Map.of("b", base, "q", quote, "m", mid, "s", spreadBp));
    }

    public Optional<InterestRule> interestRule(String jurisdiction, String product) {
        return jdbc.query("select * from interest_rules where jurisdiction = :j and product = :p", Map.of("j", jurisdiction, "p", product),
                (rs, i) -> new InterestRule(rs.getString("jurisdiction"), rs.getString("product"), rs.getString("day_count"),
                        (Integer) rs.getObject("max_annual_rate_bp"), rs.getString("disclosure"))).stream().findFirst();
    }

    public void insertQuote(String id, String entityId, String product, String json, Instant expiresAt) {
        jdbc.update("""
                insert into quotes(id, entity_id, product, quote_json, status, expires_at) values (:id, :e, :p, :j, 'OPEN', :x)
                """, new MapSqlParameterSource().addValue("id", id).addValue("e", entityId).addValue("p", product)
                .addValue("j", json).addValue("x", Timestamp.from(expiresAt)));
    }

    public Optional<StoredQuote> quote(String id) {
        return jdbc.query("select * from quotes where id = :id", Map.of("id", id), (rs, i) -> new StoredQuote(rs.getString("id"),
                rs.getString("status"), rs.getString("quote_json"), rs.getTimestamp("expires_at").toInstant(), rs.getString("used_by")))
                .stream().findFirst();
    }

    /** Atomically marks a quote used, only if it is still open and not expired. */
    public boolean consume(String id, String usedBy) {
        return jdbc.update("""
                update quotes set status = 'USED', used_at = now(), used_by = :u
                where id = :id and status = 'OPEN' and expires_at > now()
                """, Map.of("id", id, "u", usedBy)) == 1;
    }
}
