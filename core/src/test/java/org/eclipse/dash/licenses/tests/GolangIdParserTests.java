/*************************************************************************
 * Copyright (c) 2019 The Eclipse Foundation and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution, and is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *************************************************************************/
package org.eclipse.dash.licenses.tests;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.dash.licenses.GolangIdParser;
import org.junit.jupiter.api.Test;

class GolangIdParserTests {

	@Test
	void testPathAndModule() {
		assertEquals("go/golang/github.com%2Fspf13/cobra/v0.0.5", new GolangIdParser()
				.parseId("github.com/spf13/cobra v0.0.5 h1:f0B+LkLX6DtmRH1isoNA9VTtNUK9K8xYd28JNNfOv/s=").toString());
		assertEquals("go/golang/golang.org%2Fx/tools/v0.0.0-20180221164845-07fd8470d635", new GolangIdParser().parseId(
				"golang.org/x/tools v0.0.0-20180221164845-07fd8470d635/go.mod h1:n7NCudcB/nEzxVGmLbDWY5pfWTLqBcC2KZ6jyYvM4mQ=")
				.toString());
		assertEquals("go/golang/gopkg.in%2Falecthomas/kingpin.v2/v2.2.6",
				new GolangIdParser().parseId(
						"gopkg.in/alecthomas/kingpin.v2 v2.2.6/go.mod h1:FMv+mEhP44yOT+4EoQTLFTRgOQ1FBLkstjWtayDeSgw=")
						.toString());
	}

	@Test
	void testNoPath() {
		assertEquals("go/golang/google.golang.org/genproto/v0.0.0-20190418145605-e7d98fc518a7",
				new GolangIdParser().parseId(
						"google.golang.org/genproto v0.0.0-20190418145605-e7d98fc518a7/go.mod h1:VzzqZJRnGkLBvHegQrXjBqPurQTc5/KpmUdxsrq26oE=")
						.toString());
		assertEquals("go/golang/go.etcd.io/bbolt/v1.3.2", new GolangIdParser()
				.parseId("go.etcd.io/bbolt v1.3.2/go.mod h1:IbVyRI1SCnLcuJnV2u8VeU0CEYM7e686BmAb1XKL+uU=").toString());
		assertEquals("go/golang/-/go.opencensus.io/v0.21.0", new GolangIdParser()
				.parseId("go.opencensus.io v0.21.0/go.mod h1:mSImk1erAIZhrmZN+AvHh14ztQfjbGwt4TtuofqLduU=").toString());
	}

	@Test
	void testNoModule() {
		assertEquals("go/golang/go.uber.org/zap/v1.10.0", new GolangIdParser()
				.parseId("go.uber.org/zap v1.10.0/go.mod h1:vwi/ZaCAaUcBkycHslxD9B2zi4UTXhF60s6SWpuDF0Q=").toString());
	}

	@Test
	void testFails() {
		assertNull(new GolangIdParser().parseId("groupid:artifactid:v1.0"));
		assertNull(new GolangIdParser().parseId("groupid:artifactid 1.0"));
	}
}