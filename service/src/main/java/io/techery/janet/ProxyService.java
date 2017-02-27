package io.techery.janet;

import java.util.HashMap;
import java.util.Map;

import io.techery.janet.proxy.LabeledAction;
import io.techery.janet.proxy.ServiceMappingRule;

final public class ProxyService extends ActionService {

  private Class supportedAnnotation;
  private Map<? extends ActionService, ServiceMappingRule> rules;

  ProxyService(Class supportedAnnotation, Map<? extends ActionService, ServiceMappingRule> rules) {
    this.supportedAnnotation = supportedAnnotation;
    this.rules = rules;
  }

  @Override protected Class getSupportedAnnotationType() {
    return supportedAnnotation;
  }

  @Override void setCallback(Callback callback) {
    super.setCallback(null);
    for (ActionService actionService : rules.keySet()) {
      actionService.setCallback(callback);
    }
  }

  @SuppressWarnings("unchecked")
  @Override protected <A> void sendInternal(ActionHolder<A> holder) throws JanetException {
    findService(holder).sendInternal(holder);
  }

  @Override protected <A> void cancel(ActionHolder<A> holder) {
    findService(holder).cancel(holder);
  }

  private ActionService findService(ActionHolder holder) throws JanetInternalException {
    LabeledAction action = checkAndCast(holder.action());
    for (Map.Entry<? extends ActionService, ServiceMappingRule> entry : rules.entrySet()) {
      if (entry.getValue().matches(action)) return entry.getKey();
    }
    throw new JanetInternalException(new IllegalArgumentException("Cant find proper service for " + holder.action()
        .getClass()
        .getName()));
  }

  private static LabeledAction checkAndCast(Object action) throws JanetInternalException {
    if (!(action instanceof LabeledAction)) {
      throw new JanetInternalException(String.format("%s must implement %s",
          action.getClass().getCanonicalName(), LabeledAction.class.getCanonicalName()));
    }
    return (LabeledAction) action;
  }

  public static class Builder {

    private Class supportedAnnotation;
    private Map<ActionService, ServiceMappingRule> rules;

    public Builder(Class supportedAnnotation) {
      if (!supportedAnnotation.isAnnotation()) {
        throw new IllegalArgumentException(supportedAnnotation.getCanonicalName() + " is not an annotation");
      }
      this.supportedAnnotation = supportedAnnotation;
      this.rules = new HashMap<ActionService, ServiceMappingRule>();
    }

    public Builder add(ActionService service, ServiceMappingRule rule) {
      if (supportedAnnotation != service.getSupportedAnnotationType()) {
        String message = String.format("Service with unsupported annotation: expected - %s but service has %s",
            supportedAnnotation.getCanonicalName(), service.getSupportedAnnotationType().getCanonicalName());
        throw new IllegalArgumentException(message);
      }
      rules.put(service, rule);
      return this;
    }

    public ProxyService build() {
      if (rules.isEmpty()) throw new IllegalStateException("No service added, can't operate");
      return new ProxyService(supportedAnnotation, rules);
    }

  }

}
