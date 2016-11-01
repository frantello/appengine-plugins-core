/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.appengine.cloudsdk.internal.process;

import static java.lang.ProcessBuilder.Redirect;

import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.common.base.Charsets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


/**
 * Default process runner that allows synchronous or asynchronous execution. It also allows
 * monitoring output and checking the exit code of the child process.
 */
public class DefaultProcessRunner implements ProcessRunner {
  private final boolean async;
  private final List<ProcessOutputLineListener> stdOutLineListeners = new ArrayList<>();
  private final List<ProcessOutputLineListener> stdErrLineListeners = new ArrayList<>();
  private final List<ProcessExitListener> exitListeners;
  private final List<ProcessStartListener> startListeners;
  private final boolean inheritProcessOutput;

  private Map<String, String> environment;

  /**
   * Base constructor.
   *
   * @param async           whether to run commands asynchronously
   * @param exitListeners   client consumers of process onExit event
   * @param startListeners  client consumers of process onStart event
   */
  public DefaultProcessRunner(boolean async,
                               List<ProcessExitListener> exitListeners,
                               List<ProcessStartListener> startListeners,
                               boolean inheritProcessOutput) {
    this.async = async;
    this.exitListeners = exitListeners;
    this.startListeners = startListeners;
    this.inheritProcessOutput = inheritProcessOutput;
  }

  /**
   * Constructor that attaches output listeners to a process. It assumes the generated subprocess
   * does not inherit stdout/stderr.
   *
   * @param async                whether to run commands asynchronously
   * @param exitListeners        client consumers of process onExit event
   * @param startListeners       client consumers of process onStart event
   * @param stdOutLineListeners  client consumers of process standard output
   * @param stdErrLineListeners  client consumers of process error output
   */
  public DefaultProcessRunner(boolean async,
                              List<ProcessExitListener> exitListeners,
                              List<ProcessStartListener> startListeners,
                              List<ProcessOutputLineListener> stdOutLineListeners,
                              List<ProcessOutputLineListener> stdErrLineListeners) {
    this(async, exitListeners, startListeners, false /* inheritProcessOutput */);
    this.stdOutLineListeners.addAll(stdOutLineListeners);
    this.stdErrLineListeners.addAll(stdErrLineListeners);
  }

  /**
   * Executes a shell command.
   *
   * <p>If any output listeners were configured, output will go to them only. Otherwise, process
   * output will be redirected to the caller via inheritIO.
   *
   * @param command the shell command to execute
   */
  @Override
  public void run(String[] command) throws ProcessRunnerException {
    try {
      // Configure process builder.
      final ProcessBuilder processBuilder = new ProcessBuilder();

      // If there are no listeners, we might still want to redirect stdout and stderr to the parent
      // process, or not.
      if (stdOutLineListeners.isEmpty() && inheritProcessOutput) {
        processBuilder.redirectOutput(Redirect.INHERIT);
      }
      if (stdErrLineListeners.isEmpty() && inheritProcessOutput) {
        processBuilder.redirectError(Redirect.INHERIT);
      }
      if (environment != null) {
        processBuilder.environment().putAll(environment);
      }

      processBuilder.command(command);

      Process process = processBuilder.start();

      // Only handle stdout or stderr if there are listeners.
      if (!stdOutLineListeners.isEmpty()) {
        handleStdOut(process);
      }
      if (!stdErrLineListeners.isEmpty()) {
        handleErrOut(process);
      }

      for (ProcessStartListener startListener : startListeners) {
        startListener.onStart(process);
      }

      if (async) {
        asyncRun(process);
      } else {
        shutdownProcessHook(process);
        syncRun(process);
      }

    } catch (IOException | InterruptedException | IllegalThreadStateException e) {
      throw new ProcessRunnerException(e);
    }
  }

  /**
   * Environment variables to append to the current system environment variables.
   */
  @Override
  public void setEnvironment(Map<String, String> environment) {
    this.environment = environment;
  }

  private void handleStdOut(final Process process) {
    final Scanner stdOut = new Scanner(process.getInputStream(), Charsets.UTF_8.name());
    Thread stdOutThread = new Thread("standard-out") {
      @Override
      public void run() {
        while (stdOut.hasNextLine() && !Thread.interrupted()) {
          String line = stdOut.nextLine();
          for (ProcessOutputLineListener stdOutLineListener : stdOutLineListeners) {
            stdOutLineListener.onOutputLine(line);
          }
        }
        stdOut.close();
      }
    };
    stdOutThread.setDaemon(true);
    stdOutThread.start();
  }

  private void handleErrOut(final Process process) {
    final Scanner stdErr = new Scanner(process.getErrorStream(), Charsets.UTF_8.name());
    Thread stdErrThread = new Thread("standard-err") {
      @Override
      public void run() {
        while (stdErr.hasNextLine() && !Thread.interrupted()) {
          String line = stdErr.nextLine();
          for (ProcessOutputLineListener stdErrLineListener : stdErrLineListeners) {
            stdErrLineListener.onOutputLine(line);
          }
        }
        stdErr.close();
      }
    };
    stdErrThread.setDaemon(true);
    stdErrThread.start();
  }

  private void syncRun(final Process process) throws InterruptedException {
    int exitCode = process.waitFor();
    for (ProcessExitListener exitListener : exitListeners) {
      exitListener.onExit(exitCode);
    }
  }

  private void asyncRun(final Process process) throws InterruptedException {
    if (exitListeners.size() > 0) {
      Thread exitThread = new Thread("wait-for-exit") {
        @Override
        public void run() {
          try {
            process.waitFor();
          } catch (InterruptedException e) {
            e.printStackTrace();
          } finally {
            int exitCode = process.exitValue();
            for (ProcessExitListener exitListener : exitListeners) {
              exitListener.onExit(exitCode);
            }
          }
        }
      };
      exitThread.setDaemon(true);
      exitThread.start();
    }
  }

  private void shutdownProcessHook(final Process process) {
    Runtime.getRuntime().addShutdownHook(new Thread("destroy-process") {
      @Override
      public void run() {
        if (process != null) {
          process.destroy();
        }
      }
    });
  }

  @Override
  public void addStdOutListener(ProcessOutputLineListener stdOutListener) {
    stdOutLineListeners.add(stdOutListener);
  }

  @Override
  public void addExitListener(ProcessExitListener exitListener) {
    exitListeners.add(exitListener);
  }

  @Override
  public void addStdErrListener(ProcessOutputLineListener stdErrListener) {
    stdErrLineListeners.add(stdErrListener);
  }

  @Override
  public void removeStdErrListener(ProcessOutputLineListener stdErrListener) {
    stdErrLineListeners.remove(stdErrListener);
  }

  @Override
  public void removeStdOutListener(ProcessOutputLineListener stdOutListener) {
    stdOutLineListeners.remove(stdOutListener);
  }

  @Override
  public void removeExitListener(ProcessExitListener exitListener) {
    exitListeners.remove(exitListener);
  }
}
