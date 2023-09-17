
all: clean test test-js

repl:
	lein repl

lint:
	clj-kondo --lint src --lint test

release:
	lein release

clean:
	rm -rf target

.PHONY: test
test:
	lein test

test-js:
	lein with-profile +cljs cljsbuild once
	node target/tests.js

toc-install:
	npm install --save markdown-toc

toc-build:
	node_modules/.bin/markdown-toc -i README.md
