package io.techery.janet.model;

import io.techery.janet.proxy.LabeledAction;

@OtherServiceAction
public class OtherTestAction implements LabeledAction {
  @Override public String getLabel() {
    return "service1";
  }
}
