Test project for sbt-svelte
============================

Run `npm install` in order to install all the necessary packages.

`sbt run` to run locally. Try modify *.vue to see that the changes are recompiled.

`sbt test` to run the browser test. It's important that the change is re-compiled when we run the test.

`sbt stage` to package app for production deployment, and run ` ./target/universal/stage/bin/test-play-project -Dplay.http.secret.key=abcdefghijk`. 
