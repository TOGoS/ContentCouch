all: ContentCouch-dev.jar

.DELETE_ON_ERROR:

ContentCouch-dev.jar: $(shell find src)
	rm -f "$@" # Always remove whatever's there so we don't accidentally clobber an existing file
	find src/main/java -name '*.java' >.src-files.lst
	rm -rf web/WEB-INF/classes
	mkdir -p web/WEB-INF/classes
	javac -extdirs ext-lib -d "web/WEB-INF/classes" @.src-files.lst -target 6 -source 6
	cp -a src/main/java/* "web/WEB-INF/classes/"
	rm -rf jar
	mkdir -p jar/META-INF
	cd jar && cp -a ../web/WEB-INF/classes/* . && unzip ../ext-lib/togos.mf-latest.jar
	echo "Manifest-Version: 1.0" >jar/META-INF/MANIFEST.MF
	echo "Main-Class: contentcouch.app.ContentCouchCommand" >>jar/META-INF/MANIFEST.MF
	cd jar && zip -r - . >../"$@"
	chmod -w "$@"
