CCOUCH_PROJ_DIR=`dirname "$0"`
CCOUCH_MAIN_REPO="$CCOUCH_PROJ_DIR"/junk-repo

java -cp "$CCOUCH_PROJ_DIR"/web/WEB-INF/classes contentcouch.app.ContentCouchCommand -repo "$CCOUCH_MAIN_REPO" "$@"
