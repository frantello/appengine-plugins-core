/**
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.tools.app.module;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.app.GCloudExecutionException;
import com.google.cloud.tools.app.InvalidFlagException;
import com.google.cloud.tools.app.Option;
import com.google.cloud.tools.app.ProcessCaller;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for {@link SetDefaultAction}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SetDefaultActionTest {
  @Mock
  private ProcessCaller callerMock;

  @Before
  public void setUp() throws GCloudExecutionException {
    when(callerMock.getGcloudPath()).thenReturn("here");
    when(callerMock.call()).thenReturn(true);
  }

  @Test
  public void testPrepareCommand_withModule() {
    SetDefaultAction action = new SetDefaultAction(
        ImmutableList.of("module1"),
        "v1",
        ImmutableMap.<Option, String>of());

    Set<String> expected = ImmutableSet.of(action.getProcessCaller().getGcloudPath(), "preview",
        "app", "modules", "set-default", "module1", "--version", "v1");
    Set<String> actual = new HashSet<>(action.getProcessCaller().getCommand());
    assertEquals(expected, actual);
  }

  @Test
  public void testPrepareCommand_noModule() {
    SetDefaultAction action = new SetDefaultAction(
        ImmutableList.<String>of(),
        "v1",
        ImmutableMap.of(Option.SERVER, "server.com")
    );

    Set<String> expected = ImmutableSet.of(action.getProcessCaller().getGcloudPath(), "preview", "app",
        "modules", "set-default", "--version", "v1", "--server", "server.com");
    Set<String> actual = new HashSet<>(action.getProcessCaller().getCommand());

    assertEquals(expected, actual);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyVersion() {
    new SetDefaultAction(
        ImmutableList.of("module1", "module2"),
        "",
        ImmutableMap.<Option, String>of()
    );
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullVersion() {
    new SetDefaultAction(
        ImmutableList.of("module1", "module2"),
        null,
        ImmutableMap.<Option, String>of()
    );
  }

  @Test
  public void testCheckFlags_allFlags() {
    Map<Option, String> flags = ImmutableMap.of(Option.SERVER, "server.com");

    new SetDefaultAction(
        ImmutableList.of("module1", "module2"),
        "v1",
        flags
    );
  }

  @Test
  public void testCheckFlags_oneFlag() {
    new SetDefaultAction(
        ImmutableList.of("module1", "module2"),
        "v1",
        ImmutableMap.<Option, String>of()
    );
  }

  @Test(expected = InvalidFlagException.class)
  public void testCheckFlags_error() {
    Map<Option, String> flags = ImmutableMap.of(
        Option.SERVER, "server.com",
        Option.ADMIN_HOST, "disallowed flag!!!"
    );

    new SetDefaultAction(ImmutableList.of("module1", "module2"), "v1", flags);
  }

  @Test
  public void testExecute() throws GCloudExecutionException, IOException {
    SetDefaultAction action = new SetDefaultAction(
        ImmutableList.of("module1", "module2"),
        "v1",
        ImmutableMap.<Option, String>of()
    );
    action.setProcessCaller(callerMock);

    action.execute();

    verify(callerMock, times(1)).call();
  }
}