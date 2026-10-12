package com.collins.platform.reconciliation;

import com.collins.platform.reconciliation.ReconModels.Explanation;
import com.collins.platform.reconciliation.ReconModels.LedgerTxn;
import com.collins.platform.reconciliation.ReconModels.Line;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Explains why a statement line did not match. Uses Spring AI when a chat model is configured
 * (AI_CHAT_MODEL_PROVIDER=openai and OPENAI_API_KEY), otherwise deterministic rules.
 * Guardrail: the explainer only reads and suggests. It never posts to the ledger; a person decides.
 */
@Component
public class MismatchExplainer {

    private static final Logger log = LoggerFactory.getLogger(MismatchExplainer.class);

    private static final String SYSTEM = """
            You are a payments reconciliation analyst. You explain, in two or three plain sentences, why a bank or
            card-network statement line did not match the internal double-entry ledger, and suggest one next step
            (for example: post to suspense, contact the bank, check for a missing settlement, or match manually).
            Never invent transaction ids. Never claim money was moved. Amounts are in minor units.
            """;

    private final ObjectProvider<ChatModel> chatModel;

    public MismatchExplainer(ObjectProvider<ChatModel> chatModel) {
        this.chatModel = chatModel;
    }

    public Explanation explain(Line line, List<LedgerTxn> candidates) {
        ChatModel model = chatModel.getIfAvailable();
        if (model != null) {
            try {
                String answer = ChatClient.create(model).prompt()
                        .system(SYSTEM)
                        .user(prompt(line, candidates))
                        .call()
                        .content();
                return new Explanation(line.id(), "AI", answer, suggestion(line, candidates));
            } catch (RuntimeException ex) {
                log.warn("AI explanation failed, falling back to rules: {}", ex.getMessage());
            }
        }
        return rules(line, candidates);
    }

    private static String prompt(Line line, List<LedgerTxn> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("Statement line: ref=").append(line.externalRef()).append(", amount=").append(line.amountMinor())
                .append(' ').append(line.currency()).append(", date=").append(line.valueDate())
                .append(", description=").append(line.description()).append('\n');
        sb.append("Ledger transactions with the same reference: ").append(candidates.isEmpty() ? "none" : "").append('\n');
        for (LedgerTxn t : candidates) {
            sb.append("- ").append(t.transactionId()).append(' ').append(t.transactionType()).append(' ').append(t.status())
                    .append(" entries=").append(t.entries()).append('\n');
        }
        return sb.toString();
    }

    private static Explanation rules(Line line, List<LedgerTxn> candidates) {
        String text;
        if (candidates.isEmpty()) {
            text = "No ledger transaction carries reference " + line.externalRef() + ". The money arrived (or left) at the bank but "
                    + "nothing on the platform explains it yet: possibly an unknown deposit, a settlement not yet posted, or a manual bank fee.";
        } else if (candidates.stream().anyMatch(t -> !"POSTED".equals(t.status()))) {
            text = "A ledger transaction with this reference exists but is not posted (pending or voided), so cash has not been recognised yet.";
        } else {
            text = "Ledger transactions with this reference exist, but none moves exactly " + line.amountMinor() + " " + line.currency()
                    + " through cash (1000). The amounts differ: check fees netted by the bank or a partial settlement.";
        }
        return new Explanation(line.id(), "RULES", text, suggestion(line, candidates));
    }

    private static String suggestion(Line line, List<LedgerTxn> candidates) {
        return candidates.isEmpty() ? "Post to suspense (2900) and investigate" : "Compare amounts and correct with a reversal if needed";
    }
}
