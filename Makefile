# GNU Make 3.81 compatible wrappers; Gradle remains the build authority.
.DEFAULT_GOAL := help
# Builds and native launches share outputs. Serialize even with make -j.
.NOTPARALLEL:

GRADLE ?= ./gradlew
GRADLE_ARGS ?=
PYTHON ?= python3
COMPAT_ARGS ?=
PROFILE ?= standalone
RUN_DIR ?= .compat/runs/$(PROFILE)
PACKAGED ?= false
TEST_SELECTION ?= all
INVENTORY ?= validation/compatibility-lock.json
CULT_DEST ?= .compat/cult-source-new
CULT_GRADLE_HOME ?= .compat/cult-gradle-home
EVIDENCE_DIR ?=
COMMAND ?=

RUN_ARGS = "-PcompatProfile=$(PROFILE)" "-PcompatRunDir=$(RUN_DIR)" "-Ppackaged=$(PACKAGED)" "-PcompatTestSelection=$(TEST_SELECTION)"
COMPAT = $(PYTHON) tools/compatibility.py

.PHONY: help build assemble jar classes clean rebuild test test-java test-python check ci tasks version dependencies resolve-dependencies data javadoc publish-local publish-repo compat-help compat-verify compat-ranges compat-build-cult compat-prepare compat-verify-run client server gametest capture stop

help: ## Show targets and options (default)
	@printf 'FourTwenty development commands\n\n'
	@awk 'BEGIN { FS = ":.*## " } /^[a-zA-Z0-9_-]+:.*## / { printf "  %-23s %s\n", $$1, $$2 }' Makefile
	@printf '\nOverrides: GRADLE, GRADLE_ARGS, PYTHON, COMPAT_ARGS, PROFILE, RUN_DIR,\n'
	@printf 'PACKAGED, TEST_SELECTION, INVENTORY, CULT_DEST, CULT_GRADLE_HOME,\n'
	@printf 'EVIDENCE_DIR, COMMAND. See docs/DEVELOPMENT.md for defaults and examples.\n'

build: ## Build the mod JAR and run Gradle checks
	$(GRADLE) $(GRADLE_ARGS) build
assemble: ## Assemble artifacts without tests
	$(GRADLE) $(GRADLE_ARGS) assemble
jar: ## Package the production mod into build/libs
	$(GRADLE) $(GRADLE_ARGS) jar
classes: ## Compile production Java and process resources
	$(GRADLE) $(GRADLE_ARGS) classes
clean: ## Remove build outputs; retain run worlds, caches, and evidence
	$(GRADLE) $(GRADLE_ARGS) clean
rebuild: ## Clean, then build and run Gradle checks in order
	$(GRADLE) $(GRADLE_ARGS) clean build

test: test-java test-python ## Run Java and Python unit tests
test-java: ## Run JUnit tests (not native GameTests)
	$(GRADLE) $(GRADLE_ARGS) test
test-python: ## Run compatibility-tool unit tests
	$(PYTHON) -m unittest discover -s tools/tests -v
check: ## Run Gradle checks and Python tooling tests
	$(GRADLE) $(GRADLE_ARGS) check
	$(PYTHON) -m unittest discover -s tools/tests -v
ci: ## Build and run unit checks without a persistent Gradle daemon
	$(GRADLE) $(GRADLE_ARGS) --no-daemon build
	$(PYTHON) -m unittest discover -s tools/tests -v

tasks: ## List every Gradle task
	$(GRADLE) $(GRADLE_ARGS) tasks --all
version: ## Show wrapper and JVM versions
	$(GRADLE) $(GRADLE_ARGS) --version
dependencies: ## Show dependency trees; GRADLE_ARGS accepts --configuration NAME
	$(GRADLE) dependencies $(GRADLE_ARGS)
resolve-dependencies: ## Resolve build/test dependencies using existing integrity pins
	$(GRADLE) $(GRADLE_ARGS) resolveCompatibilityDependencies
data: ## Generate data into src/generated/resources
	$(GRADLE) $(GRADLE_ARGS) runData
javadoc: ## Generate Java API documentation
	$(GRADLE) $(GRADLE_ARGS) javadoc
publish-local: ## Publish to the user's local Maven repository
	$(GRADLE) $(GRADLE_ARGS) publishToMavenLocal
publish-repo: ## Publish to this project's file-based repo/ directory
	$(GRADLE) $(GRADLE_ARGS) publish

compat-help: ## Show compatibility-tool arguments
	$(COMPAT) --help
compat-verify: ## Verify frozen reference inventory against installed JARs
	$(COMPAT) verify $(COMPAT_ARGS)
compat-ranges: ## Check loader dependency ranges for INVENTORY
	$(GRADLE) $(GRADLE_ARGS) verifyCompatibility "-PcompatInventory=$(INVENTORY)"
compat-build-cult: ## Build the pinned sibling revision in a fresh CULT_DEST
	$(COMPAT) build-cult --destination "$(CULT_DEST)" --gradle-home "$(CULT_GRADLE_HOME)" $(COMPAT_ARGS)
compat-prepare: ## Prepare PROFILE in a fresh RUN_DIR; pass JARs in COMPAT_ARGS
	$(COMPAT) prepare --profile "$(PROFILE)" --destination "$(RUN_DIR)" $(COMPAT_ARGS)
compat-verify-run: ## Verify prepared profile and packaged/source composition
	$(COMPAT) verify-run --profile "$(PROFILE)" --destination "$(RUN_DIR)" --packaged "$(PACKAGED)" $(COMPAT_ARGS)
client: ## Launch a client in an already prepared RUN_DIR
	$(GRADLE) $(GRADLE_ARGS) runClient $(RUN_ARGS)
server: ## Launch a prepared dedicated server; handle EULA manually
	$(GRADLE) $(GRADLE_ARGS) runServer $(RUN_ARGS)
gametest: ## Run native GameTests in a prepared RUN_DIR with completion guard
	$(GRADLE) $(GRADLE_ARGS) runGameTestServer $(RUN_ARGS)
capture: ## Capture evidence; requires EVIDENCE_DIR and exact launch COMMAND
	@test -n "$(EVIDENCE_DIR)" || { printf '%s\n' 'Set EVIDENCE_DIR to a fresh evidence directory.' >&2; exit 2; }
	@test -n "$(COMMAND)" || { printf '%s\n' 'Set COMMAND to the exact launch command being recorded.' >&2; exit 2; }
	$(COMPAT) capture --destination "$(RUN_DIR)" --output "$(EVIDENCE_DIR)" --command "$(COMMAND)" $(COMPAT_ARGS)
stop: ## Stop Gradle daemons for this wrapper version
	$(GRADLE) $(GRADLE_ARGS) --stop
