#!/bin/bash
set -xe

cd `dirname $0`/..

if [[ -z "$MAIN_SCALA_VERSION" ]]; then
    >&2 echo "Environment variable MAIN_SCALA_VERSION is not set. Check .travis.yml."
    exit 1
fi

if [[ -z "$TRAVIS_SCALA_VERSION" ]]; then
    echo "Environment variable TRAVIS_SCALA_VERSION is not set. Using MAIN_SCALA_VERSION: $MAIN_SCALA_VERSION"
    TRAVIS_SCALA_VERSION=$MAIN_SCALA_VERSION
fi

if [[ "$TRAVIS_SCALA_VERSION" = "$MAIN_SCALA_VERSION" ]]; then
    echo "Testing with docs for Scala $MAIN_SCALA_VERSION"
    sbt ++$MAIN_SCALA_VERSION compile mimaReportBinaryIssues test:compile test
    sbt ++$MAIN_SCALA_VERSION docs/tutQuick
    echo "Done testing with docs for Scala $MAIN_SCALA_VERSION"
else
    echo "Testing without docs for Scala $TRAVIS_SCALA_VERSION"
    sbt ++$TRAVIS_SCALA_VERSION compile test:compile test
    echo "Done testing without docs for Scala $TRAVIS_SCALA_VERSION"
fi
