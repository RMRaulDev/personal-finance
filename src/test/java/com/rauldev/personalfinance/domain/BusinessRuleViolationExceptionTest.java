package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BusinessRuleViolationExceptionTest {

    @Test
    void rejectsNullCode() {
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> new BusinessRuleViolationException(null, "message"));

        assertEquals("Business rule code cannot be null", exception.getMessage());
    }

    @Test
    void rejectsNullMessage() {
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> new BusinessRuleViolationException(BusinessRuleCode.INSUFFICIENT_BALANCE, null));

        assertEquals("Business rule message cannot be null", exception.getMessage());
    }

    @Test
    void preservesCodeAndMessage() {
        BusinessRuleViolationException exception =
            new BusinessRuleViolationException(BusinessRuleCode.ACCOUNT_INACTIVE, "Account is inactive");

        assertEquals(BusinessRuleCode.ACCOUNT_INACTIVE, exception.code());
        assertEquals("Account is inactive", exception.getMessage());
    }

    @Test
    void isAnUncheckedExceptionButNotAnIllegalStateException() {
        Object exception = new BusinessRuleViolationException(BusinessRuleCode.INSUFFICIENT_BALANCE, "message");

        assertTrue(exception instanceof RuntimeException);
        assertFalse(exception instanceof IllegalStateException);
    }

    @Test
    void preservesCodeMessageAndCauseWithCauseConstructor() {
        Exception cause = new Exception("root");

        BusinessRuleViolationException exception =
            new BusinessRuleViolationException(BusinessRuleCode.ACCOUNT_NAME_ALREADY_EXISTS, "duplicate", cause);

        assertEquals(BusinessRuleCode.ACCOUNT_NAME_ALREADY_EXISTS, exception.code());
        assertEquals("duplicate", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void causeConstructorRejectsNullCause() {
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> new BusinessRuleViolationException(BusinessRuleCode.ACCOUNT_NAME_ALREADY_EXISTS, "m", null));

        assertEquals("Business rule cause cannot be null", exception.getMessage());
    }

    @Test
    void causeConstructorRejectsNullCode() {
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> new BusinessRuleViolationException(null, "m", new Exception()));

        assertEquals("Business rule code cannot be null", exception.getMessage());
    }

    @Test
    void causeConstructorRejectsNullMessage() {
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> new BusinessRuleViolationException(BusinessRuleCode.ACCOUNT_INACTIVE, null, new Exception()));

        assertEquals("Business rule message cannot be null", exception.getMessage());
    }
}
