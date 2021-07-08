/*************************************************************************
 * Copyright (c) 2021 The Eclipse Foundation and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution, and is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *************************************************************************/
package org.eclipse.dash.licenses.cli;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.dash.licenses.ContentId;
import org.eclipse.dash.licenses.IContentId;
import org.eclipse.dash.licenses.InvalidContentId;

/**
 * This is a very rudimentary <code>yarn.lock</code> file reader. The format
 * does not follow any existing standard (it looks kind of like YAML, but isn't
 * YAML) and AFAICT there are no existing Java-based readers/parsers. I actually
 * can't find any description of the format, so this implementation is based on
 * my observation of it via several examples.
 * <p>
 * For example:
 * 
 * <pre>
 * # THIS IS AN AUTOGENERATED FILE. DO NOT EDIT THIS FILE DIRECTLY.
 * # yarn lockfile v1
 * 
 * "@babel/code-frame@^7.0.0", "@babel/code-frame@^7.10.4":
 *   version "7.10.4"
 *   resolved "https://registry.yarnpkg.com/@babel/code-frame/-/code-frame-7.10.4.tgz#168da1a36e90da68ae8d49c0f1b48c7c6249213a"
 *   integrity sha512-vG6SvB6oYEhvgisZNFRmRCUkLz11c7rp+tbNTynGqc6mS1d5ATd/sGyV6W0KZZnXRKMTzZDRgQT3Ou9jhpAfUg==
 *   dependencies:
 *     "@babel/highlight" "^7.10.4"
 * 
 * "@babel/compat-data@^7.12.5", "@babel/compat-data@^7.12.7":
 *   version "7.12.7"
 *   resolved "https://registry.yarnpkg.com/@babel/compat-data/-/compat-data-7.12.7.tgz#9329b4782a7d6bbd7eef57e11addf91ee3ef1e41"
 *   integrity sha512-YaxPMGs/XIWtYqrdEOZOCPsVWfEoriXopnsz3/i7apYPXQ3698UFhS6dVT1KN5qOsWmVgw/FOrmQgpRaZayGsw==
 * </pre>
 *
 * The implementation is only as sophisticated as it needs to be and only
 * provides the behaviour that I require to determine a ClearlyDefined ID from
 * the content.
 */
public class YarnLockFileReader implements IDependencyListReader {

	private BufferedReader reader;

	public YarnLockFileReader(Reader input) {
		reader = new BufferedReader(input);
	}

	@Override
	public List<IContentId> getContentIds() {
		return read().stream().map(each -> each.getId()).collect(Collectors.toList());
	}

	/**
	 * Extract a hierarchical structure out of the file that we can query
	 * associations inherent in that hierarchy. To do this, we use a stack.
	 */
	private Record read() {
		/*
		 * At any point during the traversal, the top of the stack represents the last
		 * line that we encountered and the depth of the stack indicates the level of
		 * nesting for that element.
		 */
		var stack = new Stack<Record>();
		stack.push(new Record());

		reader.lines().filter(line -> !line.isEmpty()).filter(line -> !line.startsWith("#")).forEach(line -> {
			int level = 2 + getNestingFor(line);

			// As long as we have item with a lower level of nesting that the item
			// on the top of the stack, we pop the stack.
			while (stack.size() > level) {
				stack.pop();
			}

			if (level > stack.size()) {
				// When the level is greater than the item at the top of the stack, then
				// the current item is nested under the top item.
				Record item = new Record(line);
				stack.peek().add(item);
				stack.push(item);
			} else if (level == stack.size()) {
				// When the level is the same as the item on the top of the stack, a
				// new item is created and replaces the item on the top of the stack.
				stack.pop();
				Record item = new Record(line);
				stack.peek().add(item);
				stack.push(item);
			}
		});

		// The first item in the stack is the root item. Return that.
		return stack.firstElement();
	}

	/**
	 * Answer the nesting depth for a line. Every two spaces represents one level of
	 * nesting.
	 */
	private int getNestingFor(String line) {
		int count = 0;
		for (char c : line.toCharArray()) {
			if (c == ' ') {
				count++;
			} else {
				break;
			}
		}
		return count / 2;
	}

	/**
	 * Instances of this class represent a node in the hierarchy extracted from the
	 * file. Instances only track their own nested items (recursively).
	 */
	private class Record {
		String value;
		List<Record> nested = new ArrayList<>();

		public Record() {
		}

		public Record(String line) {
			value = line.trim();
		}

		public void add(Record item) {
			nested.add(item);
		}

		/**
		 * Assume that the receiver represents a top-level entry in the file and extract
		 * a content ID from it.
		 */
		public IContentId getId() {
			var pattern = Pattern.compile("(?:@(?<namespace>[\\w-]+)\\/)?(?<name>[\\w-\\.]+)");
			var matcher = pattern.matcher(value);
			if (matcher.find()) {
				var namespace = matcher.group("namespace");
				if (namespace == null)
					namespace = "-";
				var name = matcher.group("name");
				var version = getVersion();

				return ContentId.getContentId("npm", "npmjs", namespace, name, version);
			}
			return new InvalidContentId(value);
		}

		/**
		 * The version that actually gets resolved is represented in a child row. Walk
		 * through the immediate children of the receiver to find the one row that
		 * starts with "version".
		 * 
		 * FIXME Validate that we can assume that the version is surrounded by quotes.
		 * 
		 * @return
		 */
		public String getVersion() {
			var pattern = Pattern.compile("version \"(?<version>[^\"]+)\"");
			for (Record child : nested) {
				var matcher = pattern.matcher(child.value);
				if (matcher.matches()) {
					return matcher.group("version");
				}
			}
			return null;
		}

		@Override
		public String toString() {
			return "Record: " + value;
		}

		public Stream<Record> stream() {
			return nested.stream();
		}
	}
}