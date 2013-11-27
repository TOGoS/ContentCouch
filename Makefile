all: ContentCouch.jar

.PHONY: .FORCE

ContentCouch.jar: .FORCE
	./build.sh
	./build-jar.sh
