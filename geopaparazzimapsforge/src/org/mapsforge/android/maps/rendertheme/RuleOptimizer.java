/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps.rendertheme;

import java.util.Stack;
import java.util.logging.Logger;

final class RuleOptimizer {
	private static final Logger LOGGER = Logger.getLogger(RuleOptimizer.class.getName());

	private static AttributeMatcher optimizeKeyMatcher(AttributeMatcher attributeMatcher, Stack<Rule> ruleStack) {
		for (int i = 0, n = ruleStack.size(); i < n; ++i) {
			if (ruleStack.get(i) instanceof PositiveRule) {
				PositiveRule positiveRule = (PositiveRule) ruleStack.get(i);
				if (positiveRule.keyMatcher.isCoveredBy(attributeMatcher)) {
					return AnyMatcher.getInstance();
				}
			}
		}

		return attributeMatcher;
	}

	private static AttributeMatcher optimizeValueMatcher(AttributeMatcher attributeMatcher, Stack<Rule> ruleStack) {
		for (int i = 0, n = ruleStack.size(); i < n; ++i) {
			if (ruleStack.get(i) instanceof PositiveRule) {
				PositiveRule positiveRule = (PositiveRule) ruleStack.get(i);
				if (positiveRule.valueMatcher.isCoveredBy(attributeMatcher)) {
					return AnyMatcher.getInstance();
				}
			}
		}

		return attributeMatcher;
	}

	static AttributeMatcher optimize(AttributeMatcher attributeMatcher, Stack<Rule> ruleStack) {
		if (attributeMatcher instanceof AnyMatcher || attributeMatcher instanceof NegativeMatcher) {
			return attributeMatcher;
		} else if (attributeMatcher instanceof SingleKeyMatcher) {
			return optimizeKeyMatcher(attributeMatcher, ruleStack);
		} else if (attributeMatcher instanceof SingleValueMatcher) {
			return optimizeValueMatcher(attributeMatcher, ruleStack);
		} else if (attributeMatcher instanceof MultiKeyMatcher) {
			return optimizeKeyMatcher(attributeMatcher, ruleStack);
		} else if (attributeMatcher instanceof MultiValueMatcher) {
			return optimizeValueMatcher(attributeMatcher, ruleStack);
		}
		throw new IllegalArgumentException("unknown AttributeMatcher: " + attributeMatcher);
	}

	static ClosedMatcher optimize(ClosedMatcher closedMatcher, Stack<Rule> ruleStack) {
		if (closedMatcher instanceof AnyMatcher) {
			return closedMatcher;
		}

		for (int i = 0, n = ruleStack.size(); i < n; ++i) {
			if (ruleStack.get(i).closedMatcher.isCoveredBy(closedMatcher)) {
				return AnyMatcher.getInstance();
			} else if (!closedMatcher.isCoveredBy(ruleStack.get(i).closedMatcher)) {
				LOGGER.warning("unreachable rule (closed)");
			}
		}

		return closedMatcher;
	}

	static ElementMatcher optimize(ElementMatcher elementMatcher, Stack<Rule> ruleStack) {
		if (elementMatcher instanceof AnyMatcher) {
			return elementMatcher;
		}

		for (int i = 0, n = ruleStack.size(); i < n; ++i) {
			if (ruleStack.get(i).elementMatcher.isCoveredBy(elementMatcher)) {
				return AnyMatcher.getInstance();
			} else if (!elementMatcher.isCoveredBy(ruleStack.get(i).elementMatcher)) {
				LOGGER.warning("unreachable rule (e)");
			}
		}

		return elementMatcher;
	}

	/**
	 * Private constructor to prevent instantiation from other classes.
	 */
	private RuleOptimizer() {
		// do nothing
	}
}
