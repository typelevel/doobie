#!/bin/bash

cd `dirname $0`/..

psql -c 'create database world;' -U postgres && \
  psql -c '\i world.sql' -d world -U postgres && \
  psql -d world -c "create extension postgis" -U postgres && \
  psql -d world -c "create type myenum as enum ('foo', 'bar')" -U postgres
