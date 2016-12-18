package com.google.cloud.tools.appengine.api.devserver;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class DefaultRunConfigurationTest {

  private DefaultRunConfiguration configuration = new DefaultRunConfiguration();
  
  @Test 
  public void testEnvironmentVariablesInitiallyEmpty() {
    Assert.assertTrue(configuration.getEnvironmentVariables().isEmpty());
  }
  
  @Test 
  public void testEnvironmentVariablesNullKey() {
    Map<String, String> environment = new HashMap<>();
    environment.put(null, "foo");
    try {
      configuration.setEnvironmentVariables(environment);
      Assert.fail();
    } catch (NullPointerException expected) {
    }
  }
  
  @Test 
  public void testEnvironmentVariablesNullValue() {
    Map<String, String> environment = new HashMap<>();
    environment.put("foo", null);
    try {
      configuration.setEnvironmentVariables(environment);
      Assert.fail();
    } catch (NullPointerException expected) {
    }
  }
  
  @Test 
  public void testEnvironmentVariables_equalsInName() {
    Map<String, String> environment = new HashMap<>();
    environment.put("foo=bar", "baz");
    try {
      configuration.setEnvironmentVariables(environment);
      Assert.fail();
    } catch (IllegalArgumentException expected) {
      Assert.assertTrue(expected.getMessage().contains("foo=bar"));
    }
  }
  
  @Test 
  public void testEnvironmentVariables_NulInName() {
    Map<String, String> environment = new HashMap<>();
    environment.put("foo\u0000", "baz");
    try {
      configuration.setEnvironmentVariables(environment);
      Assert.fail();
    } catch (IllegalArgumentException expected) {
      Assert.assertTrue(expected.getMessage().contains("foo\u0000"));
    }
  }
  
}
