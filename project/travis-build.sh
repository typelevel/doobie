#!/bin/sh

cd `dirname $0`/..

if [ -z "$MAIN_SCALA_VERSION" ]; then
    >&2 echo "Environment variable MAIN_SCALA_VERSION is not set. Check .travis.yml."
    exit 1
fi

if [ -z "$TRAVIS_SCALA_VERSION" ]; then
    echo "Environment variable TRAVIS_SCALA_VERSION is not set. Using MAIN_SCALA_VERSION: $MAIN_SCALA_VERSION"
    TRAVIS_SCALA_VERSION=$MAIN_SCALA_VERSION
fi

if [ "$TRAVIS_SCALA_VERSION" = "$MAIN_SCALA_VERSION" ]; then
    echo "Testing with docs for Scala $MAIN_SCALA_VERSION"
    exec sbt ++$MAIN_SCALA_VERSION scalastyle compile test:compile test docs/tut docs_cats/tut
else
    echo "Testing without docs for Scala $TRAVIS_SCALA_VERSION"
    exec sbt ++$TRAVIS_SCALA_VERSION core/test h2/test hikari/test postgres/test specs2/test
fi
