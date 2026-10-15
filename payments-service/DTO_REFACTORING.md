# DTO Refactoring - Clean Layer Communication

## Problem

The original implementation had **tight coupling** and **verbose parameter passing** between controllers and services:

### Before (Bloated)
```java
// Controller unpacks DTOs
@PostMapping
public ApiResponse<PaymentIntent> create(@RequestBody ApiRequest<CreatePaymentRequest> request) {
    CreatePaymentRequest p = request.payload();
    return ApiResponse.ok(paymentService.create(
        p.merchantId(),      // ❌ Verbose parameter unpacking
        p.amountMinor(),
        p.currency(),
        p.method(),
        p.instrument(),
        p.description()      // ❌ 6 individual parameters!
    ));
}

// Service accepts primitives
public PaymentIntent create(String merchantId, long amountMinor, String currency,
                            String method, String instrument, String description) {
    // ... business logic
}
```

### Issues:
- ❌ **Verbose** - Controllers manually unpack DTOs into individual parameters
- ❌ **Error-prone** - Easy to pass parameters in wrong order
- ❌ **Not maintainable** - Adding new field requires updating multiple places
- ❌ **Violates encapsulation** - DTOs are just data carriers, not used as such
- ❌ **Tight coupling** - Controllers know too much about service implementation

---

## Solution: Pass DTOs Through Layers

### After (Clean)
```java
// Controller passes DTO directly
@PostMapping
public ApiResponse<PaymentIntent> create(@RequestBody ApiRequest<CreatePaymentRequest> request) {
    return ApiResponse.ok(paymentService.create(request.payload()));  // ✅ Clean!
}

// Service accepts DTO
public PaymentIntent create(CreatePaymentRequest request) {
    // Access fields via request.merchantId(), request.amountMinor(), etc.
}
```

### Benefits:
- ✅ **Concise** - One parameter instead of six
- ✅ **Type-safe** - DTOs provide compile-time safety
- ✅ **Maintainable** - Add new fields to DTO, no signature changes
- ✅ **Clear intent** - "I'm creating a payment" vs "I'm passing 6 parameters"
- ✅ **Testable** - Easy to create test DTOs

---

## Changes Made

### 1. Service Layer Updated

**Before:**
```java
public PaymentIntent create(String merchantId, long amountMinor, String currency,
                            String method, String instrument, String description)
public PaymentIntent refund(String paymentId, long amountMinor, String reason)
public Dispute openDispute(String paymentId, String reason)
public SettlementResult settle(String entityId, String currency, LocalDate date)
```

**After:**
```java
public PaymentIntent create(CreatePaymentRequest request)
public PaymentIntent refund(String paymentId, RefundRequest request)
public Dispute openDispute(String paymentId, DisputeRequest request)
public SettlementResult settle(SettlementRequest request)
```

### 2. Controller Layer Updated

**PaymentIntentController:**
```java
// Before
() -> paymentService.create(p.merchantId(), p.amountMinor(), p.currency(),
                            p.method(), p.instrument(), p.description())

// After
() -> paymentService.create(request.payload())
```

**DisputeController:**
```java
// Before
() -> paymentService.openDispute(id, request.payload().reason())

// After
() -> paymentService.openDispute(id, request.payload())
```

**SettlementController:**
```java
// Before
paymentService.settle(payload.entityId(), payload.currency(), payload.date())

// After
paymentService.settle(request.payload())
```

---

## Best Practices Applied

### 1. **Data Transfer Objects (DTOs)**
DTOs encapsulate related data and travel between layers:
- Controller receives DTO from HTTP request
- Controller passes DTO to service
- Service extracts data from DTO
- Clear separation of concerns

### 2. **Immutability**
All DTOs are Java records (immutable by design):
```java
public record CreatePaymentRequest(
    @NotBlank String merchantId,
    @Positive long amountMinor,
    @NotBlank String currency,
    @NotBlank @Pattern(regexp = "CARD|MOBILE_MONEY") String method,
    String instrument,
    String description
) {}
```

### 3. **Validation at Boundaries**
Validation happens at the API boundary (controller):
```java
public ApiResponse<PaymentIntent> create(
    @Valid @RequestBody ApiRequest<CreatePaymentRequest> request)  // ✅ @Valid here
```

Service can assume data is valid - no need to re-validate.

### 4. **Clear API Contracts**
Method signatures clearly communicate intent:
```java
// Clear: "Create a payment with this request"
paymentService.create(CreatePaymentRequest request)

// Unclear: "Create something with these 6 things"
paymentService.create(String s1, long l1, String s2, String s3, String s4, String s5)
```

---

## Code Comparison

### Create Payment

**Before (10 lines, 6 parameters):**
```java
CreatePaymentRequest p = request.payload();
return ApiResponse.ok(idempotencyService.execute(
    "create-payment",
    idempotencyKey,
    p,
    PaymentIntent.class,
    () -> paymentService.create(
        p.merchantId(),
        p.amountMinor(),
        p.currency(),
        p.method(),
        p.instrument(),
        p.description()
    )
));
```

**After (7 lines, 1 parameter):**
```java
PaymentIntent result = idempotencyService.execute(
    "create-payment",
    idempotencyKey,
    request.payload(),
    PaymentIntent.class,
    () -> paymentService.create(request.payload())
);
return ApiResponse.ok(result);
```

### Refund

**Before:**
```java
RefundRequest payload = request.payload();
() -> paymentService.refund(id, payload.amountMinor(), payload.reason())
```

**After:**
```java
() -> paymentService.refund(id, request.payload())
```

### Settlement

**Before:**
```java
SettlementRequest payload = request.payload();
PaymentService.SettlementResult result = paymentService.settle(
    payload.entityId(),
    payload.currency(),
    payload.date()
);
```

**After:**
```java
PaymentService.SettlementResult result = paymentService.settle(request.payload());
```

---

## Metrics

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Create method params | 6 | 1 | -83% |
| Refund method params | 3 | 2 | -33% |
| Dispute method params | 2 | 2 | 0% (but cleaner) |
| Settlement method params | 3 | 1 | -67% |
| Lines of code (create) | 10 | 7 | -30% |
| Coupling | High | Low | ✅ |
| Maintainability | Low | High | ✅ |
| Type safety | Medium | High | ✅ |

---

## Migration Impact

### ✅ Backward Compatible
The changes are **100% backward compatible** at the HTTP API level:
- Same endpoints
- Same request/response formats
- Same validation rules
- Same business logic

### ⚠ Breaking Changes (Internal Only)
These changes are breaking **only for internal service consumers**:
- Service method signatures changed
- Controllers updated to match
- **No external API impact**

---

## Testing

All existing tests should pass with minimal changes:

```java
// Before
paymentService.create("merchant-1", 10000, "USD", "CARD", "tok_123", "Test payment");

// After
paymentService.create(new CreatePaymentRequest(
    "merchant-1", 10000, "USD", "CARD", "tok_123", "Test payment"
));
```

The DTO approach actually makes tests **more readable** and **easier to maintain**.

---

## Summary

This refactoring transforms the codebase from **primitive obsession** to **proper object-oriented design**:

- **Controllers** are thin HTTP adapters that pass DTOs
- **Services** receive rich domain objects (DTOs) instead of primitives
- **DTOs** encapsulate related data and validation
- **Clear boundaries** between layers

**Result:** Clean, maintainable, enterprise-grade code following industry best practices.