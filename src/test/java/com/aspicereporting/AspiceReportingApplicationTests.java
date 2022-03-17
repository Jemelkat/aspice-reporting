package com.aspicereporting;

import com.aspicereporting.controller.ReportController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AspiceReportingApplicationTests {

	@Autowired
	ReportController reportController;

	@Test
	void contextLoads() {
		Assertions.assertNotNull(reportController);
	}

}
