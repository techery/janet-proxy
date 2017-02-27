package io.techery.janet.proxy.sample.actions;

import java.util.ArrayList;

import io.techery.janet.http.annotations.HttpAction;
import io.techery.janet.http.annotations.Response;
import io.techery.janet.proxy.LabeledAction;

@HttpAction("/users/techery/repos")
public class GithubAction implements LabeledAction {

  @Override public String getLabel() {
    return "github";
  }

  @Response ArrayList<Repository> repositories;

  public ArrayList<Repository> getRepositories() {
    return repositories;
  }

  @Override public String toString() {
    return "GithubAction{" +
        "repositories=" + repositories +
        '}';
  }

  public static class Repository {

    public final String name;
    public final String description;

    public Repository(String name, String description) {
      this.name = name;
      this.description = description;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Repository that = (Repository) o;

      if (name != null ? !name.equals(that.name) : that.name != null) return false;
      return !(description != null ? !description.equals(that.description) : that.description != null);

    }

    @Override
    public int hashCode() {
      int result = name != null ? name.hashCode() : 0;
      result = 31 * result + (description != null ? description.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return "Repository{" +
          "name='" + name + '\'' +
          ", description='" + description + '\'' +
          '}';
    }
  }

}
