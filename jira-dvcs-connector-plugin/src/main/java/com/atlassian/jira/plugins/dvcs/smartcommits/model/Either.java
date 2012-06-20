package com.atlassian.jira.plugins.dvcs.smartcommits.model;

/**
 * An either class that holds either a value, or an error.
 */
public class Either<ERROR, VALUE> {
    private final ERROR error;
    private final VALUE value;

    Either(final ERROR error, final VALUE value) {
        this.error = error;
        this.value = value;
    }

    public ERROR getError() {
        return error;
    }

    public VALUE getValue() {
        return value;
    }

    public boolean hasError() {
        return error != null;
    }

    public boolean hasValue() {
        return value != null;
    }

    public static <E, V> Either<E, V> error(E e) {
        return new Either<E, V>(e, null);
    }

    public static <E, V> Either<E, V> value(V v) {
        return new Either<E, V>(null, v);
    }
}
