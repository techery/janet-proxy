## ProxyService
ActionService for [Janet](https://github.com/techery/janet) which delegates actions to another services.  

### Getting Started

#### 1. Define some contract for services' actions
Contract could be anything: from marker interface to logical expression by class name - it's totally up to you.

E.g. let's use interface with String identifier:
```java
public interface LabeledAction {
  String label();
}
```

```java
@SomeServiceAction
public class SomeServiceAction1 implements LabeledAction {
  @Override public String label() {
    return "service1";
  }
}
```

```java
@SomeServiceAction
public class SomeServiceAction2 implements LabeledAction {
  @Override public String label() {
    return "service2";
  }
}
```

#### 2. Define service and add it to `Janet`
```java
SomeService service1 = new SomeService();
SomeService service2 = new SomeService();
//
// service1 and service2 must process actions of same annotation type as specified for ProxyService
//
ActionService proxyService = new ProxyService.Builder(SomeServiceAction.class)
        .add(service1, new ServiceMappingRule<LabeledAction>() {
          @Override public boolean matches(LabeledAction action) {
            return action.label().equals("service1");
          }
        })
        .add(service2, new ServiceMappingRule<LabeledAction>() {
          @Override public boolean matches(LabeledAction action) {
            return action.label().equals("service2");
          }
        })
        .build();
//
Janet janet = new Janet.Builder().addService(proxyService).build();
```

#### 3. Use `ActionPipe` to send/observe action as usual
```java
ActionPipe<SomeServiceAction1> actionPipe = janet.createPipe(SomeServiceAction1.class);
actionPipe
  .createObservable(new SomeServiceAction1())
  .subscribeOn(Schedulers.io())
  .subscribe(new ActionStateSubscriber<SomeServiceAction1>()
          .onSuccess(action -> System.out.println("SomeServiceAction1 has been executed on SomeService with label 'service1'"))
  );
```

### Real use-case
Precondition: you have several http endpoints and want to use the only 1 instance of `Janet`
```java
OkClient client = new OkClient();
GsonConverter converter = new GsonConverter(new Gson());

Janet janet = new Janet.Builder()
    .addService(new SampleLoggingService(new ProxyService.Builder(HttpAction.class)
        .add(
            new HttpActionService("https://api.github.com", client, converter),
            (LabeledAction action) -> action.label().equals("github"))
        .add(
            new HttpActionService("http://xkcd.com", client, converter),
            (LabeledAction action) -> action.label().equals("xkcd"))
        .build()
    )).build();

Observable.combineLatest(
    janet.createPipe(GithubAction.class, Schedulers.io()).createObservableResult(new GithubAction()),
    janet.createPipe(XkcdAction.class, Schedulers.io()).createObservableResult(new XkcdAction()),
    (githubAction, xkcdAction) -> null
).toBlocking().subscribe();
```

```java
@HttpAction("/info.0.json")
public class XkcdAction implements LabeledAction {

  @Override public String getLabel() {
    return "xkcd";
  }

  @Response XkcdData data;
}
```

```java
@HttpAction("/users/techery/repos")
public class GithubAction implements LabeledAction {

  @Override public String getLabel() {
    return "github";
  }

  @Response ArrayList<Repository> repositories;
}
```

The output:
```
send XkcdAction{data=null}
send GithubAction{repositories=null}
onStart GithubAction{repositories=null}
onStart XkcdAction{data=null}
onSuccess XkcdAction{data=XkcdData{img='https://imgs.xkcd.com/comics/video_content.png', title='Video Content'}}
onSuccess GithubAction{repositories=[Repository{name='ABFRealmMapView', description='Real-time map view clustering for Realm'}, ... , Repository{name='Doppelganger', description='Array diffs as collection view wants it'}]}
```

### Download
```groovy
repositories {
    jcenter()
    maven { url "https://jitpack.io" }
}

dependencies {
    compile 'com.github.techery:janet-proxy:xxx'
    // explicitly depend on latest Janet for bug fixes and new features (optionally)
    compile 'com.github.techery:janet:zzz' 
}
```
* janet: [![](https://jitpack.io/v/techery/janet.svg)](https://jitpack.io/#techery/janet)
* janet-proxy: [![](https://jitpack.io/v/techery/janet-proxy.svg)](https://jitpack.io/#techery/janet-proxy)
[![Build Status](https://travis-ci.org/techery/janet-proxy.svg?branch=master)](https://travis-ci.org/techery/janet-proxy)

## License

    Copyright (c) 2017 Techery

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

