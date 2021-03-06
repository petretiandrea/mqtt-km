#!/bin/bash

if [ "$#" -ne 2 ]
  then
    echo "Use: ./bump-version old_version new_version"
    exit
fi

old_version=$1
new_version=$2

update_version() {
  echo "Updating version from '$old_version' to '$new_version' in $1"
  sed -i.bak s/$old_version/$new_version/g $1
  rm $1.bak
}

# update_version "README.md"
update_version "buildSrc/src/main/kotlin/BuildVersion.kt"

# Escape dots, e.g. 1.0.0 -> 1\.0\.0
escaped_old_version=$(echo $old_version | sed s/[.]/\\\\./g)
result=$(find ./ -type f \( -iname \*.properties -o -iname \*.md \) | grep -v "\.gradle" | grep -v "build" | xargs -I{} grep -H "$escaped_old_version" {} | grep -v CHANGES.md | grep -v COMPATIBILITY.md)
if [ -z "$result" ];
then
  git add .
  git commit -m "bump to version $2"
  git tag "v$2"
  echo "Done"
else
  echo "ERROR: Previous version is present in the project: $result"
  exit 1
fi