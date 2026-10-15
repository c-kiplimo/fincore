# Idempotency Scopes - Constants vs Hardcoded Strings

## Problem: Hardcoded Strings (Before)

```java
// PaymentIntentController
idempotencyService.execute(
    "create-payment",  // ❌ Typo risk: "creat-payment", "create-paymentt"
    ...
)

// Refund
idempotencyService.execute(
    "refund:" + id,    // ❌ Could type "refnd:", "refund;" by mistake
    ...
)

// DisputeController
idempotencyService.execute(
    "dispute:" + id,   // ❌ Inconsistent: someone might use "chargeback:"
    ...
)
```

### Issues with Hardcoded Strings:

| Problem | Impact |
|---------|--------|
| **Typos** | `"creat-payment"` compiles fine, breaks at runtime |
| **No IDE support** | No autocomplete, no refactoring tools |
| **Hard to find usage** | Can't click to find all usages |
| **Inconsistency** | Different developers use different strings |
| **Hard to refactor** | Change scope name = search & replace 10+ files |
| **No documentation** | What scopes exist? Need to search codebase |

---

## Solution: Constants (After)

### 1. Created `IdempotencyScopes` Utility Class

```java
public final class IdempotencyScopes {

    private IdempotencyScopes() {
        // Utility class - no instantiation
    }

    // Static constant for payment creation
    public static final String CREATE_PAYMENT = "create-payment";

    // Prefixes for dynamic scopes
    public static final String REFUND_PREFIX = "refund:";
    public static final String DISPUTE_PREFIX = "dispute:";

    // Helper methods for building scoped keys
    public static String refundScope(String paymentId) {
        return REFUND_PREFIX + paymentId;
    }

    public static String disputeScope(String paymentId) {
        return DISPUTE_PREFIX + paymentId;
    }
}
```

### 2. Updated Controllers to Use Constants

**PaymentIntentController:**
```java
import static com.collins.platform.payments.api.IdempotencyScopes.CREATE_PAYMENT;
import static com.collins.platform.payments.api.IdempotencyScopes.refundScope;

// Create payment
idempotencyService.execute(
    CREATE_PAYMENT,     // ✅ Type-safe constant
    ...
)

// Refund
idempotencyService.execute(
    refundScope(id),    // ✅ Helper method prevents typos
    ...
)
```

**DisputeController:**
```java
import static com.collins.platform.payments.api.IdempotencyScopes.disputeScope;

// Dispute
idempotencyService.execute(
    disputeScope(id),   // ✅ Consistent, type-safe
    ...
)
```

---

## Benefits of Constants

### ✅ 1. **Compile-Time Safety**

```java
// Before - typo compiles but fails at runtime
idempotencyService.execute("creat-payment", ...)  // Bug!

// After - typo caught by compiler
idempotencyService.execute(CREAT_PAYMENT, ...)  // ❌ Compilation error!
```

### ✅ 2. **IDE Support**

- **Autocomplete:** Type `Idempotency` → IDE suggests `IdempotencyScopes`
- **Navigate to definition:** Ctrl+Click on constant to see where it's defined
- **Find all usages:** Right-click → "Find Usages" shows everywhere it's used
- **Refactoring:** Rename constant → IDE updates all usages automatically

### ✅ 3. **Single Source of Truth**

```java
// Want to change "create-payment" to "payment-create"?
// Before: Search & replace 10+ files manually
// After: Change 1 constant, done!

public static final String CREATE_PAYMENT = "payment-create";  // One change
```

### ✅ 4. **Documentation**

```java
/**
 * Scope for creating payment intents.
 * All payment creations share this scope.
 */
public static final String CREATE_PAYMENT = "create-payment";
```

New developers can read `IdempotencyScopes.java` to understand all available scopes!

### ✅ 5. **Consistency Guaranteed**

```java
// Before: Different developers might use:
"create-payment"
"payment-create"
"create_payment"
"new-payment"

// After: Everyone uses:
CREATE_PAYMENT  // ✅ Always consistent!
```

### ✅ 6. **Better Code Reviews**

```java
// Before - reviewer must remember scope names
idempotencyService.execute("refund:" + id, ...)  // Is this the right format?

// After - reviewer sees method name is clear
idempotencyService.execute(refundScope(id), ...)  // ✅ Obviously correct
```

---

## Comparison: Before vs After

