SOURCE_VERSION = 1.7
JFLAGS ?= -g:source,lines,vars -encoding utf8
PROCESSOR_PATH = $(BUILD_DIR)/net.aeten.core
TOUCH_DIR = .touch
DIST ?= JAR

all: src test

# Sources
SRC = core alert stream messenger messenger.stream parsing.properties parsing.xml parsing.yaml
src: $(SRC)
core::               jcip.annotations slf4j
alert::              core
stream::             core slf4j
messenger::          core slf4j
messenger.stream::   messenger stream slf4j
parsing.properties:: core slf4j
parsing.xml::        core
parsing.yaml::       core

# COST
COTS = jcip.annotations slf4j
cots: $(COTS)
jcip.annotations::
slf4j::

# Tests
TEST = messenger.test parsing.test stream.test spi.test
test: $(TEST)
messenger.test:: messenger.stream parsing.yaml slf4j;         $(RUN_TEST)
parsing.test::   parsing.properties parsing.xml parsing.yaml; $(RUN_TEST)
stream.test::    stream;                                      $(RUN_TEST)
spi.test::       core;                                        $(RUN_TEST)

clean:
	$(RM) -rf $(BUILD_DIR) $(DIST_DIR) $(GENERATED_DIR) $(TOUCH_DIR)

# TODO
define RUN_TEST
@echo Run test $@
endef


SRC_DIRS = src/ test/
MODULES = $(SRC) $(COTS) $(TEST)
-include java.mk

