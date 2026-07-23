package com.fitops.commons.api;

import java.util.function.Consumer;

/**
 * A field of a JSON Merge Patch (RFC 7386) request body, carrying three states rather than two:
 *
 * <ul>
 *   <li>{@link #undefined()} — the key was absent; leave the target unchanged
 *   <li>{@link #ofNull()} — the key was present with a {@code null} value; clear the target
 *   <li>{@link #of(Object)} — the key was present with a value; set the target
 * </ul>
 *
 * <p>Request layer only. This type must never appear on a response DTO, entity, port signature, or
 * domain event: absence-versus-null is a property of an incoming merge document.
 *
 * <p>Prefer the static factories. The canonical constructor is public only because a record cannot
 * narrow it.
 */
public record PatchValue<T>(boolean present, T value) {
  private static final PatchValue<?> UNDEFINED = new PatchValue<>(false, null);
  private static final PatchValue<?> NULL = new PatchValue<>(true, null);

  public PatchValue {
    if (!present && value != null) {
      throw new IllegalArgumentException("An absent PatchValue cannot carry a value");
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> PatchValue<T> undefined() {
    return (PatchValue<T>) UNDEFINED;
  }

  @SuppressWarnings("unchecked")
  public static <T> PatchValue<T> ofNull() {
    return (PatchValue<T>) NULL;
  }

  /** The key was present with a non-null value. */
  public static <T> PatchValue<T> of(T value) {
    if (value == null) {
      throw new IllegalArgumentException("Use ofNull() for an explicitly null field");
    }
    return new PatchValue<>(true, value);
  }

  /** The key was present; {@code null} means the client sent an explicit null. */
  public static <T> PatchValue<T> ofNullable(T value) {
    return value == null ? ofNull() : of(value);
  }

  /** Whether the key appeared in the request body at all. */
  public boolean isPresent() {
    return present;
  }

  /** Whether the key appeared with an explicit {@code null} — meaning "clear this field". */
  public boolean isNull() {
    return present && value == null;
  }

  public boolean hasValue() {
    return present && value != null;
  }

  public T orElse(T other) {
    return present ? value : other;
  }

  /**
   * Runs {@code consumer} when the key was present — <strong>including</strong> when it was an
   * explicit null, in which case {@code null} is passed. This is the apply-a-patch operation:
   * omitted leaves the target alone, present overwrites it with whatever was sent.
   */
  public void ifPresent(Consumer<T> consumer) {
    if (present) {
      consumer.accept(value);
    }
  }

  public T get() {
    if (!present) {
      throw new IllegalStateException("PatchValue is undefined");
    }
    return value;
  }
}
