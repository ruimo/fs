#!/bin/sh
sbt dist
docker build --no-cache -t ruimo/fs .
