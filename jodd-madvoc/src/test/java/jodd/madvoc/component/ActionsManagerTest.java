// Copyright (c) 2003-present, Jodd Team (http://jodd.org)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package jodd.madvoc.component;

import jodd.madvoc.MadvocConfig;
import jodd.madvoc.WebApp;
import jodd.madvoc.config.ActionDefinition;
import jodd.madvoc.config.ActionRuntime;
import jodd.madvoc.macro.RegExpPathMacros;
import jodd.madvoc.macro.WildcardPathMacros;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActionsManagerTest {

	public static class FooAction {
		public void one() {
		}
		public void two() {
		}
		public void three() {
		}
	}

	@Test
	void testActionPathMacros1() {
		WebApp webapp = new WebApp();
		webapp.start();

		ActionsManager actionsManager = webapp.madvocContainer().lookupComponent(ActionsManager.class);

		actionsManager.register(FooAction.class, "one", new ActionDefinition("/{one}"));

		ActionRuntime actionRuntime = actionsManager.lookup("/foo", null);
		assertNotNull(actionRuntime);

		actionRuntime = actionsManager.lookup("/foo/boo", null);
		assertNull(actionRuntime);
		actionRuntime = actionsManager.lookup("/foo/boo/zoo", null);
		assertNull(actionRuntime);
	}

	@Test
	void testActionPathMacros2() {
		WebApp webapp = new WebApp();
		webapp.start();
		
		ActionsManager actionsManager = webapp.madvocContainer().lookupComponent(ActionsManager.class);

		actionsManager.register(FooAction.class, "one", new ActionDefinition("/{one}"));
		actionsManager.register(FooAction.class, "two", new ActionDefinition("/xxx-{two}"));

		ActionRuntime actionRuntime = actionsManager.lookup("/foo", null);
		assertEquals("one", actionRuntime.actionClassMethod().getName());

		actionRuntime = actionsManager.lookup("/foo/boo", null);
		assertNull(actionRuntime);

		actionRuntime = actionsManager.lookup("/xxx-foo", null);
		assertEquals("two", actionRuntime.actionClassMethod().getName());	// best match!

	}

	@Test
	void testActionPathMacros3() {
		WebApp webapp = new WebApp();
		webapp.start();

		ActionsManager actionsManager = webapp.madvocContainer().lookupComponent(ActionsManager.class);

		actionsManager.register(FooAction.class, "one", new ActionDefinition("/yyy-{one}"));
		actionsManager.register(FooAction.class, "two", new ActionDefinition("/xxx-{two}"));

		assertEquals(2, actionsManager.getActionsCount());

		ActionRuntime actionRuntime = actionsManager.lookup("/foo", null);
		assertNull(actionRuntime);

		actionRuntime = actionsManager.lookup("/yyy-111", null);
		assertEquals("one", actionRuntime.actionClassMethod().getName());

		actionRuntime = actionsManager.lookup("/xxx-222", null);
		assertEquals("two", actionRuntime.actionClassMethod().getName());

		try {
			actionsManager.register(FooAction.class, "two", new ActionDefinition("/xxx-{two}"));
			fail("error");
		} catch (Exception ex) {
			// ignore
		}
	}

	@Test
	void testActionPathMacros4() {
		WebApp webapp = new WebApp();
		webapp.start();

		ActionsManager actionsManager = webapp.madvocContainer().lookupComponent(ActionsManager.class);

		actionsManager.register(FooAction.class, "one", new ActionDefinition("/{one}"));
		actionsManager.register(FooAction.class, "one", new ActionDefinition("/dummy"));		// no macro
		actionsManager.register(FooAction.class, "two", new ActionDefinition("/{two}/{three}"));
		actionsManager.register(FooAction.class, "three", new ActionDefinition("/life/{three}"));

		ActionRuntime actionRuntime = actionsManager.lookup("/foo", null);
		assertEquals("one", actionRuntime.actionClassMethod().getName());

 		actionRuntime = actionsManager.lookup("/scott/ramonna", null);
		assertEquals("two", actionRuntime.actionClassMethod().getName());

		actionRuntime = actionsManager.lookup("/life/universe", null);
		assertEquals("three", actionRuntime.actionClassMethod().getName());

		actionRuntime = actionsManager.lookup("/scott/ramonna/envy", null);
		assertNull(actionRuntime);

		actionRuntime = actionsManager.lookup("/life/universe/else", null);
		assertNull(actionRuntime);
	}

	@Test
	void testActionPathMacrosRegexp() {
		WebApp webapp = new WebApp();
		webapp.start();

		ActionsManager actionsManager = webapp.madvocContainer().lookupComponent(ActionsManager.class);
		MadvocConfig madvocConfig = webapp.madvocContainer().lookupComponent(MadvocConfig.class);
		madvocConfig.setPathMacroClass(RegExpPathMacros.class);

		actionsManager.register(FooAction.class, "one", new ActionDefinition("/{one:[ab]+}"));

		ActionRuntime actionRuntime = actionsManager.lookup("/a", null);
		assertNotNull(actionRuntime);

		actionRuntime = actionsManager.lookup("/ac", null);
		assertNull(actionRuntime);
	}

	@Test
	void testActionPathMacrosWildcard() {
		WebApp webapp = new WebApp();
		webapp.start();

		ActionsManager actionsManager = webapp.madvocContainer().lookupComponent(ActionsManager.class);
		MadvocConfig madvocConfig = webapp.madvocContainer().lookupComponent(MadvocConfig.class);
		madvocConfig.setPathMacroClass(WildcardPathMacros.class);

		actionsManager.register(FooAction.class, "one", new ActionDefinition("/{one:a?a}"));

		ActionRuntime actionRuntime = actionsManager.lookup("/aaa", null);
		assertNotNull(actionRuntime);

		actionRuntime = actionsManager.lookup("/aab", null);
		assertNull(actionRuntime);
	}
}
