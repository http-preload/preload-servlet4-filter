package com.example.httppreload.entity;

import java.util.List;
import java.util.StringJoiner;

import org.graalvm.polyglot.Source;

public class LinkHeader implements Comparable<LinkHeader> {
  private String conditionId;
  private Source condition;
  private String value;

  public LinkHeader() {
  }

  public LinkHeader(List<Link> links, String conditionId, Source condition) {
    this.setValue(LinkHeader.toString(links));
    this.setConditionId(conditionId);
    this.setCondition(condition);
  }

  public String getConditionId() {
    return conditionId;
  }

  public void setConditionId(String conditionId) {
    this.conditionId = conditionId;
  }

  public Source getCondition() {
    return condition;
  }

  public void setCondition(Source condition) {
    this.condition = condition;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public int compareTo(LinkHeader that) {
    if (this.conditionId == null) {
      return that.conditionId == null ? 0 : 1;
    } else {
      return that.conditionId == null ? -1 : 0;
    }
  }

  public static String toString(List<Link> links) {
    StringJoiner sj = new StringJoiner(", ");
    for (Link link : links) {
      sj.add(link.toString());
    }
    return sj.toString();
  }
}