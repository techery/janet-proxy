package io.techery.janet.proxy;

public interface ServiceMappingRule<T> {
  boolean matches(T action);
}
