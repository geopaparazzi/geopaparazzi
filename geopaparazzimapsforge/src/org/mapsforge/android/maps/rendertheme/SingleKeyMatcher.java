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

import java.util.Arrays;
import java.util.List;

import org.mapsforge.core.model.Tag;

class SingleKeyMatcher implements AttributeMatcher {
	private final String key;

	SingleKeyMatcher(String key) {
		this.key = key;
	}

	@Override
	public boolean isCoveredBy(AttributeMatcher attributeMatcher) {
		return attributeMatcher == this || attributeMatcher.matches(Arrays.asList(new Tag(this.key, null)));
	}

	@Override
	public boolean matches(List<Tag> tags) {
		for (int i = 0, n = tags.size(); i < n; ++i) {
			if (this.key.equals(tags.get(i).key)) {
				return true;
			}
		}
		return false;
	}
}