### Before (Hardcoded Strings)
```java
// PaymentIntentController.java
idempotencyService.execute("create-payment", ...)

// Later in same file
idempotencyService.execute("refund:" + id, ...)

// DisputeController.java
idempotencyService.execute("dispute:" + id, ...)

// Somewhere else (typo!)
idempotencyService.execute("creat-payment", ...)  // Compiles but wrong!
```

**Problems:**
- 🔴 No compile-time checking
- 🔴 Typos not caught until runtime
- 🔴 Hard to find all usages
- 🔴 No IDE refactoring support
- 🔴 No documentation

### After (Constants)
```java
// PaymentIntentController.java
import static IdempotencyScopes.*;

idempotencyService.execute(CREATE_PAYMENT, ...)
idempotencyService.execute(refundScope(id), ...)

// DisputeController.java
import static IdempotencyScopes.*;

idempotencyService.execute(disputeScope(id), ...)

// Try to make a typo
idempotencyService.execute(CREAT_PAYMENT, ...)  // ❌ Won't compile!
```

**Benefits:**
- ✅ Compile-time type safety
- ✅ IDE autocomplete & navigation
- ✅ Refactoring tools work
- ✅ Self-documenting code
- ✅ Enforced consistency

---

## Real-World Scenario

### Requirement: "Change refund scope format from 'refund:X' to 'payment:X:refund'"

**Before (Hardcoded Strings):**
```java
// Need to find and change in 3+ files:
// PaymentIntentController.java
idempotencyService.execute("refund:" + id, ...)  // Change here

// Tests: PaymentIntentControllerTest.java
verify(idempotency).execute(eq("refund:" + id), ...)  // Change here

// Documentation: API_DOCS.md
"Use scope format: refund:{paymentId}"  // Change here

// Risk: Miss some files = inconsistent behavior!
```

**After (Constants):**
```java
// IdempotencyScopes.java - ONE PLACE TO CHANGE
public static String refundScope(String paymentId) {
    return "payment:" + paymentId + ":refund";  // Change once!
}

// All usages automatically updated:
idempotencyService.execute(refundScope(id), ...)  // ✅ Still works
verify(idempotency).execute(eq(refundScope(id)), ...)  // ✅ Still works
```

**Result:** One change, all code updated, zero risk of missing files!

---

## Best Practices

### ✅ DO:
```java
// Use constants for static scopes
public static final String CREATE_PAYMENT = "create-payment";

// Use helper methods for dynamic scopes
public static String refundScope(String paymentId) {
    return REFUND_PREFIX + paymentId;
}

// Import statically for cleaner code
import static IdempotencyScopes.*;
```

### ❌ DON'T:
```java
// Don't hardcode strings in multiple places
"create-payment"  // ❌ Repeated everywhere

// Don't concatenate manually
"refund:" + id    // ❌ Inconsistent

// Don't use magic strings
execute("cp", ...)  // ❌ What does "cp" mean?
```

---

## Testing Benefits

### Before:
```java
@Test
void testRefund() {
    // Have to remember exact string format
    verify(idempotency).execute(eq("refund:pi_123"), ...);  // Easy to mess up
}
```

### After:
```java
@Test
void testRefund() {
    // Use same constant as production code
    verify(idempotency).execute(eq(refundScope("pi_123")), ...);  // ✅ Guaranteed match!
}
```

---

## Summary

| Aspect | Hardcoded Strings | Constants |
|--------|-------------------|-----------|
| **Typo Safety** | 🔴 Runtime errors | ✅ Compile errors |
| **IDE Support** | 🔴 None | ✅ Full support |
| **Refactoring** | 🔴 Manual, error-prone | ✅ Automated, safe |
| **Documentation** | 🔴 Scattered in code | ✅ Centralized |
| **Consistency** | 🔴 Easy to mess up | ✅ Enforced |
| **Maintainability** | 🔴 Hard | ✅ Easy |
| **Find Usages** | 🔴 Text search only | ✅ IDE knows all usages |

**Result:** Safer, more maintainable, professional code! 🎯

---

## All Available Scopes

Quick reference for developers:

```java
// Static scopes
IdempotencyScopes.CREATE_PAYMENT         → "create-payment"

// Dynamic scopes (include resource ID)
IdempotencyScopes.refundScope(id)        → "refund:{paymentId}"
IdempotencyScopes.disputeScope(id)       → "dispute:{paymentId}"
```

This is now the **single source of truth** for all idempotency scopes in the payment service!