demo:
	sbt run

console:
	sbt test:console

publish:
	sbt publish-local

repl:
	# Requires Ammonite 0.7.0 and sbt publish-local
	amm -f extras/Repl.scala
