package io.jhoyt.bubbletimer.domain.core;

import io.jhoyt.bubbletimer.domain.exceptions.DomainException;

import java.util.function.Function;

/**
 * Result pattern implementation for consistent error handling.
 * This provides a functional approach to handling success and failure cases.
 * 
 * @param <T> Type of the success value
 */
public class Result<T> {
    private final T data;
    private final DomainException error;
    private final boolean isSuccess;
    
    /**
     * Private constructor to enforce immutability
     */
    private Result(T data, DomainException error, boolean isSuccess) {
        this.data = data;
        this.error = error;
        this.isSuccess = isSuccess;
    }
    
    /**
     * Create a successful result
     * @param data Success data
     * @param <T> Type of the data
     * @return Result containing the data
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(data, null, true);
    }
    
    /**
     * Create a failed result
     * @param error Domain exception that caused the failure
     * @param <T> Type of the data (unused in failure case)
     * @return Result containing the error
     */
    public static <T> Result<T> failure(DomainException error) {
        return new Result<>(null, error, false);
    }
    
    /**
     * Create a failed result with a generic exception
     * @param error Generic exception that caused the failure
     * @param <T> Type of the data (unused in failure case)
     * @return Result containing the error wrapped in a DomainException
     */
    public static <T> Result<T> failure(Exception error) {
        DomainException domainError = new DomainException(error.getMessage(), "GENERIC_ERROR", error) {};
        return new Result<>(null, domainError, false);
    }
    
    /**
     * Check if the result is successful
     * @return true if the result is successful
     */
    public boolean isSuccess() {
        return isSuccess;
    }
    
    /**
     * Check if the result is a failure
     * @return true if the result is a failure
     */
    public boolean isFailure() {
        return !isSuccess;
    }
    
    /**
     * Get the success data
     * @return Success data
     * @throws IllegalStateException if the result is a failure
     */
    public T getData() {
        if (!isSuccess) {
            throw new IllegalStateException("Cannot get data from failed result");
        }
        return data;
    }
    
    /**
     * Get the error
     * @return Domain exception that caused the failure
     * @throws IllegalStateException if the result is successful
     */
    public DomainException getError() {
        if (isSuccess) {
            throw new IllegalStateException("Cannot get error from successful result");
        }
        return error;
    }
    
    /**
     * Get the data or throw the error
     * @return Success data
     * @throws DomainException if the result is a failure
     */
    public T getDataOrThrow() throws DomainException {
        if (isSuccess) {
            return data;
        } else {
            throw error;
        }
    }
    
    /**
     * Transform the success value using a function
     * @param mapper Function to transform the success value
     * @param <R> Type of the transformed value
     * @return New result with transformed value, or the same error if failed
     */
    public <R> Result<R> map(Function<T, R> mapper) {
        if (isSuccess) {
            return Result.success(mapper.apply(data));
        } else {
            return Result.failure(error);
        }
    }
    
    /**
     * Transform the success value using a function that returns a Result
     * @param mapper Function to transform the success value into a Result
     * @param <R> Type of the transformed value
     * @return New result from the mapper, or the same error if failed
     */
    public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
        if (isSuccess) {
            return mapper.apply(data);
        } else {
            return Result.failure(error);
        }
    }
    
    /**
     * Execute a function if the result is successful
     * @param consumer Function to execute with the success value
     * @return This result unchanged
     */
    public Result<T> onSuccess(java.util.function.Consumer<T> consumer) {
        if (isSuccess) {
            consumer.accept(data);
        }
        return this;
    }
    
    /**
     * Execute a function if the result is a failure
     * @param consumer Function to execute with the error
     * @return This result unchanged
     */
    public Result<T> onFailure(java.util.function.Consumer<DomainException> consumer) {
        if (!isSuccess) {
            consumer.accept(error);
        }
        return this;
    }
    
    /**
     * Get a default value if the result is a failure
     * @param defaultValue Default value to return if failed
     * @return Success data or default value
     */
    public T getDataOrDefault(T defaultValue) {
        return isSuccess ? data : defaultValue;
    }
    
    /**
     * Get a default value if the result is a failure
     * @param supplier Supplier for the default value
     * @return Success data or default value from supplier
     */
    public T getDataOrDefault(java.util.function.Supplier<T> supplier) {
        return isSuccess ? data : supplier.get();
    }
    
    /**
     * Check if the error is recoverable
     * @return true if the error is recoverable, false if successful
     */
    public boolean isRecoverable() {
        return !isSuccess && error.isRecoverable();
    }
    
    /**
     * Get a user-friendly error message
     * @return User-friendly error message, or null if successful
     */
    public String getUserFriendlyMessage() {
        return isSuccess ? null : error.getUserFriendlyMessage();
    }
    
    @Override
    public String toString() {
        if (isSuccess) {
            return "Result.success(" + data + ")";
        } else {
            return "Result.failure(" + error.getMessage() + ")";
        }
    }
}
