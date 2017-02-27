package io.techery.janet.proxy;

public interface ServiceMappingRule {
  boolean matches(LabeledAction action);
}
