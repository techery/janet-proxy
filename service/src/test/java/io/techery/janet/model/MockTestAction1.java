package io.techery.janet.model;

@MockServiceAction
public class MockTestAction1 implements LabeledAction {
  @Override public String label() {
    return "service1";
  }
}
