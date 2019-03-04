/*
 * Copyright 2019
 */
package com.j2eeguys.qaanalytics.loader;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author honor
 *
 */
class ControllerTest {

	/**
	 * Test method for {@link com.j2eeguys.qaanalytics.loader.Controller#queryDate()}.
	 */
	@Test
	void testQueryDate() {
		Controller c = new Controller();
		c.queryDate();
		System.out.println(c.month);
		System.out.println(c.year);
	}

	/**
	 * Test method for {@link com.j2eeguys.qaanalytics.loader.Controller#run()}.
	 */
	@Test
	void testRun() {
		fail("Not yet implemented");
	}

}
