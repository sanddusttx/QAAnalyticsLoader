/*
 * Copyright (c) 2019 This code is licensed under the GPLv2. Please contact
 * Sanddust at sanddust@j2eeguys.com for additional licenses.
 */
package com.j2eeguys.qaanalytics;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/*
 * Test class for {@link Loader}
 * @author Gorky gorky@j2eeguys.com
 */
class LoaderTest {

  /**
   * Test method for {@link Loader#queryDate()}.
   */
  @SuppressWarnings("static-method")
  @Test
  @Tag("UI")
  void testQueryDate() {
    Loader c = new Loader();
    c.queryDate();
    System.out.println(c.month);
    System.out.println(c.year);
  }

}
