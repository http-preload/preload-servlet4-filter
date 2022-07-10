package com.example.httppreload.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.graalvm.polyglot.Source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PreloadManifest {
  private int preloadFileVersion;
  private Map<String, String> conditions;
  private Map<String, List<Link>> resources;

  private Source supportsEarlyHints;
  private transient Map<String, List<LinkHeader>> map;
  private transient int numConditions = 0;

  public PreloadManifest() {
    this.map = new LinkedHashMap<String, List<LinkHeader>>();
  }

  public int getPreloadFileVersion() {
    return preloadFileVersion;
  }

  public void setPreloadFileVersion(int preloadFileVersion) {
    this.preloadFileVersion = preloadFileVersion;
  }

  public Map<String, String> getConditions() {
    return conditions;
  }

  public void setConditions(Map<String, String> conditions) {
    this.conditions = conditions;
  }

  public Map<String, List<Link>> getResources() {
    return resources;
  }

  public void setResources(Map<String, List<Link>> resources) {
    this.resources = resources;
  }

  private List<LinkHeader> ensureGroup(String reqPath) {
    List<LinkHeader> group = map.get(reqPath);
    if (group == null) {
      group = new ArrayList<LinkHeader>();
      map.put(reqPath, group);
    }
    return group;
  }

  public void normalize() {
    Map<String, Source> functions = new HashMap<String, Source>();
    if (conditions != null) {
      for (Map.Entry<String, String> e : conditions.entrySet()) {
        String value = e.getValue();
        Source condition = Source.create("js", value.startsWith("function") ? "(" + value + ")" : value);
        functions.put(e.getKey(), condition);
      }
    }
    Source $supportsEarlyHints = functions.get("$supportsEarlyHints");
    // As of Jun 2022, only Chromium 103+ supports Early Hints
    this.supportsEarlyHints = $supportsEarlyHints != null ? $supportsEarlyHints
        : Source.create("js",
            "(userAgentData, headers)=>userAgentData.brands.some((e)=>e.brand==='Chromium'&&parseInt(e.version)>=103)");
    map.clear();
    int numConditions = 0;
    for (Map.Entry<String, List<Link>> e : resources.entrySet()) {
      String key = e.getKey();
      List<Link> links = e.getValue();
      if (links.size() == 0) {
        continue;
      }
      StringTokenizer st = new StringTokenizer(key);
      if (!st.hasMoreElements()) {
        continue;
      }
      String reqPath = st.nextToken();
      List<LinkHeader> group = ensureGroup(reqPath);
      String conditionId = null;
      Source condition = null;
      if (st.hasMoreElements()) {
        conditionId = st.nextToken();
        condition = functions.get(conditionId);
        if (condition == null) {
          throw new RuntimeException("Preload condition \"" + conditionId + "\" is not defined");
        }
        numConditions++;
      }
      group.add(new LinkHeader(links, conditionId, condition));
    }
    this.numConditions = numConditions;
  }

  public List<LinkHeader> lookup(String path) {
    return map.get(path);
  }

  public boolean hasAnyCondition() {
    return numConditions > 0;
  }

  public Source supportsEarlyHintsCondition() {
    return supportsEarlyHints;
  }

}