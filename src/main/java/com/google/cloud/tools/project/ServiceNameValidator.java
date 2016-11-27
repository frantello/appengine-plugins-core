/*
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

package com.google.cloud.tools.project;

/**
 * Each service and each version must have a name. A name can contain numbers, letters, and hyphens.
 * It cannot be longer than 63 characters and cannot start or end with a hyphen."
 */
public class ServiceNameValidator {

  public static boolean validate(String name) {
    if (name == null) {
      return false;
    } else if (name.isEmpty()) {
      return false;
    } else if (name.startsWith("-")) {
      return false;
    } else if (name.endsWith("-")) {
      return false;
    } else if (name.length() > 63) {
      return false;
    } else {
      for (char c : name.toCharArray()) {
        if (Character.isAlphabetic(c)) {
          continue;
        } else if (Character.isDigit(c)) {
          continue;
        } else if (c == '-') {
          continue;
        } else {
          return false;
        }
      }
    }
    
    return true;
  }

}
