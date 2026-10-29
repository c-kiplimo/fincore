package com.collins.platform.ledger.domain;

public record EntryCommand(String account, Direction direction, long amountMinor, String currency, boolean enforceNonNegative) {

    public String baseCode() {
        int colon = account.indexOf(':');
        return colon < 0 ? account : account.substring(0, colon);
    }
}
